import com.sun.nio.sctp.SendFailedNotification;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DroneTest {

    private Scheduler scheduler;
    private Drone drone;

    @BeforeEach
    public void setUp() {
        scheduler = new Scheduler();
        drone = new Drone(scheduler);
    }

    @Test
    public void testSendToFire() throws InterruptedException {
        Zone zone = new Zone (1, 0,0,700,600);
       Event fireEvent = new Event(Duration.ofSeconds(1),zone,1, Event.Type.FIRE_DETECTED, Event.Severity.HIGH);
       scheduler.newFireRequest(fireEvent);

       Thread droneThread = new Thread(drone);
       droneThread.start();
       //cannot wait for thread to finish because the drone thread is meant to run infinitely

       //check if the scheduler recieved the update after the fire is dealt with
       Event newFireStatus = scheduler.receiveUpdates();

       //assertions to verify expected behaviour
       assertNotNull(newFireStatus);
       assertEquals(Event.Type.DRONE_REQUEST, newFireStatus.getType());
       assertEquals(Event.Severity.OUT, newFireStatus.getSeverity());
    }
}
