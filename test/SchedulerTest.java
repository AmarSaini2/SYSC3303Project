import java.io.IOException;
import java.net.*;
import java.time.LocalTime;
import java.util.Arrays;

import org.junit.After;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Scheduler class.
 * This class tests the functionality of handling fire requests and updates.
 */
public class SchedulerTest {
    Scheduler scheduler;

    /**
     * Sets up the test environment before each test case.
     * Initializes a new instance of the Scheduler class.
     */
    @BeforeEach
    public void setUp() {
        scheduler = new Scheduler(5000, 6000);
    }

    /**
     * Tests the newFireRequest method of the Scheduler.
     * This test verifies that an event sent to the scheduler is correctly
     * retrieved from the scheduler's request queue.
     *
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    @Test
    public void testRun() throws InterruptedException {
        Zone zone = new Zone (1, 0,0,700,600);
        Event event = new Event(LocalTime.now(), zone, Event.Type.FIRE_DETECTED, Event.Severity.HIGH);
        Drone drone = new Drone(6000);

        new Thread(()->{
            try {
                DatagramSocket testSocket = new DatagramSocket(7000);

                byte[] buffer = event.createMessage("NEW_EVENT:");
                DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getLocalHost(), 5000);
                testSocket.send(sendPacket);

                DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                testSocket.receive(receivePacket);

                String returnedResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println(returnedResponse);

                buffer ="FINISH".getBytes();
                sendPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getLocalHost(), 5000);
                testSocket.send(sendPacket);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

        new Thread(()->scheduler.activeAction()).start();

        drone.sendWakeupMessage();
        DatagramSocket socket = drone.getSocket();
        DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
        try {
            socket.receive(receivePacket);
            Event returnedEvent = Event
                    .deserializeEvent(Arrays.copyOfRange(receivePacket.getData(), 10, receivePacket.getLength()));
            System.out.println("\nCheck if Scheduler passed the right event to Drone: ");
            assertEquals(event.getId(), returnedEvent.getId());
            System.out.println("id: Expected: "+event.getId()+", Actual: " +returnedEvent.getId());
            assertEquals(event.getZone().getId(), returnedEvent.getZone().getId());;
            System.out.println("zone id: Expected: "+event.getZone().getId()+", Actual: " +returnedEvent.getZone().getId());
            assertEquals(event.getType(), returnedEvent.getType());;
            System.out.println("type: Expected: "+event.getType()+", Actual: " +returnedEvent.getType());
            assertEquals(event.getSeverity(), returnedEvent.getSeverity());;
            System.out.println("severity: Expected: "+event.getSeverity()+", Actual: " +returnedEvent.getSeverity());
            System.out.println();

            byte[] sendMessage = "En Route:0:0:15.00".getBytes();
            socket.send(new DatagramPacket(sendMessage, sendMessage.length, InetAddress.getLocalHost(), 6000));

            socket.receive(receivePacket);
            String receiveMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());

            System.out.println("\nCheck if Scheduler passed the right event to Drone: ");
            assertEquals(receiveMessage, "DROP:15.00");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //Lets threads finish
        sleep(1000);
    }
}