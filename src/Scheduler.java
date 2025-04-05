import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * The Scheduler class is responsible for:
 * - Receiving fire events from the FireIncident subsystem.
 * - Assigning fire events to available drones.
 * - Managing drone availability and reassigning failed missions.
 * - Handling drone responses asynchronously using a separate monitoring thread.
 * The Scheduler runs as a separate thread and interacts with the system via two
 * queues:
 * - `sharedFireQueue` → Receives fire requests from `FireIncident`.
 * - `responseQueue` → Receives updates from `Drones` after task completion.
 */
public class Scheduler extends Thread {
    private DatagramSocket fireIncidentSocket, droneSocket;
    private InetAddress fireIncidentAddress;
    private int fireIncidentPort;

    protected final ConcurrentHashMap<Integer, Event> fullyServicedEvents;

    protected final PriorityBlockingQueue<Event> eventQueue;// Queue for fire events

    protected final ConcurrentLinkedQueue<String> logQueue;// Queue for logs (used in GUI)
    protected final ConcurrentHashMap<Integer, HashMap<String, Object>> allDroneList;// TODO might want to add a passive
                                                                                     // drone object and change the
                                                                                     // drone we have now into drone
                                                                                     // subsystem
    private final CopyOnWriteArrayList<Integer> freeDroneList; // Contains a list of all free drones
    private final CopyOnWriteArrayList<Integer> faultedDroneList; // Contains a list of all faulted drones
    protected boolean finish; // Flag to stop the scheduler when all tasks are complete

    private SchedulerState currentState;
    private SchedulerFSM schedulerFSM;

