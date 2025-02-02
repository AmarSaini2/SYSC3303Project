import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;

public class FireIncidentTest {

    FireIncident incident;
    Scheduler scheduler;
    Drone drone;

    @BeforeEach
    public void setup(){
        scheduler = new Scheduler();
        incident = new FireIncident("src\\test_Event_File.csv", "src\\test_Zone_File.csv", scheduler);
        drone = new Drone(scheduler);

        Thread schedulerThread = new Thread(scheduler);
        Thread droneThread = new Thread(drone);
        schedulerThread.start();
        droneThread.start();
    } 

    @Test
    public void testReadZoneFile() throws IOException{
        //create test file and write some values to it
        File zoneFile = new File("src\\test_Zone_File.csv");
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
        
        assertEquals(zones.get(0).getId(), 1);
        assertArrayEquals(zones.get(0).getStart(), new int[]{0,0});
        assertArrayEquals(zones.get(0).getEnd(), new int[] {700,600});
    }

    @Test
    public void testReadEventFile() throws IOException{
        //create test file and write some values to it
        File eventFile = new File("src\\test_Event_File.csv");
        try(FileWriter writer = new FileWriter(eventFile)){
            writer.write("Time\tZone ID\tEvent Type\tSeverity\n");
            writer.write("10:01:02\t1\tFIRE_DETECTED\thigh\n");
            //writer.write("11:22:33\t2\tDRONE_REQUEST\tlow");
            writer.close();
        }


        //read event file
        incident.readZoneFile();
        incident.readEventFile();

        
        ArrayList<Event> events = incident.getEvents();

        assertEquals(1, events.get(0).getZone());
        assertEquals(Event.Type.FIRE_DETECTED, events.get(0).getType());
        assertEquals(Event.Severity.OUT, events.get(0).getSeverity());
    }
}
