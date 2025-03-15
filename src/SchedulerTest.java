import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Scheduler class.
 * This class tests the functionality of handling fire requests and updates.
 */
public class SchedulerTest {
    private GenericQueue<Event> sharedFireQueue; // Queue for incoming fire events
    private GenericQueue<DroneResponse> responseQueue; // Queue for drone responses
    private ArrayList<Drone> drones;
    private Scheduler scheduler;
    private Drone drone;

    /**
     * Sets up the test environment before each test case.
     * Initializes a new instance of the Scheduler class.
     */
    @BeforeEach
    public void setUp() {
        sharedFireQueue = new GenericQueue<>();
        responseQueue = new GenericQueue<>();
        drones = new ArrayList<>();
        //scheduler = new Scheduler(sharedFireQueue, responseQueue, drones);
        //drone = new Drone(responseQueue);
        drones.add(drone);
    }

    /**
     * Tests the newFireRequest method of the Scheduler.
     * This test verifies that an event sent to the scheduler is correctly
     * retrieved from the scheduler's request queue.
     *
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    @Test
    public void testNewFireRequest() throws InterruptedException {
        Zone zone = new Zone(1, 0, 0, 700, 600);
        Event event = new Event(LocalTime.now(), zone, Event.Type.FIRE_DETECTED, Event.Severity.HIGH);

        // Start the scheduler thread
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.start();

        // Ensure event is processed by waiting a short time
        Thread.sleep(500);

        // Retrieve the event from the queue
        Event retrievedEvent = sharedFireQueue.get();
        assertNotNull(retrievedEvent, "Retrieved event should not be null");
        assertEquals(event.getId(), retrievedEvent.getId(), "Event ID should match");
    }

    /**
     * Tests the sendUpdate method of the Scheduler.
     * This test verifies that an update sent to the scheduler is correctly
     * retrieved from the scheduler's update queue.
     *
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    @Test
    public void testSendUpdate() throws InterruptedException {
        Zone zone = new Zone(1, 0, 0, 700, 600);
        Event event = new Event(LocalTime.now(), zone, Event.Type.DRONE_REQUEST, Event.Severity.HIGH);

        // Ensure update is processed by waiting a short time
        Thread.sleep(500);

        // Retrieve the event from the scheduler
        Event retrievedEvent = sharedFireQueue.get();
        assertNotNull(retrievedEvent, "Retrieved update event should not be null");
        assertEquals(event.getId(), retrievedEvent.getId(), "Update event ID should match");
    }
}
