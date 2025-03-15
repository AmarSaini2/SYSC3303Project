//import java.io.IOException;
//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//import java.time.LocalTime;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
///**
// * Unit tests for the Drone class.
// * This class tests the functionality of sending fire requests
// * and receiving updates from the Scheduler.
// */
//public class DroneTest {
//    private static Drone drone;
//
//    /**
//     * Sets up the test environment before each test case.
//     * Initializes a new instance of the Scheduler and Drone classes.
//     */
//    @BeforeAll
//    public static void setUp() {
//        drone = new Drone(6000);
//    }
//
//    @Test
//    public void testWakeUp(){
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    DatagramSocket testSocket = new DatagramSocket(6000);
//                    DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
//
//                    testSocket.receive(receivePacket);
//                    String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength())
//
//                    testSocket.close();
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }).start();
//
//        drone.sendWakeupMessage();
//    }
//
//    /**
//     * Tests the functionality of sending a fire request to the Scheduler
//     * and verifying that the update is received after the fire is dealt with.
//     *
//     * @throws InterruptedException if the thread is interrupted while waiting.
//     */
//    @Test
//    public void testSendToFire() throws InterruptedException {
//        Zone zone = new Zone (1, 0,0,700,600);
//        Event fireEvent = new Event(LocalTime.now(), zone, Event.Type.FIRE_DETECTED, Event.Severity.HIGH);
//
//        //start drone thread
//        drone.assignFire(fireEvent);
//        //cannot wait for thread to finish because the drone thread is meant to run infinitely
//
//        //check if the scheduler recieved the update after the fire is dealt with
//        Event newFireStatus = response.getEvent();
//
//        //assertions to verify expected behaviour
//        assertNotNull(newFireStatus);
//        assertEquals(newFireStatus, fireEvent);
//        assertEquals(Event.Severity.OUT, newFireStatus.getSeverity());
//    }
//}
