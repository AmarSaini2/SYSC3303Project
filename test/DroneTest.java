import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Drone class.
 * This class tests the functionality of sending fire requests
 * and receiving updates from the Scheduler.
 */
public class DroneTest {
    GenericQueue<DroneResponse> droneResponseQueue;

    private Drone drone;

    /**
     * Sets up the test environment before each test case.
     * Initializes a new instance of the Scheduler and Drone classes.
     */
    @BeforeEach
    public void setUp() {
        droneResponseQueue = new GenericQueue<DroneResponse>();

        drone = new Drone(droneResponseQueue);
        drone.start();
    }

    /**
     * Tests the functionality of sending a fire request to the Scheduler
     * and verifying that the update is received after the fire is dealt with.
     *
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    @Test
    public void testSendToFire() throws InterruptedException {
        Zone zone = new Zone (1, 0,0,700,600);
       Event fireEvent = new Event(LocalTime.now(), zone, Event.Type.FIRE_DETECTED, Event.Severity.HIGH);

       //start drone thread
        drone.assignFire(fireEvent);
       //cannot wait for thread to finish because the drone thread is meant to run infinitely

        DroneResponse response = droneResponseQueue.get();

       //check if the scheduler recieved the update after the fire is dealt with
       Event newFireStatus = response.getEvent();

       //assertions to verify expected behaviour
       assertNotNull(newFireStatus);
       assertEquals(newFireStatus, fireEvent);
        assertEquals(Event.Severity.OUT, newFireStatus.getSeverity());
    }
}
