import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Unit tests for the FireIncident class.
 * This class tests the functionality of reading zone and event files,
 * as well as the interaction with the Scheduler and Drone classes.
 */
public class FireIncidentTest {
    FireIncident fireIncident;

    /**
     * Sets up the test environment before each test case.
     * Initializes the Scheduler, FireIncident, and Drone instances,
     * and starts their respective threads.
     */
    @BeforeEach
    public void setup(){
        fireIncident = new FireIncident("test\\test_Event_File.csv", "test\\test_Zone_File.csv", 5500);
    }

    /**
     * Tests the reading of the zone file.
     * This test verifies that the zones are processed correctly
     * and that the expected values are retrieved from the HashMap.
     *
     * @throws IOException if an I/O error occurs while reading the zone file.
     */
    @Test
    public void testReadZoneFile() throws IOException{
        //create test file and write some values to it
        File zoneFile = new File("test\\test_Zone_File.csv");
        try(FileWriter writer = new FileWriter(zoneFile)){
            writer.write("Zone ID\tZone Start\tZone End\n");
            writer.write("1\t(0;0)\t(700;600)\n");
            writer.write("2\t(0;600)\t(650;1500)\n");
            writer.close();
        }

        //read zone file
        fireIncident.readZoneFile();

        //check that zone is processed correctly
        HashMap<Integer, Zone> zones = fireIncident.getZones();

        System.out.println("Zone 1:");
        assertEquals(1, zones.get(1).getId());
        System.out.println("id: Expected: 1, Actual: "+ zones.get(1).getId());
        assertArrayEquals(new int[]{0,0}, zones.get(1).getStart());
        System.out.println("start: Expected: [0, 0], Actual: " +Arrays.toString(zones.get(1).getStart()));
        assertArrayEquals(new int[] {700,600}, zones.get(1).getEnd());
        System.out.println("end: Expected: [700, 700], Actual: " +Arrays.toString(zones.get(1).getEnd()));
        System.out.println();

        System.out.println("Zone 2:");
        assertEquals(2, zones.get(2).getId());
        System.out.println("id: Expected: 2, Actual: "+ zones.get(2).getId());
        assertArrayEquals(new int[]{0,600}, zones.get(2).getStart());
        System.out.println("start: Expected: [0, 600], Actual: " +Arrays.toString(zones.get(2).getStart()));
        assertArrayEquals(new int[] {650,1500}, zones.get(2).getEnd());
        System.out.println("end: Expected: [650, 1500], Actual: " +Arrays.toString(zones.get(2).getEnd()));
    }

    /**
     * Tests the reading of the event file.
     * This test verifies that the events are processed correctly
     * and that the expected values are retrieved from the events list.
     *
     * @throws IOException if an I/O error occurs while reading the event file.
     */
    @Test
    public void testReadEventFile() throws IOException{
        //create test file and write some values to it
        File eventFile = new File("test\\test_Event_File.csv");
        try(FileWriter writer = new FileWriter(eventFile)){
            writer.write("Time\tZone ID\tEvent Type\tSeverity\n");
            writer.write("10:01:02\t1\tFIRE_DETECTED\thigh\n");
            writer.write("11:22:33\t2\tDRONE_REQUEST\tlow");
        }

        //Create thread that will receive the events from the FireIncident
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket testSocket = new DatagramSocket(5500);
                    DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);

                    testSocket.receive(receivePacket);
                    testSocket.receive(receivePacket);
                    Event event1 = Event
                            .deserializeEvent(Arrays.copyOfRange(receivePacket.getData(), 10, receivePacket.getLength()));
                    System.out.println("Event 1:");
                    assertEquals(1, event1.getZone().getId());
                    System.out.println("zone: Expected: 1, Actual: " +event1.getZone().getId());
                    assertEquals(Event.Type.FIRE_DETECTED, event1.getType());
                    System.out.println("type: Expected: FIRE_DETECTED, Actual: " +event1.getType());
                    assertEquals(Event.Severity.HIGH, event1.getSeverity());
                    System.out.println("severity: Expected: HIGH, Actual: " +event1.getSeverity());

                    testSocket.receive(receivePacket);
                    Event event2 = Event
                            .deserializeEvent(Arrays.copyOfRange(receivePacket.getData(), 10, receivePacket.getLength()));
                    System.out.println("Event 2:");
                    assertEquals(2, event2.getZone().getId());
                    System.out.println("zone: Expected: 2, Actual: " +event2.getZone().getId());
                    assertEquals(Event.Type.DRONE_REQUEST, event2.getType());
                    System.out.println("type: Expected: DRONE_REQUEST, Actual: " +event2.getType());
                    assertEquals(Event.Severity.LOW, event2.getSeverity());
                    System.out.println("severity: Expected: LOW, Actual: " +event2.getSeverity());

                    testSocket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        //read event file
        fireIncident.readZoneFile();
        fireIncident.readEventFile();
    }
}