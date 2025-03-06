import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;

/*
 * TODOS:
 * -implement drone tracking
 * -priorityblockingqueue for events
 * -add parsed events to queue
 * -remove events when they are claimed downed by the drones
 * -pick the highest priority event to send out next
 * 
 * -implement UDP compliance:
 * -designate ports for sending/receiving with everyone DRONE NEEDED
 * -set up socket for receiving from FireIncident/Drone DONE
 * -unpack incoming packets -> either add to eventqueue or remove from eventqueue NEED TO PUT IN QUEUES
 * -set up socket for sending to drones
 * -pack outgoiing packets to drones
 */

/**
 * The Scheduler class is responsible for:
 * - Receiving fire events from the FireIncident subsystem.
 * - Assigning fire events to available drones.
 * - Managing drone availability and reassigning failed missions.
 * - Handling drone responses asynchronously using a separate monitoring thread.
 * 
 * The Scheduler runs as a separate thread and interacts with the system via two
 * queues:
 * - `sharedFireQueue` → Receives fire requests from `FireIncident`.
 * - `responseQueue` → Receives updates from `Drones` after task completion.
 */
public class Scheduler extends Thread {
    private DatagramSocket FISocket, droneSocket;
    private InetAddress FI_addr;
    private int FI_port;

    private final GenericQueue<Event> sharedFireQueue; // Queue for incoming fire events
    private final GenericQueue<DroneResponse> responseQueue; // Queue for drone responses

    private final PriorityBlockingQueue<Event> eventQueue;//Queue for fire events
    private  CopyOnWriteArrayList<Object[]> freeDroneList;//List of free drones
    
    //private final PriorityBlockingQueue<Event> eventQueue;//Queue for events

    private final ArrayList<Drone> drones; // List of drones managed by the scheduler
    private boolean finish; // Flag to stop the scheduler when all tasks are complete
    private boolean areAllDronesBusy; // Tracks whether all drones are currently occupied

    private boolean freeDroneListInUse;

    /**
     * Constructor for the Scheduler class.
     * 
     * @param sharedFireQueue Queue containing fire events from the FireIncident
     *                        subsystem.
     * @param responseQueue   Queue where drones report task success/failure.
     * @param drones          List of all drones available in the system.
     */
    public Scheduler(GenericQueue<Event> sharedFireQueue, GenericQueue<DroneResponse> responseQueue,
                     ArrayList<Drone> drones) {
        this.finish = false; // Initially, the scheduler runs continuously
        this.sharedFireQueue = sharedFireQueue;
        this.responseQueue = responseQueue;
        this.drones = drones;
        this.areAllDronesBusy = false; // Initially, some drones are expected to be available
        this.eventQueue = new PriorityBlockingQueue<>();
        this.freeDroneList = new CopyOnWriteArrayList<>();
        this.freeDroneListInUse = false;
    }

