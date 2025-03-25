import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
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


//    private final PriorityBlockingQueue<Event> eventQueue;//Queue for fire events
    private final LinkedHashMap<Integer, Event> eventQueue;//Queue for fire events
    private final ConcurrentHashMap<Integer, Object[]> freeDroneList;
    private final ConcurrentHashMap<Integer, Object[]> allDroneList;
//    private final CopyOnWriteArrayList<Object[]> freeDroneList;//List of free drones
//    private final CopyOnWriteArrayList<Object[]> allDroneList;//List of all drones


    private boolean finish; // Flag to stop the scheduler when all tasks are complete
//    private boolean freeDroneListInUse;

    /**
     * Constructor for the Scheduler class.
     *
     */
    public Scheduler(int fireIncidentReceivePort, int droneReceivePort) {
        this.finish = false; // Initially, the scheduler runs continuously
        this.eventQueue = new LinkedHashMap<>();

        this.freeDroneList = new ConcurrentHashMap<>();
        this.allDroneList = new ConcurrentHashMap<>();

        try{
            fireIncidentSocket = new DatagramSocket(fireIncidentReceivePort);
            droneSocket = new DatagramSocket(droneReceivePort);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    private void handleFireIncident(){
        while (eventQueue.isEmpty()) {
            byte[] buffer = new byte[2048];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                fireIncidentSocket.setSoTimeout(1000);
                fireIncidentSocket.receive(packet);
                fireIncidentAddress = packet.getAddress();
                fireIncidentPort = packet.getPort();
                String msg = new String(packet.getData(), 0, packet.getLength());
                if (msg.toUpperCase().equals("FINISH")){
                    System.out.println("[Scheduler]: Received: FINISH");
                    this.finishEvents();

                    return;
                }
                Event event = Event.deserializeEvent(packet.getData());
                System.out.println("[Scheduler]: Event Received: " + event.toString());
                //INSERT INTO PRIORITY QUEUE
//                eventQueue.add(event);
                eventQueue.putLast(event.getId(), event);
                synchronized (this){
                    notifyAll();
                }
                System.out.println("[Scheduler]: Added event to eventQueue");
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

        while (!finish) { // Scheduler runs until it is told to stop
            handleFireIncident();

            if(!finish){
                try {
                    // If all drones are busy, the scheduler waits for an available drone
                    while (freeDroneList.isEmpty()) {
                        synchronized (this) {
                            wait(); // Waits until a drone becomes free (notified by monitorDroneResponses)
                        }
                    }

                    // Retrieve the next fire request from the queue (Blocking call - waits if queue is empty)
                    while (eventQueue.isEmpty()) {
                        synchronized (this) {
                            wait();
                        }
                    }

                    Event event = null;
                    for(Integer id: this.eventQueue.keySet()){
                        event = this.eventQueue.remove(id);
                        break;
                    }

                    // Assign fire request to an available drone
                    if(freeDroneList.isEmpty()){
                        System.out.println("[Scheduler]: No available drones");
                    }else{
                        for(Integer id: freeDroneList.keySet()){
                            Object[] drone = freeDroneList.get(id);
                            sendToDrone(event, (int) drone[0], (InetAddress) drone[1]);
                            freeDroneList.remove(id);
                            event.addAssignedDrone(drone);//assign drone to event
                            System.out.println("[Scheduler]: Assigning Drone to event: " + event);
                            break;
                        }

                    }


                } catch (InterruptedException e) {
                    e.printStackTrace(); // Handle unexpected interruptions
                }
            }
        }

        finishDrones();

        fireIncidentSocket.close();
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
                        this.freeDroneList.put(Integer.parseInt(splitMessage[1]), new Object[]{packet.getPort(), packet.getAddress()});//add drone to free drone list
                        this.allDroneList.put(Integer.parseInt(splitMessage[1]), new Object[]{packet.getPort(), packet.getAddress()});
                        System.out.println("[Scheduler]: Adding Drone to free drone list...");
                        synchronized(this){
                            notifyAll();
                        }
                        sendToDrone("RECEIVED", packet.getPort(), packet.getAddress());//send response to drone
                        break;
                    case "FAILURE": //FAILURE:DRONE_ID:EVENT_ID
                        //System.out.println("[Scheduler] Drone " + response.getDroneId() + "failed! Reassigning fire: " + response.getEvent().toString());
//                        eventQueue.put(response.getEvent());
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
                        freeDroneList.put(Integer.parseInt(splitMessage[1]), new Object[] {packet.getPort(), packet.getAddress()});
                        fireIncidentSocket.send(new DatagramPacket(packet.getData(), packet.getLength(), fireIncidentAddress, fireIncidentPort));//forward drone messages to FireIncident
                        break;
                }
            }catch (SocketException e){
                return;
            }catch(IOException e){
                e.printStackTrace();
            }

            // Notify the scheduler (run method) that a drone is available for a new assignment
            synchronized (this) {
                notifyAll();
            }
        }
    }

    private void finishDrones(){
        for(Integer id: allDroneList.keySet()){
            Object[] drone = allDroneList.get(id);
            sendToDrone("FINISH", (int) drone[0], (InetAddress) drone[1]);
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
        System.out.println("[Scheduler] Shutting down...");
    }

    public static void main(String[] args) {
        Scheduler scheduler = new Scheduler(5000, 6000);
        scheduler.start();
    }
}
