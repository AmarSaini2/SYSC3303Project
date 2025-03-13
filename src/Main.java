import java.time.LocalTime;
import java.util.ArrayList;

/**
 * The Main class initializes and starts the firefighting drone system.
 * 
 * This class is responsible for:
 * - Creating the shared communication queues.
 * - Initializing the FireIncident, Scheduler, and Drones.
 * - Starting each component as a separate thread.
 * - Using join() to ensure all threads complete execution properly.
 * 
 * The system follows a multi-threaded architecture:
 * - FireIncident Thread → Reads fire events from a file and sends them to
 * the scheduler.
 * - Scheduler Thread → Assigns drones to fire events and manages
 * coordination.
 * - Drone Threads → Process assigned fire events and execute fire
 * extinguishing operations.
 */
public class Main {
    public static void main(String[] args) {

        // Create a shared queue for fire incidents to communicate with the scheduler
        GenericQueue<Event> sharedFireQueue = new GenericQueue<Event>();

        // Create a shared queue for drone responses to communicate with the scheduler
        GenericQueue<DroneResponse> droneResponseQueue = new GenericQueue<DroneResponse>();

        // List to hold all drones in the system
        ArrayList<Drone> drones = new ArrayList<Drone>();

        // Define the number of drones in the simulation
        int nDrones = 2; // Currently, only one drone is being added

        // Create and add drones to the system
        for (int i = 0; i < nDrones; i++) {
            Drone drone = new Drone(droneResponseQueue);
            drones.add(drone);
        }

        // Create the scheduler which will manage drone assignments and event processing
        Scheduler scheduler = new Scheduler(sharedFireQueue, droneResponseQueue, drones);

        // Create the FireIncident subsystem that will read fire incidents from a file
        FireIncident fireIncident = new FireIncident("src/Event_File.csv", "src/Zone_File.csv", sharedFireQueue);

        // Start the FireIncident subsystem (reads fire events and sends them to the
        // scheduler)
        fireIncident.start();

        // Start the Scheduler (assigns drones to fire incidents)
        scheduler.start();

        // Start all drones (each drone operates independently)
        for (Drone d : drones) {
            d.start();
        }

        /**
         * Synchronization using `join()`
         * - Ensures the main thread waits until all other threads have finished
         * execution.
         * - Prevents premature termination of the program.
         * - Joins in the order: Scheduler → FireIncident → Drones.
         */
        try {
            scheduler.join(); // Wait for the scheduler to finish execution
            fireIncident.join(); // Wait for the FireIncident subsystem to finish execution
            for (Drone d : drones) {
                d.join(); // Wait for each drone to finish execution
            }
        } catch (InterruptedException e) {
            e.printStackTrace(); // Handle any interruption in thread execution
        }
    }
}
