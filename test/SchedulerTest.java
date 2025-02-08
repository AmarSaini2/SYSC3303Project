import java.time.Duration;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Scheduler class.
 * This class tests the functionality of handling fire requests and updates.
 */
public class SchedulerTest {

    private Scheduler scheduler;


    /**
     * Sets up the test environment before each test case.
     * Initializes a new instance of the Scheduler class.
     */
    @BeforeEach
    public void setUp() {
        scheduler = new Scheduler();
        
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
        Zone zone = new Zone (1, 0,0,700,600);
        Event event = new Event(LocalTime.now(), zone, Event.Type.FIRE_DETECTED, Event.Severity.HIGH);

        // Create a producer thread to send the fire request
       Thread producer = new Thread(() -> scheduler.newFireRequest(event));
       producer.start();
       producer.join();//wait for producer thread to finish running

       //Retrieve the event from the scheduler
       Event retrievedEvent = scheduler.requestForFire();
       assertNotNull(retrievedEvent);
       assertEquals(event.getId(), retrievedEvent.getId());//check that the same event was sent and recieved 
    }

    /**
     * Tests the sendUpdate method of the Scheduler.
     * This test verifies that an update sent to the scheduler is correctly
     * retrieved from the scheduler's update queue.
     *
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    @Test
    public void testSendUpdate() throws InterruptedException{
        Zone zone = new Zone (1, 0,0,700,600);
        Event event = new Event(LocalTime.now(), zone, Event.Type.DRONE_REQUEST, Event.Severity.HIGH);

        //create and updater thread to send the update
        Thread updater = new Thread(() -> scheduler.sendUpdate(event));
        updater.start();
        updater.join(); //wait for thread to finish

        //Retrieve the event form the scheduler
        Event retrievedEvent = scheduler.receiveUpdates();
        assertNotNull(retrievedEvent);
        assertEquals(event.getId(), retrievedEvent.getId());//check that the event sent and the one recieved are identical
    }

}
