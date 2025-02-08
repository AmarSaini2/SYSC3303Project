import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Unit tests for the FireIncident class.
 * This class tests the functionality of reading zone and event files,
 * as well as the interaction with the Scheduler and Drone classes.
 */
public class FireIncidentTest {

    FireIncident incident;
    Scheduler scheduler;
    Drone drone;


    /**
     * Sets up the test environment before each test case.
     * Initializes the Scheduler, FireIncident, and Drone instances,
     * and starts their respective threads.
     */
    @BeforeEach
    public void setup(){
        scheduler = new Scheduler();
        incident = new FireIncident("test\\test_Event_File.csv", "test\\test_Zone_File.csv", scheduler);
        drone = new Drone(scheduler);
        Thread schedulerThread = new Thread(scheduler);
        Thread droneThread = new Thread(drone);
        Thread incidentThread = new Thread(incident);
        schedulerThread.start();
        droneThread.start();
        incidentThread.start();
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
        incident.readZoneFile();

        //check that zone is processed correctly
        HashMap<Integer, Zone> zones = incident.getZones();

        
        assertEquals(zones.get(1).getId(), 1);
        assertArrayEquals(zones.get(1).getStart(), new int[]{0,0});
        assertArrayEquals(zones.get(1).getEnd(), new int[] {700,600});

        assertEquals(zones.get(2).getId(), 2);
        assertArrayEquals(zones.get(2).getStart(), new int[]{0,600});
        assertArrayEquals(zones.get(2).getEnd(), new int[] {650,1500});
    }

    /**
     * Tests the reading of the event file.
     * This test verifies that the events are processed correctly
     * and that the expected values are retrieved from the events list.
     *
     * @throws IOException if an I/O error occurs while reading the event file.
     */
    @Test
    public void testReadEventFile() throws IOException{//buggy because of the known scheduler deadlock issue. Will be fixed in Iteration 2
        //create test file and write some values to it
        File eventFile = new File("test\\test_Event_File.csv");
        try(FileWriter writer = new FileWriter(eventFile)){
            writer.write("Time\tZone ID\tEvent Type\tSeverity\n");
            writer.write("10:01:02\t1\tFIRE_DETECTED\thigh\n");
            writer.write("11:22:33\t2\tDRONE_REQUEST\tlow");
            writer.close();
        }

        //read event file
        incident.readZoneFile();
        incident.readEventFile();


        ArrayList<Event> events = incident.getEvents();

        assertEquals(1, events.get(0).getZone().getId());
        assertEquals(Event.Type.FIRE_DETECTED, events.get(0).getType());
        assertEquals(Event.Severity.OUT, events.get(0).getSeverity());
    }
}
