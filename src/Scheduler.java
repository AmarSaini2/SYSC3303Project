import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
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

    private final ConcurrentHashMap<Integer, Event> fullyServicedEvents;

    private final PriorityBlockingQueue<Event> eventQueue;//Queue for fire events

    private final ConcurrentHashMap<Integer, HashMap<String, Object>> allDroneList;//TODO might want to add a passive drone object and change the drone we have now into drone subsystem
    private final CopyOnWriteArrayList<Integer> freeDroneList; //Contains a list of all free drones
    private final CopyOnWriteArrayList<Integer> faultedDroneList; //Contains a list of all faulted drones

    private boolean finish; // Flag to stop the scheduler when all tasks are complete

    /**
     * Constructor for the Scheduler class.
     *
     */
    public Scheduler(int fireIncidentReceivePort, int droneReceivePort) {
        this.finish = false; // Initially, the scheduler runs continuously
        this.eventQueue = new PriorityBlockingQueue<>();

        this.allDroneList = new ConcurrentHashMap<>();
        this.freeDroneList = new CopyOnWriteArrayList<>();
        this.faultedDroneList = new CopyOnWriteArrayList<>();

        try{
            fireIncidentSocket = new DatagramSocket(fireIncidentReceivePort);
            droneSocket = new DatagramSocket(droneReceivePort);
        }catch(IOException e){
            throw new RuntimeException(e);
        }

        this.fullyServicedEvents = new ConcurrentHashMap<>();
    }

    private void handleFireIncident(){
        while (!finish) {
            try {
                DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);

                fireIncidentSocket.receive(packet);
                fireIncidentAddress = packet.getAddress();
                fireIncidentPort = packet.getPort();

                String message = new String(packet.getData(), 0, packet.getLength());
                String[] splitMessage = message.split(":");

                switch (splitMessage[0].toUpperCase()){
                    case "FINISH":
                        System.out.println("[Scheduler]: Received: FINISH");
                        this.finishEvents();
                        break;
                    default:
                        Event event = Event.deserializeEvent(packet.getData());
                        System.out.println("[Scheduler]: Event Received: " + event.toString());
                        //INSERT INTO PRIORITY QUEUE
                        eventQueue.put(event);
                        synchronized (this){
                            notifyAll();
                        }
                        System.out.println("[Scheduler]: Added event to eventQueue");
                }
            } catch (SocketTimeoutException e) {
                System.out.println("[Scheduler]: No pending Fire Incidents");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void sendToDrone(Event event, int port, InetAddress address){
        byte[] data = event.serializeEvent();
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        try {
            droneSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendToDrone(String s, int port, InetAddress address){
        byte[] data = s.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        try {
            droneSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        // Start a separate thread to handle drone responses asynchronously
        new Thread(this::monitorDroneResponses).start();
        new Thread(this::handleFireIncident).start();

        while(!finish){
            try {
                while (eventQueue.isEmpty() || freeDroneList.isEmpty()) {
                    synchronized (this) {
                        wait();
                        if(finish){
                            return;
                        }
                    }
                }

                //Gets the event with the highest priority
                Event event = this.eventQueue.take();
                event.setAgentSent(event.getAgentSent()+15.0); //TODO get rid of 15.0 constant using the amount of agent left in drone tank

                //Checks if event still requires more agent
                if(event.getAgentSent() < event.getAgentRequired()){
                    //Add event back to queue if it still requires more agent
                    this.eventQueue.put(event);
                }else{
                    this.fullyServicedEvents.put(event.getId(), event);
                }

                //Sends the first drone in freeDroneList to the event
                for(Integer id: this.freeDroneList){
                    HashMap<String, Object> drone = this.allDroneList.get(id);
                    sendToDrone(event, (int) drone.get("port"), (InetAddress) drone.get("address"));
                    this.freeDroneList.remove(id);
                    System.out.println("[Scheduler]: Assigned Drone to event: " + event);
                    break;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
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
    private void monitorDroneResponses() {
        while (!finish) { // Runs continuously until the scheduler is stopped
            // Waits for a drone to send a response (Blocking call)
            byte[] buffer = new byte[2048];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try{
                droneSocket.setBroadcast(true);
                droneSocket.receive(packet);

                //handle status update packets
                String message = new String(packet.getData(),0 , packet.getLength());
                String[] splitMessage = message.split(":");
                switch (splitMessage[0].toUpperCase()){
                    case "ONLINE": //ONLINE:DRONE_ID
                        HashMap<String, Object> droneHashMap = new HashMap<>();
                        droneHashMap.put("port", packet.getPort());
                        droneHashMap.put("address", packet.getAddress());
                        droneHashMap.put("volume", 15.0);
                        droneHashMap.put("location", new Integer[]{0,0});
                        this.allDroneList.put(Integer.parseInt(splitMessage[1]), droneHashMap);
                        this.freeDroneList.add(Integer.parseInt(splitMessage[1]));

                        System.out.println("[Scheduler]: Adding Drone to free drone list...");
                        synchronized(this){
                            notifyAll();
                        }
                        sendToDrone("RECEIVED", packet.getPort(), packet.getAddress());//send response to drone
                        break;
                    case "FAULTED": //FAILURE:DRONE_ID:EVENT_ID
                        //System.out.println("[Scheduler] Drone " + response.getDroneId() + "failed! Reassigning fire: " + response.getEvent().toString());
                        this.faultedDroneList.add(Integer.parseInt(splitMessage[1]));
                        break;
                    case "REFILL_REQUIRED":
                        //System.out.println("[Scheduler] Drone " + response.getDroneId() + " needs refill. Will be available soon.");
                        //no further action, drone will RTB and refill automatically
                        break;
                    case "SUCCESS": //SUCCESS:DRONE_ID:EVENT_ID:AMOUNT_OF_AGENT
                        //System.out.println("[Scheduler] Drone " + response.getDroneId() + " successfully extinguished fire: " + response.getEvent().toString());
                        synchronized (this) {
                            eventQueue.remove(Integer.parseInt(splitMessage[2]));
                        }
                        freeDroneList.add(Integer.parseInt(splitMessage[1]));
                        fireIncidentSocket.send(new DatagramPacket(packet.getData(), packet.getLength(), fireIncidentAddress, fireIncidentPort));//forward drone messages to FireIncident

                        // Notify the scheduler (run method) that a drone is available for a new assignment
                        synchronized (this){
                            notifyAll();
                        }
                        break;
                }
            }catch (SocketException e){
                return;
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    private void finishDrones(){
        for(Integer id: allDroneList.keySet()){
            HashMap<String, Object> drone = allDroneList.get(id);
            sendToDrone("FINISH", (int) drone.get("port"), (InetAddress) drone.get("address"));
            System.out.println("[Scheduler]: Sent to Drone: FINISH");
        }
        droneSocket.close();
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
    }

    public static void main(String[] args) {
        Scheduler scheduler = new Scheduler(5000, 6000);
        scheduler.start();
    }
}
