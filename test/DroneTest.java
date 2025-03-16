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

    @Test
    public void testRun(){
        Drone drone = new Drone(6500);

        drone.start();
            try {
                DatagramSocket testSocket = new DatagramSocket(6500);
                DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);

                testSocket.receive(receivePacket);
                String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                assertTrue(receivedMessage.equals("ONLINE"));
                System.out.println("\nExpected: ONLINE, Actual: " +receivedMessage+ "\n");

                InetAddress droneAddr = receivePacket.getAddress();
                int dronePort = receivePacket.getPort();

                String sendMessage = "RECEIVED";
                DatagramPacket sendPacket = new DatagramPacket(sendMessage.getBytes(), sendMessage.getBytes().length, droneAddr, dronePort);
                testSocket.send(sendPacket);

                Zone zone = new Zone (1, 0,0,700,600);
                Event event = new Event(LocalTime.now(), zone, Event.Type.FIRE_DETECTED, Event.Severity.LOW);

                byte[] buffer = event.serializeEvent();
                DatagramPacket sendPacket2 = new DatagramPacket(buffer, buffer.length, droneAddr, dronePort);
                testSocket.send(sendPacket2);

                DatagramPacket receivePacket2 = new DatagramPacket(new byte[1024], 1024);

                testSocket.receive(receivePacket2);
                DroneResponse droneResponse = DroneResponse.deserializeResponse(receivePacket2.getData());
                Event returnedEvent = droneResponse.getEvent();
                System.out.println("\nEvent Severity: Expected: OUT, Actual: " +returnedEvent.getSeverity()+ "\n");


            } catch (IOException e) {
                throw new RuntimeException(e);
            }


    }

}
