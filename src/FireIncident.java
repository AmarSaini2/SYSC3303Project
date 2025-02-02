import java.io.*;
import java.lang.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/***
 * The FireIncident class represents a fire incidents subsystem that reads event and zone data
 * from files and relays them to a scheduler to process.
 */
public class FireIncident extends Thread {
    private final String eventFilePath;
    private final String zoneFilePath;
    private final Scheduler scheduler;
    private ArrayList<Event> events;
    private HashMap<Integer, Zone> zones;

    /**
     * Constructs a FireIncident instance.
     *
     * @param eventFilePath Path to the event data file.
     * @param zoneFilePath Path to the zone data file.
     * @param scheduler The scheduler responsible for handling fire incidents.
     */
    public FireIncident(String eventFilePath, String zoneFilePath, Scheduler scheduler) {
        this.eventFilePath = eventFilePath;
        this.zoneFilePath = zoneFilePath;

        this.scheduler = scheduler;
        this.events = new ArrayList<>();
        this.zones = new HashMap<Integer, Zone>();
    }
  
    //Adds a new zone from a file input

    /**
     * Reads zone data from the inputted file and stores it in the list of zones.
     * Skips the header row in the file.
     */
    public void readZoneFile(){
        try (Scanner scanner = new Scanner(new File(zoneFilePath))) {
            scanner.nextLine(); // Skip header row
            String inputLine;
            while (scanner.hasNextLine()) {
                inputLine = scanner.nextLine();
                String[] tokens = inputLine.split("\t");

                int id = Integer.parseInt(tokens[0]); // Extract zone ID and convert to int

                // Extract and parse start coordinates
                String startCoord = tokens[1].substring(1, tokens[1].length()-1);
                String[] startCoords = startCoord.split(";");
                int startX = Integer.parseInt(startCoords[0]);
                int startY = Integer.parseInt(startCoords[1]);

                // Extract and parse end coordinates
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
            e.printStackTrace(); // Handle file reading errors
        }
    }

    /**
     * Reads event data from the specified file, processes it, and sends fire requests to the scheduler.
     * Also updates event status based on scheduler feedback.
     */
    public void readEventFile() {
        try (Scanner scanner = new Scanner(new File(eventFilePath))) {
            scanner.nextLine();// Skip header row
            String inputLine;
            while (scanner.hasNextLine()) { // Loop through all lines of the file
                inputLine = scanner.nextLine();
                String[] tokens = inputLine.split("\t"); // Split by tabs

                // Assign tokens to variables
                String timeStamp = tokens[0]; // Timestamp of the event
                int zone = Integer.parseInt(tokens[1]); // Zone ID
                String eventType = tokens[2]; // Type of event
                String fireSeverity = tokens[3]; // Severity level

                Event.Type type = null;
                Event.Severity severity = null;

                // Assign event type using enum
                if(eventType.equals("FIRE_DETECTED") ){
                    type = Event.Type.FIRE_DETECTED;
                } else if (eventType.equals("DRONE_REQUEST")){
                    type = Event.Type.DRONE_REQUEST;
                }

                // Assign Severity values using enum
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
                // Parse timestamp into Duration object
                String[] splitTimeStamp = timeStamp.split(":"); //split by colon
                int hours = Integer.parseInt(splitTimeStamp[0]);
                int min = Integer.parseInt(splitTimeStamp[1]);
                int sec = Integer.parseInt(splitTimeStamp[2]);

                Duration time = Duration.ofHours(hours)
                                           .plusMinutes(min)
                                           .plusSeconds(sec);

                // Only process valid events
                if(type != null && severity != null){
                    Event incident = new Event(time, zones.get(zone), type, severity);
                    this.events.add(incident);

                    scheduler.newFireRequest(incident);
                    updateEvents(this.scheduler.receiveUpdates());
                    //System.out.println("FireIncidentSubsystem: Sent incident to scheduler -> " + incident.toString());
                }else{
                    System.out.println("Type or severity of the event is incorrect");
                }
            }
            scheduler.finishEvents(); // Notify scheduler that all events are processed
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateEvents(this.scheduler.receiveUpdates()); // Update events with scheduler feedback
    }

    /**
     * Updates an existing event in the list based on received scheduler feedback.
     *
     * @param event The updated event information.
     */
    public void updateEvents(Event event) {
        int index = events.indexOf(event);

        //the code should not be done this way, in iteration 2 we should change the events arrayList to a hashmap of <id:object> similar to zones.
        if(index != -1){
            this.events.set(index, event);
        }else{
            System.out.println("the fireIncident could not find an event to update");
        }
    }

     /**
     * Retrieves the list of zones (for testing purposes).
     *
     * @return The list of zones.
     */
    public HashMap<Integer, Zone> getZones(){//for testing purposes
        return zones;
    }
    /**
     * Retrieves the list of events (for testing purposes).
     *
     * @return The list of events.
     */
    public ArrayList<Event> getEvents(){//for testing purposes
        return this.events;
    }

    /**
     * The entry point for the FireIncident thread.
     * Reads zone and event data sequentially.
     */
    @Override
    public void run() {
        this.readZoneFile(); // Load zones
        this.readEventFile(); // Load events and communicate with scheduler
    }
}