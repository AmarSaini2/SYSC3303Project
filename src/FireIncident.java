import java.lang.*;
import java.io.*;
import java.util.Scanner;

public class FireIncident implements Runnable {
    private final String filePath;
    private final Scheduler scheduler;

    public FireIncident(String filePath, Scheduler scheduler) {
        this.filePath = filePath;
        this.scheduler = scheduler;
    }

    public void run() {
        try (Scanner scanner = new Scanner(new File(filePath))) {
            String inputLine;
            while ((inputLine = scanner.nextLine()) != null) {
                String[] tokens = inputLine.split(" ");

                String timeStamp = tokens[0];
                int zone = Integer.parseInt(tokens[1]);
                String eventType = tokens[2];
                String severity = tokens[3];

                FireIncidentEvent incident = new FireIncidentEvent(timeStamp, zone, eventType, severity);
                //scheduler.newFireRequest(incident);
                System.out.println("FireIncidentSubsystem: Sent incident to scheduler -> " + incident.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        receiveUpdates();
    }

    private synchronized void receiveUpdates() {
        try {
            while (true) {
                wait();
//                String update = scheduler.getUpdate();
//                if (update != null) {
//                    System.out.println("FireIncidentSubsystem: Received update -> " + update);
//                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

class FireIncidentEvent {
    private final String timeStamp;
    private final int zoneID;
    private final String eventType;
    private final String severity;

    FireIncidentEvent(String timeStamp, int zoneID, String eventType, String severity) {
        this.timeStamp = timeStamp;
        this.zoneID = zoneID;
        this.eventType = eventType;
        this.severity = severity;
    }

    public String toString() {
        return "FireIncident [time: " + timeStamp + ", zone:" + zoneID + ", type: " + eventType + ", severity: " + severity + "]";
    }
}