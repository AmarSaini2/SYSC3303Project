import Event.Severity;
import java.util.ArrayList;

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
    private final GenericQueue<Event> sharedFireQueue; // Queue for incoming fire events
    private final GenericQueue<DroneResponse> responseQueue; // Queue for drone responses
    private final ArrayList<Drone> drones; // List of drones managed by the scheduler
    private boolean finish; // Flag to stop the scheduler when all tasks are complete
    private boolean areAllDronesBusy; // Tracks whether all drones are currently occupied

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
        // Start a separate thread to handle drone responses asynchronously
        new Thread(this::monitorDroneResponses).start();

        while (!finish) { // Scheduler runs until it is told to stop
            try {
                // If all drones are busy, the scheduler waits for an available drone
                while (areAllDronesBusy) {
                    synchronized (this) {
                        wait(); // Waits until a drone becomes free (notified by monitorDroneResponses)
                    }
                }

                // Retrieve the next fire request from the queue (Blocking call - waits if queue is empty)
                Event fireRequest = sharedFireQueue.get();
                System.out.println("[Scheduler] Received request: " + fireRequest);

                // Assign fire request to an available drone
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
            try {
                // Waits for a drone to send a response (Blocking call)
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
                    response.getEvent().setSeverity(Event.Severity.OUT);
                    
                }

                // A drone has completed its mission (success, failure, or refill request),
                // so at least one drone is now available.
                this.areAllDronesBusy = false;

                // Notify the scheduler (run method) that a drone is available for a new
                // assignment
                synchronized (this) {
                    notifyAll();
                }
            } catch (InterruptedException e) {
                e.printStackTrace(); // Handle unexpected interruptions
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
        System.out.println("[Scheduler] Shutting down...");
    }
}