    /**
     * Constructor for the Scheduler class.
     *
     */
    public Scheduler(int fireIncidentReceivePort, int droneReceivePort) {
        this.finish = false; // Initially, the scheduler runs continuously

        this.eventQueue = new PriorityBlockingQueue<>();
        this.fullyServicedEvents = new ConcurrentHashMap<>();

        this.logQueue = new ConcurrentLinkedQueue<>();

        this.allDroneList = new ConcurrentHashMap<>();
        this.freeDroneList = new CopyOnWriteArrayList<>();
        this.faultedDroneList = new CopyOnWriteArrayList<>();

        try {
            fireIncidentSocket = new DatagramSocket(fireIncidentReceivePort);
            droneSocket = new DatagramSocket(droneReceivePort);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.schedulerFSM = new SchedulerFSM();
        this.schedulerFSM.initialize();
        this.currentState = SchedulerFSM.getState("Idle");
    }

    public void setState(String stateName) {
        this.currentState = SchedulerFSM.getState(stateName);
    }

    public String getStateAsString() {
        return this.currentState.getStateString();
    }

    /**
     * The main execution loop of the Scheduler.
     * - Continuously checks for new fire requests.
     * - Assigns fire events to available drones.
     * - If no drones are available, it waits for one to become free.
     * - If a drone fails to complete its task, the fire request is requeued.
     */
    @Override
    public void run() {
        while (!finish) {
            System.out.println("[Scheduler] Entering " + getStateAsString() + " state");
            logQueue.add("[Scheduler] Entering " + getStateAsString() + " state");
            this.currentState.action(this);
        }
    }

    public void idleAction() {
        try {
            DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
            this.fireIncidentSocket.setSoTimeout(0);
            this.fireIncidentSocket.receive(packet);

            String message = new String(packet.getData(), 0, packet.getLength());

            if (message.equalsIgnoreCase("ACTIVATE")) {
                this.currentState.handleOn(this);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void activeAction() {
        // Start a separate thread to handle drone responses asynchronously
        new Thread(this::processDroneMessages).start();
        new Thread(this::processFireIncidentMessages).start();

        while (!finish) {
            try {
                // Waits for there to be an available event and drone
                while (eventQueue.isEmpty() || freeDroneList.isEmpty()) {
                    synchronized (this) {
                        wait();
                        if (finish) {
                            return;
                        }
                    }
                }

                // Gets the event with the highest priority
                Event event = this.eventQueue.take();

                // Sends the first drone in freeDroneList to the event
                // TODO can be improved by picking the closest drone instead of just the first
                for (Integer id : this.freeDroneList) {
                    event.setAgentSent(event.getAgentSent() + (double) allDroneList.get(id).get("volume"));

                    // Checks if event still requires more agent
                    if (event.getAgentSent() < (event.getAgentRequired() - 0.0001)) {
                        // Add event back to queue if it still requires more agent
                        this.eventQueue.put(event);
                    } else {
                        this.fullyServicedEvents.put(event.getId(), event);
                    }

                    this.freeDroneList.remove(id);

                    sendToDrone(event, id);
                    break;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void processFireIncidentMessages() {
        while (!finish) {
            try {
                DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);

                fireIncidentSocket.setSoTimeout(1000);
                fireIncidentSocket.receive(packet);
                fireIncidentAddress = packet.getAddress();
                fireIncidentPort = packet.getPort();

                String message = new String(packet.getData(), 0, packet.getLength());
                String[] splitMessage = message.split(":");
                switch (splitMessage[0].toUpperCase()) {
                    case "NEW_EVENT":
                        Event event = Event
                                .deserializeEvent(Arrays.copyOfRange(packet.getData(), 10, packet.getLength()));
                        System.out.println("[Scheduler]: Event Received: " + event.toString());
                        logQueue.add("[Scheduler]: Event Received: " + event);
                        // INSERT INTO PRIORITY QUEUE
                        eventQueue.put(event);
                        synchronized (this) {
                            notifyAll();
                        }
                        System.out.println("[Scheduler]: Added event to eventQueue");
                        logQueue.add("[Scheduler]: Added event to eventQueue");
                        break;
                    case "FINISH":
                        System.out.println("[Scheduler]: Received: FINISH");
                        logQueue.add("[Scheduler]: Received: FINISH");
                        this.finishEvents();
                        break;
                    case "ACTIVATE":
                        // Ignore
                        break;
                    default:
                        System.out.println("Invalid message: " + message);
                }
            } catch (SocketTimeoutException e) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Monitors drone responses in a separate thread.
     * - Continuously listens for responses from drones.
     * - If a drone fails its mission, the fire event is requeued for
     * reassignment.
     * - If drones were previously marked as busy, notifies the scheduler that a
     * drone is free.
     */
    private void processDroneMessages() {
        while (!finish) { // Runs continuously until the scheduler is stopped
            try {
                // Waits for a drone to send a response (Blocking call)
                DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                droneSocket.setSoTimeout(1000);
                droneSocket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());

                String[] splitMessage = message.split(":");
                if(!splitMessage[0].equals("FAULT_EVENT")) {
                    System.out.println("[Scheduler] Received: " + message);
                }
                logQueue.add("[Scheduler] Received: " + message);

                HashMap<String, Object> localHashMap;
                int id;
                switch (splitMessage[0]) {
                    case "ONLINE": // ONLINE:DRONE_ID
                        HashMap<String, Object> droneHashMap = new HashMap<>();
                        droneHashMap.put("port", packet.getPort());
                        droneHashMap.put("address", packet.getAddress());
                        droneHashMap.put("volume", 15.0);
                        droneHashMap.put("location", new Integer[] { 0, 0 });
                        droneHashMap.put("state", "Online");
                        this.allDroneList.put(Integer.parseInt(splitMessage[1]), droneHashMap);
                        this.freeDroneList.add(Integer.parseInt(splitMessage[1]));

                        sendToDrone("OK", Integer.parseInt(splitMessage[1]));

                        // Notify the scheduler (run method) that a drone is available for a new
                        // assignment
                        synchronized (this) {
                            notifyAll();
                        }
                        break;
                    case "LOCATION":
                        id = Integer.parseInt(splitMessage[1]);
                        localHashMap = this.allDroneList.get(id);
                        Integer[] currentLocation = {Integer.parseInt(splitMessage[2]), Integer.parseInt(splitMessage[3])};
                        localHashMap.put("location", currentLocation);
                        this.allDroneList.put(id, localHashMap);
                        this.sendToDrone("OK", id);
                        break;

                    case "En Route": {
                        // Tells drone how much agent to drop
                        // TODO calculate amount of agent to drop
                        //update current state for gui
                        id = Integer.parseInt(splitMessage[1]);
                        localHashMap = this.allDroneList.get(id);
                        localHashMap.put("state", "En Route");
                        this.allDroneList.put(id, localHashMap);
                        double agentDropAmount = 15.0;
                        int eventId = Integer.parseInt(splitMessage[2]);
                        Event event = null;

                        // Finds the relevant event
                        if (this.fullyServicedEvents.containsKey(eventId)) {
                            event = this.fullyServicedEvents.get(eventId);
                        } else {
                            for (Event e : this.eventQueue) {
                                if (e.getId() == eventId) {
                                    event = e;
                                    break;
                                }
                            }
                        }

                        if (event.getAgentRequired() < 14.9999) {
                            agentDropAmount = event.getAgentRequired();
                        }

                        sendToDrone(String.format("DROP:%.2f", agentDropAmount), Integer.parseInt(splitMessage[1]));
                        break;
                    }
                    case "Dropping Agent": {// Dropping Agent:droneId:eventId:agentDropAmount:carryVolume

                        //update current state for gui
                        id = Integer.parseInt(splitMessage[1]);
                        localHashMap = this.allDroneList.get(id);
                        localHashMap.put("state", "Dropping Agent");
                        this.allDroneList.put(id, localHashMap);


                        // Update allDroneList with proper drone carryVolume
                        this.allDroneList.get(Integer.parseInt(splitMessage[1])).put("volume",
                                Double.parseDouble(splitMessage[4]));

                        // TODO can be added later once NEW_EVENT case is added to some sendReceive
                        // Add drone back to freeDroneList if it still has some agent in tank
                        // if(Double.parseDouble(splitMessage[4]) > 0.0001){
                        // this.freeDroneList.add(Integer.parseInt(splitMessage[1]));
                        // }

                        int eventId = Integer.parseInt(splitMessage[2]);
                        if (this.fullyServicedEvents.containsKey(eventId)) {
                            Event event = this.fullyServicedEvents.get(eventId);
                            double agentRequired = event.getAgentRequired() - Double.parseDouble(splitMessage[3]);
                            if (agentRequired < 0.0001) {
                                event.setAgentRequired(0.0);
                                event.setAgentSent(0.0);
                                event.setSeverity(Event.Severity.OUT);
                                this.fullyServicedEvents.remove(eventId);
                                byte[] msg = ("SUCCESS:" + splitMessage[1] + ":" + splitMessage[2]).getBytes();
                                this.fireIncidentSocket.send(
                                        new DatagramPacket(msg, msg.length, fireIncidentAddress, fireIncidentPort));// forward
                                                                                                                    // drone
                                                                                                                    // messages
                                                                                                                    // to
                                                                                                                    // FireIncident
                            } else {
                                event.setAgentRequired(agentRequired);
                                event.setAgentSent(event.getAgentSent() - Double.parseDouble(splitMessage[3]));
                            }
                        } else {
                            for (Event event : this.eventQueue) {
                                if (event.getId() == eventId) {
                                    event.setAgentRequired(
                                            event.getAgentRequired() - Double.parseDouble(splitMessage[3]));
                                    event.setAgentSent(event.getAgentSent() - Double.parseDouble(splitMessage[3]));
                                    break;
                                }
                            }
                        }

                        sendToDrone("OK", Integer.parseInt(splitMessage[1]));

                        // TODO check previous
                        // Notify the scheduler (run method) that a drone is available for a new
                        // assignment
                        // synchronized (this){
                        // notifyAll();
                        // }
                        break;
                    }
                    case "Returning To Base":
                        //updating state for gui
                        id = Integer.parseInt(splitMessage[1]);
                        localHashMap = this.allDroneList.get(id);
                        localHashMap.put("state", "Returning to Base");
                        this.allDroneList.put(id, localHashMap);
                        sendToDrone("OK", Integer.parseInt(splitMessage[1]));
                        break;
                    case "Filling Tank":
                        //updating state for gui
                        id = Integer.parseInt(splitMessage[1]);
                        localHashMap = this.allDroneList.get(id);
                        localHashMap.put("state", "Filling Tank");
                        this.allDroneList.put(id, localHashMap);

                        this.allDroneList.get(Integer.parseInt(splitMessage[1])).put("volume", 15.0);
                        // If drone is not in freeDroneList or faultedDroneList then add it to
                        // freeDroneList
                        if (!freeDroneList.contains(Integer.parseInt(splitMessage[1]))
                                && !this.faultedDroneList.contains(Integer.parseInt(splitMessage[1]))) {
                            this.freeDroneList.add(Integer.parseInt(splitMessage[1]));
                        }

                        sendToDrone("OK", Integer.parseInt(splitMessage[1]));

                        // Notify the scheduler (run method) that a drone is available for a new
                        // assignment
                        synchronized (this) {
                            notifyAll();
                        }
                        break;
                    case "FAULT_EVENT":
                        //updating  state for gui
                        id = Integer.parseInt(splitMessage[1]);
                        localHashMap = this.allDroneList.get(id);
                        localHashMap.put("state", "FAULT");
                        this.allDroneList.put(id, localHashMap);


                        byte[] receivedData = Arrays.copyOfRange(packet.getData(), 13+splitMessage[1].length(), packet.getLength());
                        FaultEvent fault = FaultEvent.deserializeFaultEvent(receivedData);
                        if (fault != null) {
                            int droneId = fault.getDroneID();
                            FaultEvent.Type faultType = fault.getFaultType();

                            System.out.println("[Scheduler] Fault Received: " + fault.toString());
                            switch (faultType) {
                                case NOZZLE_JAM:
                                    handleNozzleJam(fault);
                                    break;
                                case STUCK_IN_FLIGHT:
                                    handleStuckDrone(fault);
                                    break;
                            }

                            //Find the event using the eventId
                            int eventId = fault.getEvent().getId();
                            Event event = null;
                            if (this.fullyServicedEvents.containsKey(eventId)) {
                                event = this.fullyServicedEvents.get(eventId);
                            } else {
                                for (Event e : this.eventQueue) {
                                    if (e.getId() == eventId) {
                                        event = e;
                                        break;
                                    }
                                }
                            }

                            //Calculate the new agentSent for the event
                            double agentReceived = Double.parseDouble(splitMessage[1]);
                            double agentSent = event.getAgentSent() - agentReceived;
                            event.setAgentSent(agentSent);

                            sendToDrone("OK", droneId);
                            continue;
                        }
                        break;
                    default:
                        System.out.println("Invalid message: " + message);
                }
            } catch (SocketTimeoutException e) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendToDrone(Event event, int droneId) {
        byte[] message = event.createMessage("NEW_EVENT:");
        System.out.println("[Scheduler] Sent Drone " + droneId + ": " + event);
        logQueue.add("[Scheduler] Sent Drone " + droneId + ": " + event);
        DatagramPacket packet = new DatagramPacket(message, message.length,
                (InetAddress) this.allDroneList.get(droneId).get("address"),
                (int) this.allDroneList.get(droneId).get("port"));
        try {
            droneSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendToDrone(String s, int droneId) {
        System.out.println("[Scheduler] Sent Drone " + droneId + ": " + s);
        logQueue.add("[Scheduler] Sent Drone " + droneId + ": " + s);
        byte[] data = s.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length,
                (InetAddress) this.allDroneList.get(droneId).get("address"),
                (int) this.allDroneList.get(droneId).get("port"));
        try {
            droneSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void finishDrones() {
        for (Integer id : allDroneList.keySet()) {
            HashMap<String, Object> drone = allDroneList.get(id);
            sendToDrone("FINISH", id);
            System.out.println("[Scheduler]: Sent to Drone " + id + ": FINISH");
            logQueue.add("[Scheduler]: Sent to Drone " + id + ": FINISH");
        }
    }

    /**
     * Called by FireIncident when all fire events have been processed.
     *
     * - Sets the finish flag to true, signaling all loops to exit.
     * - Notifies any waiting threads to avoid deadlocks.
     */
    public synchronized void finishEvents() {
        this.finish = true; // Stop execution of scheduler and monitoring threads
        notifyAll(); // Notify waiting threads to prevent indefinite blocking
        finishDrones();
        System.out.println("[Scheduler] Shutting down...");
        logQueue.add("[Scheduler] Shutting down...");
    }

    private void handleNozzleJam(FaultEvent fault) {
        int droneId = fault.getDroneID();
        Event event = fault.getEvent();

        // sendToDrone("RETURN_TO_BASE", droneId);
        
        eventQueue.put(event);
        System.out.println("Event requeued: " + event.toString());

        faultedDroneList.add(droneId);
        freeDroneList.remove(Integer.valueOf(droneId));
        System.out.println("[Scheduler] Drone " + droneId + " added to faulted list and removed from free list!");
    }

    private void handleStuckDrone(FaultEvent fault) {
        int droneId = fault.getDroneID();
        Event event = fault.getEvent();
        // sendToDrone("RETURN_TO_BASE", droneId);

        eventQueue.put(event);
        System.out.println("Event requeued: " + event.toString());

        faultedDroneList.add(droneId);
        freeDroneList.remove(Integer.valueOf(droneId));

        System.out.println("[Scheduler] Drone " + droneId + " stuck in flight!");
        System.out.println("[Scheduler] Drone " + droneId + " added to faulted list and removed from free list!");
    }

    public static void main(String[] args) {
        Scheduler scheduler = new Scheduler(5000, 6000);
        scheduler.start();
    }
}
