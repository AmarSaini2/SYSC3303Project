import java.time.Duration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SchedulerTest {

    private Scheduler scheduler;

    @BeforeEach
    public void setUp() {
        scheduler = new Scheduler();
        
    }

    @Test
    public void testnewFireRequest() throws InterruptedException {
       Event event = new Event(Duration.ofSeconds(1),1,1, Event.Type.FIRE_DETECTED, Event.Severity.HIGH);
       Thread producer = new Thread(() -> scheduler.newFireRequest(event));
       producer.start();
       producer.join();//wait for producer thread to finish running

       Event retrievedEvent = scheduler.requestForFire();
       assertNotNull(retrievedEvent);
       assertEquals(event.getId(), retrievedEvent.getId());//check that the same event was sent and recieved 
    }

    @Test
    public void testSendUpdate() throws InterruptedException{
        Event event = new Event(Duration.ofSeconds(1),1,1,Event.Type.DRONE_REQUEST, Event.Severity.HIGH);
        Thread updater = new Thread(() -> scheduler.sendUpdate(event));
        updater.start();
        updater.join(); //wait for thread to finish

        Event retrievedEvent = scheduler.receiveUpdates();
        assertNotNull(retrievedEvent);
        assertEquals(event.getId(), retrievedEvent.getId());//check that the event sent and the one recieved are identical
    }

}