    private void handleFireIncident(){
        byte[] buffer = new byte[512];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try{
            FISocket.setSoTimeout(100);
            FISocket.receive(packet);
            FI_addr = packet.getAddress();
            FI_port = packet.getPort();
            Event event = Event.deserializeEvent(packet.getData());
            System.out.println("[Scheduler]: Event Received: " + event.toString());
            //INSERT INTO PRIORITY QUEUE
            eventQueue.add(event);
            System.out.println("[Scheduler]: Added event to eventQueue");
        }catch(SocketTimeoutException e){
            System.out.println("[Scheduler]: No pending FireIncidents");
            return;
        }catch(IOException e){
            e.printStackTrace();
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
     * 
     * - Continuously checks for new fire requests.
     * - Assigns fire events to available drones.
     * - If no drones are available, it waits for one to become free.
     * - If a drone fails to complete its task, the fire request is requeued.
     */
    @Override
    public void run() {
        try{
            FISocket = new DatagramSocket(5000);
            droneSocket = new DatagramSocket(6000);
        }catch(IOException e){
            e.printStackTrace();
        }


        /*
        handleFireIncident();//REPLACE TO CORRECT PLACEMENT
        handleFireIncident();
        try{
            System.out.println("First item out of the priority queue: " + eventQueue.take().toString());//change to send message to drone
        }catch(InterruptedException e){
            e.printStackTrace();
        }

        */
        

        
        // Start a separate thread to handle drone responses asynchronously
        new Thread(this::monitorDroneResponses).start();

        while (!finish) { // Scheduler runs until it is told to stop
                handleFireIncident();

            try {
                // If all drones are busy, the scheduler waits for an available drone
                while (areAllDronesBusy) {
                    synchronized (this) {
                        wait(); // Waits until a drone becomes free (notified by monitorDroneResponses)
                    }
                }

                
                // Retrieve the next fire request from the queue (Blocking call - waits if queue is empty)
                Event event = eventQueue.take();
                /*
                Event fireRequest = sharedFireQueue.get();
                System.out.println("[Scheduler] Received request: " + fireRequest);
                */

                // Assign fire request to an available drone
                if(freeDroneList.isEmpty()){
                    System.out.println("[Scheduler]: No available drones");
                }else{
                    if(freeDroneListInUse){
                        synchronized (this) {
                            wait();
                        }
                    }
                    freeDroneListInUse = true;
                    for(Object[] drone: freeDroneList){
                        sendToDrone(event, (int) drone[0], (InetAddress) drone[1]);
                        freeDroneList.remove(drone);
                        event.addAssignedDrone(drone);//assign drone to event
                        System.out.println("[Scheduler]: Assigning Drone to event: " + event);
                    }
                    freeDroneListInUse = false;
                    synchronized (this) {
                        notifyAll();
                    }
                    
                }
                
                /*
                boolean assigned = false;
                for (Drone drone : drones) {
                    if (drone.isFree()) { // Check if the drone is free
                        drone.assignFire(fireRequest); // Assign the fire to the drone
                        System.out.println("[Scheduler] Assigned request to " + drone);
                        assigned = true;
                        break; // Stop searching once an available drone is found
                    }
                }
                

                // If no drones are available, requeue the fire event
                if (!assigned) {
                    System.out.println("[Scheduler] No available drones. Requeuing request...");
                    this.areAllDronesBusy = true; // Mark all drones as busy
                    sharedFireQueue.add(fireRequest); // Put the request back into the queue
                }
                */

            } catch (InterruptedException e) {
                e.printStackTrace(); // Handle unexpected interruptions
            }
        }
    }

    /**
     * Monitors drone responses in a separate thread.
     * 
     * - Continuously listens for responses from drones.
     * - If a drone fails its mission, the fire event is requeued for
     * reassignment.
     * - If drones were previously marked as busy, notifies the scheduler that a
     * drone is free.
     */
    private void monitorDroneResponses() {
        while (!finish) { // Runs continuously until the scheduler is stopped
                // Waits for a drone to send a response (Blocking call)
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try{
                    droneSocket.setBroadcast(true);
                    droneSocket.receive(packet);
                    //handle status update packets
                    String data = new String(packet.getData());
                    if(data.contains("ONLINE")){//change to correct message
                        if(freeDroneListInUse){
                            synchronized(this){
                                wait();
                            }
                        }
                        freeDroneListInUse = true;
                        freeDroneList.add(new Object[]{packet.getPort(), packet.getAddress()});//add drone to free drone list
                        System.out.println("[Scheduler]: Adding Drone to free drone list...");
                        freeDroneListInUse = false;
                        synchronized(this){
                            notifyAll();
                        }
                        sendToDrone("RECEIVED", packet.getPort(), packet.getAddress());//send response to drone
                        continue; //restart loop for next message
                    }

                    //handle drone response message
                    DroneResponse response = DroneResponse.deserializeResponse(packet.getData());
                    System.out.println("[Scheduler] Processing Drone Response: " + response);

                    //handling response types
                    switch (response.getResponseType()){
                        case DroneResponse.ResponseType.FAILURE:
                            System.out.println("[Scheduler] Drone " + response.getDroneId() + "failed! Reassigning fire: " + response.getEvent());
                            eventQueue.add(response.getEvent());
                            break;
                        case DroneResponse.ResponseType.REFILL_REQUIRED:
                            System.out.println("[Scheduler] Drone " + response.getDroneId() + " needs refill. Will be available soon.");
                            //no further action, drone will RTB and refill automatically
                            break;
                        case DroneResponse.ResponseType.SUCCESS:
                            System.out.println("[Scheduler] Drone " + response.getDroneId() + " successfully extinguished fire: " + response.getEvent());
                            eventQueue.remove(response.getEvent());

                            /*
                            //send a message to all other drones that are currently working this event 
                            if(freeDroneListInUse){
                                synchronized(this){
                                    wait();
                                }
                            }
                            freeDroneListInUse = true;
                            for(Object[] drone: response.getEvent().getAssignedDrones()){
                                //send stop message to each drone -> Code in stop method on drone and write function to send message
                                sendToDrone("STOP", (int) drone[0], (InetAddress) drone[1]);
                            }
                            freeDroneListInUse = false;
                            synchronized (this) {
                                notifyAll();
                            }
                            */
                            
                            break;    
                    }
        
                }catch(IOException e){
                    e.printStackTrace();
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
                /*
                DroneResponse response = responseQueue.get();
                System.out.println("[Scheduler] Processing Drone Response: " + response);
                DroneResponse.ResponseType responseType = response.getResponseType();

                // Handle different drone response cases
                if (responseType == DroneResponse.ResponseType.FAILURE) {
                    System.out.println("[Scheduler] Drone " + response.getDroneId() + " failed! Reassigning fire: "
                            + response.getEvent());
                    sharedFireQueue.add(response.getEvent()); // Reassign the fire request
                } else if (responseType == DroneResponse.ResponseType.REFILL_REQUIRED) {
                    System.out.println("[Scheduler] Drone " + response.getDroneId() + " needs refill. Will be available soon.");
                    // The drone will return to base & refill automatically, no requeue needed.
                } else if (responseType == DroneResponse.ResponseType.SUCCESS) {
                    System.out.println("[Scheduler] Drone " + response.getDroneId() + " successfully extinguished fire: "
                            + response.getEvent());
                    
                }
                */

                // A drone has completed its mission (success, failure, or refill request),
                // so at least one drone is now available.
                this.areAllDronesBusy = false;

                // Notify the scheduler (run method) that a drone is available for a new
                // assignment
                synchronized (this) {
                    notifyAll();
                }
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
        FISocket.close();
        droneSocket.close();
        System.out.println("[Scheduler] Shutting down...");
    }
}
