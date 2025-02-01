import java.lang.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class FireIncident extends Thread {
    private final String eventFilePath;
    private final String zoneFilePath;
    private final Scheduler scheduler;
    private ArrayList<Event> events;
    private ArrayList<Zone> zones;

    public FireIncident(String eventFilePath, String zoneFilePath, Scheduler scheduler) {
        this.eventFilePath = eventFilePath;
        this.zoneFilePath = zoneFilePath;

        this.scheduler = scheduler;
        this.events = new ArrayList<>();
        this.zones = new ArrayList<>();
    }

    //Adds a new zone from a file input
    //TODO probably need to consider what to do with headers
    public void readZoneFile(){
        try (Scanner scanner = new Scanner(new File(zoneFilePath))) {
            String inputLine;
            while ((inputLine = scanner.nextLine()) != null) {
                String[] tokens = inputLine.split(" ");

                int id = Integer.parseInt(tokens[0]);
                int start = Integer.parseInt(tokens[1]);
                int end = Integer.parseInt(tokens[2]);

                Zone zone = new Zone(id, start, end);
                this.zones.add(zone);
                System.out.println("New zone: id = " +id+ " start = " +start+ " end = " +end);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readEventFile() {
        try (Scanner scanner = new Scanner(new File(eventFilePath))) {
            String inputLine;
            while ((inputLine = scanner.nextLine()) != null) {
                String[] tokens = inputLine.split(" ");

                String timeStamp = tokens[0];
                int zone = Integer.parseInt(tokens[1]);
                String eventType = tokens[2];
                String fireSeverity = tokens[3];

                Event.Type type = null;
                Event.Severity severity = null;

                if(eventType.equals("FIRE_DETECTED") ){
                    type = Event.Type.FIRE_DETECTED;
                } else if (eventType.equals("DRONE_REQUEST")){
                    type = Event.Type.DRONE_REQUEST;
                }

                if(fireSeverity.equals("HIGH") ){
                    severity = Event.Severity.HIGH;
                } else if (fireSeverity.equals("MODERATE")){
                    severity = Event.Severity.MODERATE;
                } else if (fireSeverity.equals("LOW")){
                    severity = Event.Severity.LOW;
                }

                if(type != null && severity != null){
                    Event incident = new Event(Integer.parseInt(timeStamp), zone, this.events.size(), type, severity);
                    this.events.add(incident);

                    scheduler.newFireRequest(incident);
                    System.out.println("FireIncidentSubsystem: Sent incident to scheduler -> " + incident.toString());
                }else{
                    System.out.println("Type or severity of the event is incorrect");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        updateEvents(this.scheduler.receiveUpdates());
    }

    public void updateEvents(Event event) {
        this.events.set(event.getId(), event);
    }

    //TODO Testing purposes only! Will need to be removed later
    @Override
    public void run() {
        Zone zone = new Zone(1, 0, 400);
        Event event = new Event(24, 1, 1, Event.Type.FIRE_DETECTED, Event.Severity.HIGH);
        this.zones.add(zone);
        this.events.add(event);
        this.scheduler.newFireRequest(event);
        updateEvents(this.scheduler.receiveUpdates());

        Zone zone1 = new Zone(2, 0, 400);
        Event event1 = new Event(24, 2, 1, Event.Type.FIRE_DETECTED, Event.Severity.MODERATE);
        this.zones.add(zone1);
        this.events.add(event1);
        this.scheduler.newFireRequest(event1);
        updateEvents(this.scheduler.receiveUpdates());
    }
}