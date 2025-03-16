import java.io.IOException;
import java.net.*;
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
    private DatagramSocket FISocket, droneSocket;
    private InetAddress FI_addr;
    private int FI_port;


    private final PriorityBlockingQueue<Event> eventQueue;//Queue for fire events
    private final CopyOnWriteArrayList<Object[]> freeDroneList;//List of free drones


    private boolean finish; // Flag to stop the scheduler when all tasks are complete
    private boolean freeDroneListInUse;

    /**
     * Constructor for the Scheduler class.
     *
     */
    public Scheduler(int fireIncidentReceivePort, int droneReceivePort) {
        this.finish = false; // Initially, the scheduler runs continuously
        this.eventQueue = new PriorityBlockingQueue<>();
        this.freeDroneList = new CopyOnWriteArrayList<>();
        this.freeDroneListInUse = false;

        try{
            FISocket = new DatagramSocket(fireIncidentReceivePort);
            droneSocket = new DatagramSocket(droneReceivePort);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    private void handleFireIncident(){
        byte[] buffer = new byte[2048];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try{
            FISocket.setSoTimeout(1000);
            FISocket.receive(packet);
            FI_addr = packet.getAddress();
            FI_port = packet.getPort();
            Event event = Event.deserializeEvent(packet.getData());
            System.out.println("[Scheduler]: Event Received: " + event.toString());
            //INSERT INTO PRIORITY QUEUE
            eventQueue.add(event);
            System.out.println("[Scheduler]: Added event to eventQueue");
        }catch(SocketTimeoutException e){
            System.out.println("[Scheduler]: No pending Fire Incidents");
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

            try {
                // If all drones are busy, the scheduler waits for an available drone
                while (freeDroneList.isEmpty()) {
                    synchronized (this) {
                        wait(); // Waits until a drone becomes free (notified by monitorDroneResponses)
                    }
                }


                // Retrieve the next fire request from the queue (Blocking call - waits if queue is empty)
                Event event = eventQueue.take();

                // Assign fire request to an available drone
                if(freeDroneList.isEmpty()){
                    System.out.println("[Scheduler]: No available drones");
                }else{
                    for(Object[] drone: freeDroneList){
                        sendToDrone(event, (int) drone[0], (InetAddress) drone[1]);
                        freeDroneList.remove(drone);
                        event.addAssignedDrone(drone);//assign drone to event
                        System.out.println("[Scheduler]: Assigning Drone to event: " + event);
                    }

                }


            } catch (InterruptedException e) {
                e.printStackTrace(); // Handle unexpected interruptions
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

                FISocket.send(new DatagramPacket(packet.getData(), packet.getLength(), FI_addr, FI_port));//forward drone messages to FireIncident


                //handling response types
                switch (response.getResponseType()){
                    case DroneResponse.ResponseType.FAILURE:
                        //System.out.println("[Scheduler] Drone " + response.getDroneId() + "failed! Reassigning fire: " + response.getEvent().toString());
                        eventQueue.add(response.getEvent());
                        break;
                    case DroneResponse.ResponseType.REFILL_REQUIRED:
                        //System.out.println("[Scheduler] Drone " + response.getDroneId() + " needs refill. Will be available soon.");
                        //no further action, drone will RTB and refill automatically
                        break;
                    case DroneResponse.ResponseType.SUCCESS:
                        //System.out.println("[Scheduler] Drone " + response.getDroneId() + " successfully extinguished fire: " + response.getEvent().toString());
                        eventQueue.remove(response.getEvent());
                        freeDroneList.add(new Object[] {packet.getPort(), packet.getAddress()});

                        break;
                }

            }catch(IOException e){
                e.printStackTrace();
            }catch(InterruptedException e){
                e.printStackTrace();
            }

            // Notify the scheduler (run method) that a drone is available for a new assignment
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
