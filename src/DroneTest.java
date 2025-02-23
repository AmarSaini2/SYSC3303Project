import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Drone class.
 * This class tests the functionality of sending fire requests
 * and receiving updates from the Scheduler.
 */
public class DroneTest {
    private GenericQueue<Event> sharedFireQueue; // Queue for incoming fire events
    private GenericQueue<DroneResponse> responseQueue; // Queue for drone responses
    private ArrayList<Drone> drones;
    private Scheduler scheduler;
    private Drone drone;
    private LocalTime time;

    /**
     * Sets up the test environment before each test case.
     * Initializes a new instance of the Scheduler and Drone classes.
     */
    @BeforeEach
    public void setUp() {
        sharedFireQueue = new GenericQueue<>();
        responseQueue = new GenericQueue<>();
        drones = new ArrayList<>();
        scheduler = new Scheduler(sharedFireQueue, responseQueue, drones);
        drone = new Drone(responseQueue);
        drones.add(drone);  // Add drone to the scheduler's list of drones

        // Start the scheduler in a separate thread
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.start();
    }

    /**
     * Tests the functionality of sending a fire request to the Scheduler
     * and verifying that the update is received after the fire is dealt with.
     *
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    @Test
    public void testSendToFire() throws InterruptedException {
        // Create the zone and event for the test
        Zone zone = new Zone(1, 0, 0, 700, 600);
        Event fireEvent = new Event(LocalTime.now(), zone, Event.Type.FIRE_DETECTED, Event.Severity.HIGH);

        // Send fire request to scheduler
        sharedFireQueue.add(fireEvent);

        // Start drone thread to handle fire
        Thread droneThread = new Thread(drone);
        droneThread.start();

        // Wait for the drone to process the fire event and send a response
        DroneResponse response = null;
        boolean receivedResponse = false;

        // Wait for a response from the drone (either SUCCESS, FAILURE, or REFILL_REQUIRED)
        long startTime = System.currentTimeMillis();
        while (!receivedResponse && (System.currentTimeMillis() - startTime) < 10000) { // Timeout after 20 seconds
            if (!responseQueue.isEmpty()) {
                response = responseQueue.get();
                receivedResponse = true;
            }
            Thread.sleep(100); // Allow the drone some time to process
        }

        // Validate the response type
        assertEquals(DroneResponse.ResponseType.SUCCESS, response.getResponseType());

        // Verify if the event status was updated correctly
        Event newFireStatus = drone.getAssignedFire();
        assertNotNull(newFireStatus);
        assertEquals(Event.Type.FIRE_DETECTED, newFireStatus.getType());
        assertEquals(Event.Severity.OUT, newFireStatus.getSeverity());
    }
}
