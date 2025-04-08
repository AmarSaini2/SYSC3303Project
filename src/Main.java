import java.util.Set;
import java.io.IOException;

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
    public static void main(String[] args) throws IOException {
        Drone drone0 = new Drone(6000, "src/droneFaultInjection_0.txt");
        //Drone drone0 = new Drone(6000);
        Drone drone1 = new Drone(6000, "src/droneFaultInjection_0.txt");
        Drone drone2 = new Drone(6000, "src/droneFaultInjection_0.txt");
        Drone drone3 = new Drone(6000, "src/droneFaultInjection_0.txt");
        Drone drone4 = new Drone(6000, "src/droneFaultInjection_0.txt");
        Drone drone5 = new Drone(6000, "src/droneFaultInjection_0.txt");
        Drone drone6 = new Drone(6000, "src/droneFaultInjection_0.txt");
        Drone drone7 = new Drone(6000, "src/droneFaultInjection_0.txt");
        Drone drone8 = new Drone(6000, "src/droneFaultInjection_0.txt");
        Drone drone9 = new Drone(6000, "src/droneFaultInjection_0.txt");

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
        drone3.start();
        drone4.start();
        drone5.start();
        drone6.start();
        drone7.start();
        drone8.start();
        drone9.start();


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
            drone0.start();
            drone1.start();
            drone2.start();
            drone3.start();
            drone4.start();
            drone5.start();
            drone6.start();
            drone7.start();
            drone8.start();
            drone9.start();
        } catch (InterruptedException e) {
            e.printStackTrace(); // Handle any interruption in thread execution
        }
    }
}
