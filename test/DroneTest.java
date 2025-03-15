import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.LocalTime;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Drone class.
 * This class tests the functionality of sending fire requests
 * and receiving updates from the Scheduler.
 */
public class DroneTest {
    private static Drone drone;
    private static InetAddress droneAddr;
    private static int dronePort;

    /**
     * Sets up the test environment before each test case.
     * Initializes a new instance of the Scheduler and Drone classes.
     */
    @BeforeAll
    public static void setUp() {
        drone = new Drone(6000);
    }

//    @Test
//    public void testSendWakeupMessage(){
//        new Thread(() -> {
//            try {
//                DatagramSocket testSocket = new DatagramSocket(6000);
//                DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
//
//                testSocket.receive(receivePacket);
//                String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
//                assertTrue(receivedMessage.equals("ONLINE"));
//                System.out.println("Expected: ONLINE, Actual: " +receivedMessage);
//
//                String sendMessage = "RECEIVED";
//                DatagramPacket sendPacket = new DatagramPacket(sendMessage.getBytes(), sendMessage.getBytes().length, receivePacket.getAddress(), receivePacket.getPort());
//                testSocket.send(sendPacket);
//
//                this.droneAddr = receivePacket.getAddress();
//                this.dronePort = receivePacket.getPort();
//
//                testSocket.close();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }).start();
//
//        drone.sendWakeupMessage();
//    }

    /**
     * Tests the functionality of sending a fire request to the Scheduler
     * and verifying that the update is received after the fire is dealt with.
     *
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    @Test
    public void testSendToFire() throws InterruptedException {
        Zone zone = new Zone (1, 0,0,700,600);
        Event fireEvent = new Event(LocalTime.now(), zone, Event.Type.FIRE_DETECTED, Event.Severity.LOW);

        new Thread(() -> {
            try {
                DatagramSocket testSocket = new DatagramSocket(6000);

                DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
                testSocket.receive(receivePacket);
                this.droneAddr = receivePacket.getAddress();
                this.dronePort = receivePacket.getPort();

                assertTrue(drone.getStateAsString().equals("Idle"));
                System.out.println("State: Expected: Idle, Actual: " +drone.getStateAsString());

                byte[] buffer = fireEvent.serializeEvent();
                DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, this.droneAddr, this.dronePort);
                testSocket.send(sendPacket);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

        drone.start();
//
//        assertTrue(drone.getStateAsString().equals("Idle"));
//        System.out.println("State: Expected: Idle, Actual: " +drone.getStateAsString());
//        drone.checkMessage();
//        assertTrue(drone.getStateAsString().equals("En Route"));
//        System.out.println("State: Expected: En Route, Actual: " +drone.getStateAsString());

        while(true){

            System.out.println("State: Expected: Idle, Actual: " +drone.getStateAsString());
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
