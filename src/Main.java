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
       // Drone drone0 = new Drone(6000, "src/droneFaultInjection_0.txt");
        Drone drone0 = new Drone(6000);
        Drone drone1 = new Drone(6000);
        Drone drone2 = new Drone(6000);

        TimeStampDaemon.startDaemon();

        // Create the scheduler which will manage drone assignments and event processing
        Scheduler scheduler = new Scheduler(5000, 6000);

        View view = new View(scheduler);

        // Create the FireIncident subsystem that will read fire incidents from a file
        FireIncident fireIncident = new FireIncident("src/Event_File.csv", "src/Zone_File.csv", 5000);

        // Start the FireIncident subsystem (reads fire events and sends them to the
        // scheduler)
        fireIncident.start();

        // Start the Scheduler (assigns drones to fire incidents)
        scheduler.start();

        view.start();

        drone0.start();
        drone1.start();
        drone2.start();


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
            drone0.join();
            drone1.join();
            drone2.join();
        } catch (InterruptedException e) {
            e.printStackTrace(); // Handle any interruption in thread execution
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}