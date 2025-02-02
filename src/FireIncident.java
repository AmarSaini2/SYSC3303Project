import java.io.*;
import java.lang.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class FireIncident extends Thread {
    private final String eventFilePath;
    private final String zoneFilePath;
    private final Scheduler scheduler;
    private ArrayList<Event> events;
    private HashMap<Integer, Zone> zones;

    public FireIncident(String eventFilePath, String zoneFilePath, Scheduler scheduler) {
        this.eventFilePath = eventFilePath;
        this.zoneFilePath = zoneFilePath;

        this.scheduler = scheduler;
        this.events = new ArrayList<>();
        this.zones = new HashMap<Integer, Zone>();
    }

    //Adds a new zone from a file input
    public void readZoneFile(){
        try (Scanner scanner = new Scanner(new File(zoneFilePath))) {
            scanner.nextLine();//skip header row
            String inputLine;
            while (scanner.hasNextLine()) {
                inputLine = scanner.nextLine();
                String[] tokens = inputLine.split("\t");
                //convert the id from string to int
                int id = Integer.parseInt(tokens[0]);
                //split start and end coords into seperate integer values
                String startCoord = tokens[1].substring(1, tokens[1].length()-1);
                String[] startCoords = startCoord.split(";");
                int startX = Integer.parseInt(startCoords[0]);
                int startY = Integer.parseInt(startCoords[1]);

                String endCoord = tokens[2].substring(1, tokens[2].length()-1);
                String[] endCoords  = endCoord.split(";");
                int endX = Integer.parseInt(endCoords[0]);
                int endY = Integer.parseInt(endCoords[1]);

                //add zone into arraylist of zones and print confirmation to console
                Zone zone = new Zone(id, startX, startY, endX, endY);
                this.zones.put(zone.getId(), zone);
                //System.out.println("New zone: id = " +zone.getId()+ " start = " +Arrays.toString(zone.getStart())+ " end = "+Arrays.toString(zone.getEnd()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readEventFile() {
        try (Scanner scanner = new Scanner(new File(eventFilePath))) {
            scanner.nextLine();//skip header row
            String inputLine;
            while (scanner.hasNextLine()) {//loop through all lines of the file
                inputLine = scanner.nextLine();
                String[] tokens = inputLine.split("\t");//split by tabs

                //assign tokens to variables
                String timeStamp = tokens[0];
                int zone = Integer.parseInt(tokens[1]);
                String eventType = tokens[2];
                String fireSeverity = tokens[3];

                Event.Type type = null;
                Event.Severity severity = null;

                //assign eventType values using enum
                if(eventType.equals("FIRE_DETECTED") ){
                    type = Event.Type.FIRE_DETECTED;
                } else if (eventType.equals("DRONE_REQUEST")){
                    type = Event.Type.DRONE_REQUEST;
                }

                //assign Severity values using enum
                switch(fireSeverity.toLowerCase()){
                    case "high":
                    severity = Event.Severity.HIGH;
                    break;

                    case "moderate":
                    severity = Event.Severity.MODERATE;
                    break;

                    case "low":
                    severity = Event.Severity.LOW;
                    break;

                    default:
                    System.out.println("Unknown severity: " + fireSeverity);
                    severity = null;
                    break;
                }

                //System.out.println("Time: " + timeStamp + " Zone: " + zone + " Type: " + type + " Severity: " + severity);
                //parse time string into duration value
                String[] splitTimeStamp = timeStamp.split(":"); //split by colon
                int hours = Integer.parseInt(splitTimeStamp[0]);
                int min = Integer.parseInt(splitTimeStamp[1]);
                int sec = Integer.parseInt(splitTimeStamp[2]);

                Duration time = Duration.ofHours(hours)
                                           .plusMinutes(min)
                                           .plusSeconds(sec);

                if(type != null && severity != null){
                    Event incident = new Event(time, zones.get(zone), this.events.size(), type, severity);
                    this.events.add(incident);

                    scheduler.newFireRequest(incident);
                    updateEvents(this.scheduler.receiveUpdates());
                    //System.out.println("FireIncidentSubsystem: Sent incident to scheduler -> " + incident.toString());
                }else{
                    System.out.println("Type or severity of the event is incorrect");
                }
            }
            scheduler.finishEvents();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void updateEvents(Event event) {
        this.events.set(event.getId(), event);
    }

    public HashMap<Integer, Zone> getZones(){//for testing purposes
        return this.zones;
    }

    public ArrayList<Event> getEvents(){//for testing purposes
        return this.events;
    }

    @Override
    public void run() {
        this.readZoneFile();
        this.readEventFile();
    }
}