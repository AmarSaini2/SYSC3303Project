import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.LocalTime;

import org.junit.jupiter.api.*;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Drone class.
 * This class tests the functionality of sending fire requests
 * and receiving updates from the Scheduler.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DroneTest {
    private static Drone workingDrone;
    private static Drone errorDrone;
    private InetAddress workingDroneAddr, errorDroneAddr;
    private int workingDronePort, errorDronePort;
    private DatagramSocket workingSocket, errorSocket;

    @BeforeAll
    public static void init(){
        workingDrone = new Drone(6500);
        errorDrone = new Drone(6600, "test/droneFaultInjection.csv");
    }

    @Test
    @Order(0)
    public void testInit(){
        Drone drone = new Drone(6500);

        workingDrone.start();
            try {
                DatagramSocket testSocket = new DatagramSocket(6500);
                workingSocket = testSocket;
                DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);

                testSocket.receive(receivePacket);
                String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                assertTrue(receivedMessage.equals("ONLINE:0"));
                System.out.println("\nExpected: ONLINE, Actual: " +receivedMessage+ "\n");

                InetAddress droneAddr = receivePacket.getAddress();
                int dronePort = receivePacket.getPort();

                workingDroneAddr = droneAddr;
                workingDronePort = dronePort;

                String sendMessage = "RECEIVED";
                DatagramPacket sendPacket = new DatagramPacket(sendMessage.getBytes(), sendMessage.getBytes().length, droneAddr, dronePort);
                testSocket.send(sendPacket);

                Zone zone = new Zone (1, 0,0,700,600);
                Event event = new Event(LocalTime.now(), zone, Event.Type.FIRE_DETECTED, Event.Severity.LOW);

                byte[] buffer = event.createMessage("NEW_EVENT:");
                DatagramPacket sendPacket2 = new DatagramPacket(buffer, buffer.length, droneAddr, dronePort);
                testSocket.send(sendPacket2);

                DatagramPacket receivePacket2 = new DatagramPacket(new byte[1024], 1024);

                testSocket.receive(receivePacket2);
                String droneResponse = new String(receivePacket2.getData(), 0, receivePacket2.getLength());
                assertEquals("LOCATION:0:7:6", droneResponse);


            } catch (IOException e) {
                throw new RuntimeException(e);
            }
    }

    @Test
    @Order(2)
    public void testNextState1(){
        try {
            DatagramSocket testSocket = workingSocket;
            DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);

            Zone zone = new Zone (1, 0,0,700,600);
            Event event = new Event(LocalTime.now(), zone, Event.Type.FIRE_DETECTED, Event.Severity.LOW);
            byte[] buffer = event.createMessage("DROP:15:");

            DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, workingDroneAddr, workingDronePort);
            testSocket.send(sendPacket);

            testSocket.receive(receivePacket);
            String droneResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());

            assertEquals(droneResponse, "LOCATION:0:14:12");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    @Order(3)
    public void testNextState2(){
        try {
            DatagramSocket testSocket = workingSocket;
            DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);

            Zone zone = new Zone (1, 0,0,700,600);
            Event event = new Event(LocalTime.now(), zone, Event.Type.FIRE_DETECTED, Event.Severity.LOW);
            byte[] buffer = event.createMessage("OK:");

            DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, workingDroneAddr, workingDronePort);
            testSocket.send(sendPacket);

            testSocket.receive(receivePacket);
            String droneResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());

            assertEquals(droneResponse, "LOCATION:0:21:18");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    @Order(3)
    public void testNextState3(){
        try {
            DatagramSocket testSocket = workingSocket;
            DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);

            Zone zone = new Zone (1, 0,0,700,600);
            Event event = new Event(LocalTime.now(), zone, Event.Type.FIRE_DETECTED, Event.Severity.LOW);
            byte[] buffer = event.createMessage("OK:");

            DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, workingDroneAddr, workingDronePort);
            testSocket.send(sendPacket);

            testSocket.receive(receivePacket);
            String droneResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());

            assertEquals(droneResponse, "LOCATION:0:28:24");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    @Order(3)
    public void testDroneError(){
        Drone drone = new Drone(6500, "test/droneFaultInjection.csv");

        errorDrone.start();
        try {
            DatagramSocket testSocket = new DatagramSocket(6600);
            DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);

            testSocket.receive(receivePacket);
            String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
            assertTrue(receivedMessage.equals("ONLINE:1"));
            System.out.println("\nExpected: ONLINE, Actual: " +receivedMessage+ "\n");

            InetAddress droneAddr = receivePacket.getAddress();
            int dronePort = receivePacket.getPort();

            String sendMessage = "RECEIVED";
            DatagramPacket sendPacket = new DatagramPacket(sendMessage.getBytes(), sendMessage.getBytes().length, droneAddr, dronePort);
            testSocket.send(sendPacket);

            Zone zone = new Zone (1, 0,0,700,600);
            Event event = new Event(LocalTime.now(), zone, Event.Type.FIRE_DETECTED, Event.Severity.LOW);

            byte[] buffer = event.createMessage("NEW_EVENT:");
            DatagramPacket sendPacket2 = new DatagramPacket(buffer, buffer.length, droneAddr, dronePort);
            testSocket.send(sendPacket2);
            DatagramPacket receivePacket2 = new DatagramPacket(new byte[1024], 1024);
            testSocket.receive(receivePacket2);

            buffer = event.createMessage("OK");
            sendPacket2 = new DatagramPacket(buffer, buffer.length, droneAddr, dronePort);
            testSocket.send(sendPacket2);

            receivePacket2 = new DatagramPacket(new byte[1024], 1024);

            testSocket.receive(receivePacket2);
            String droneResponse = new String(receivePacket2.getData(), 0, receivePacket2.getLength());
            assertEquals("Returning To Base:1", droneResponse); //assert that drone correctly returns back to base after encountering a fault

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
