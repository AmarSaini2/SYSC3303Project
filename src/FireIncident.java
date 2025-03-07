import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Scanner;

/**
 * The FireIncident class represents the fire incident subsystem.
 * 
 * This class:
 * - Reads fire event data (fires and drone requests) from a file.
 * - Reads zone data (fire locations) from another file.
 * - Stores zones and fire events in HashMaps.
 * - Sends fire incidents to the scheduler via a shared queue.
 * 
 * The FireIncident class runs as a separate thread that continuously processes
 * fire reports.
 */
public class FireIncident extends Thread {
    private DatagramSocket sendSocket, receiveSocket;
    private InetAddress schedulerAddr;
    private final String eventFilePath; // Path to the fire event data file
    private final String zoneFilePath; // Path to the zone data file
    private final GenericQueue<Event> sharedFireQueue; // Shared queue to communicate with the scheduler
    private HashMap<Integer, Event> events; // Stores fire events (indexed by event ID)
    private HashMap<Integer, Zone> zones; // Stores zone data (indexed by zone ID)

    /**
     * Constructs a FireIncident instance.
     *
     * @param eventFilePath   Path to the event data file (contains fire incidents).
     * @param zoneFilePath    Path to the zone data file (contains fire locations).
     * @param sharedFireQueue Queue to send fire events to the scheduler.
     */
    public FireIncident(String eventFilePath, String zoneFilePath, GenericQueue<Event> sharedFireQueue) {
        this.eventFilePath = eventFilePath;
        this.zoneFilePath = zoneFilePath;
        this.sharedFireQueue = sharedFireQueue;

        // Initialize HashMaps to store fire events and fire zones
        this.events = new HashMap<>();
        this.zones = new HashMap<>();
    }

    /**
     * Reads zone data from the zone file and stores it in the zones HashMap.
     * 
     * - The **zone file** defines the layout of fire zones.
     * - Each **zone** has an ID, a start coordinate, and an end coordinate.
     * - This method **parses** the file and stores zones in a HashMap.
     * - The first line (header) is **skipped**.
     */
    public void readZoneFile() {
        try (Scanner scanner = new Scanner(new File(zoneFilePath))) {
            scanner.nextLine(); // Skip header row
            String inputLine;
            while (scanner.hasNextLine()) {
                inputLine = scanner.nextLine();
                String[] tokens = inputLine.split("\t"); // Split each line into parts

                int id = Integer.parseInt(tokens[0]); // Extract zone ID

                // Parse the start coordinates (removing parentheses and splitting by ';')
                String startCoord = tokens[1].substring(1, tokens[1].length() - 1);
                String[] startCoords = startCoord.split(";");
                int startX = Integer.parseInt(startCoords[0]);
                int startY = Integer.parseInt(startCoords[1]);

                // Parse the end coordinates (removing parentheses and splitting by ';')
                String endCoord = tokens[2].substring(1, tokens[2].length() - 1);
                String[] endCoords = endCoord.split(";");
                int endX = Integer.parseInt(endCoords[0]);
                int endY = Integer.parseInt(endCoords[1]);

                // Create a Zone object and add it to the HashMap
                Zone zone = new Zone(id, startX, startY, endX, endY);
                this.zones.put(zone.getId(), zone);
            }
        } catch (IOException e) {
            e.printStackTrace(); // Handle file reading errors
        }
    }

    /**
     * Reads event data from the specified file and processes fire requests.
     * 
     * - The **event file** contains **fire incidents** and **drone requests**.
     * - Each event has a **timestamp**, a **zone ID**, an **event type**, and a
     * **severity level**.
     * - The method reads each event, **creates an Event object**, and **sends it to
     * the scheduler**.
     */
    public void readEventFile() {
        try (Scanner scanner = new Scanner(new File(eventFilePath))) {
            scanner.nextLine(); // Skip header row
            String inputLine;
            while (scanner.hasNextLine()) { // Process each event in the file
                inputLine = scanner.nextLine();
                String[] tokens = inputLine.split("\t"); // Split each line into components

                // Extract event details
                String timeStamp = tokens[0]; // Timestamp of the event
                int zone = Integer.parseInt(tokens[1]); // Zone ID where the fire is happening
                String eventType = tokens[2]; // Event type (e.g., FIRE_DETECTED, DRONE_REQUEST)
                String fireSeverity = tokens[3]; // Severity of fire (High, Moderate, Low)

                Event.Type type = null;
                Event.Severity severity = null;

                // Convert event type from string to enum
                if (eventType.equals("FIRE_DETECTED")) {
                    type = Event.Type.FIRE_DETECTED;
                } else if (eventType.equals("DRONE_REQUEST")) {
                    type = Event.Type.DRONE_REQUEST;
                }

                // Convert fire severity from string to enum
                switch (fireSeverity.toLowerCase()) {
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

                // Process only valid events
                if (type != null && severity != null) {
                    Event event = new Event(LocalTime.now(), zones.get(zone), type, severity);
                    this.events.put(event.getId(), event);

                    sendToScheduler(event);
                } else {
                    System.out.println("Type or severity of the event is incorrect");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendToScheduler(Event e){
        byte[] serializedEvent = e.serializeEvent();
        try{
            DatagramPacket packet = new DatagramPacket(serializedEvent, serializedEvent.length, InetAddress.getByName("127.0.0.1"), 5000);
            sendSocket.send(packet);
            System.out.println("[FireIncidentSubsystem]: Sent Packet to Scheduler containing: " + Event.deserializeEvent(serializedEvent).toString());
        }catch(UnknownHostException f){
            f.printStackTrace();
        }catch(IOException g){
            g.printStackTrace();
        }
    }

    /**
     * Updates an event's status when receiving feedback from the scheduler.
     * 
     * @param event The updated event information (e.g., fire extinguished).
     */
    public void updateEvents(Event event) {
        this.events.put(event.getId(), event);
    }

    /**
     * Retrieves the list of zones.
     * 
     * @return A HashMap of all fire zones.
     */
    public HashMap<Integer, Zone> getZones() {
        return zones;
    }

    /**
     * Retrieves the list of events.
     * 
     * @return A HashMap of all fire events.
     */
    public HashMap<Integer, Event> getEvents() {
        return this.events;
    }

    /**
     * The **entry point** for the FireIncident subsystem thread.
     * 
     * - First, it reads **zone data** (fire locations).
     * - Then, it reads **fire event data** and sends them to the scheduler.
     * - This method **runs automatically** when the thread starts.
     */
    @Override
    public void run() {
        try{
            this.sendSocket = new DatagramSocket();
        }catch(IOException e){
            e.printStackTrace();
        }
        
        this.readZoneFile(); // Load zones from the file
        this.readEventFile(); // Load fire incidents and send them to the scheduler

        this.sendSocket.close();
    }
}
