//deprecated in favour of alternate system for logging, keeping code here as proof of work for kaitlyn - sam

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LogAnalyzer {
    private static final String LOG_FILE = "src/logs.txt";
    private List<LogEntry> logEntries = new ArrayList<>();
    private static Map<String, Long> idleTime;
    private static Map<String, Long> activeTime;
    private static Map<String, Long> startTime;


    // Inner class to represent a single log entry
    public class LogEntry {
        public Long timestamp;
        public String rawLine;
        public Integer droneId;


        public LogEntry(String line) {
            String[] parts = line.split(",", 3); // Split into 3 parts only
            if (parts.length < 3) {
                throw new IllegalArgumentException("Invalid log line format: " + line);
            }

            String timeStr = parts[0].trim();
            String actor = parts[1].replace("[", "").replace("]", "");
            String rest = parts[2].trim();

            this.timestamp = parseTime(LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));
            this.rawLine = rest;

            // Extract droneId from the actor field
            if (actor.startsWith("Drone")) {
                String[] id = actor.split(" ");
                this.droneId = Integer.parseInt(id[1]);  // Extract the drone ID from the actor string
            } else {
                this.droneId = -1; // If not a drone, assign an invalid ID
            }
        }
    }


    /**
     * Parses a timestamp from a log entry and returns it as a long value.
     *
     * @return The parsed time in milliseconds.
     */
    private static long parseTime(LocalTime time) {
        String timeStr = time.toString();
        // Check if the time string has milliseconds
        if (timeStr.length() == 8) { // HH:mm:ss (without milliseconds)
            timeStr += ".000"; // Add default milliseconds
        }

        try {
            // Parse the time string with milliseconds
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
            Date date = sdf.parse(timeStr);
            return date.getTime(); // Return time in milliseconds
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return 0; // If parsing fails
    }

    // Method to parse the log file and populate the logEntries list
    public void parseLogFile() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader("src/logs.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                logEntries.add(new LogEntry(line));
            }
        }
    }

    // Inner class to represent a drone report
    public class DroneReport {
        int droneId;
        int firesExtinguished = 0;
        long timeInIdle = 0;
        long timeInEnroute = 0;
        long timeInDroppingAgent = 0;
        long timeInReturningToBase = 0;
        long timeInFillingTank = 0;
        long timeInFault = 0;
        public Long assignedTime;
        public Long extinguishedTime;

        // Add time spent in a specific state (e.g., IDLE, ENROUTE)
        public void addTimeInState(String state, long timeSpent) {
            switch (state) {
                case "IDLE": timeInIdle += timeSpent; break;
                case "ENROUTE": timeInEnroute += timeSpent; break;
                case "DROPPING_AGENT": timeInDroppingAgent += timeSpent; break;
                case "RETURNING_TO_BASE": timeInReturningToBase += timeSpent; break;
                case "FILLING_TANK": timeInFillingTank += timeSpent; break;
                case "FAULT": timeInFault += timeSpent; break;
            }
        }

        // Increment the number of fires extinguished
        public void incrementFiresExtinguished() {
            firesExtinguished++;
        }

        @Override
        public String toString() {
            return "Drone " + droneId + ":\n" +
                    "  Fires extinguished: " + firesExtinguished + "\n" +
                    "  Response time: " + getResponseTime() + " ms\n" +
                    "  Time in idle: " + timeInIdle + " ms\n" +
                    "  Time in fault: " + timeInFault + " ms\n";
        }

        // Calculate the response time between assigned time and extinguished time
        public long getResponseTime() {
            if (assignedTime != null && extinguishedTime != null) {
                return extinguishedTime - assignedTime; // Direct subtraction of the timestamp values
            }
            return 0; // Return 0 if either assignedTime or extinguishedTime is null
        }
    }

    // Main code that reads the log and processes each entry
    public void generateDroneReports() {
        Map<Integer, DroneReport> droneStats = new HashMap<>();
        Map<Integer, Long> stateStartTime = new HashMap<>();

        // Variables for overall performance metrics
        long totalFiresExtinguished = 0;
        long totalFireExtinguishingTime = 0;
        long totalActiveTime = 0;
        long totalTimeSpent = 0; // Total time for all drones

        // Process each log entry
        for (LogEntry entry : logEntries) {
            int droneId = entry.droneId;
            String line = entry.rawLine;

            // If the droneId is not in the reports map, initialize a new DroneReport for the drone
            if (!droneStats.containsKey(droneId)) {
                droneStats.put(droneId, new DroneReport());
                droneStats.get(droneId).droneId = droneId;
            }

            DroneReport report = droneStats.get(droneId);

            // Handle state transitions based on log message
            if (line.contains("Sent: En Route") || line.contains("Received: En Route")) {
                if (report.assignedTime == null) {
                    report.assignedTime = entry.timestamp;
                }
                report.addTimeInState("ENROUTE", entry.timestamp - report.assignedTime);
            } else if (line.contains("Sent: Idle") || line.contains("Received: Idle")) {
                report.addTimeInState("IDLE", entry.timestamp - report.assignedTime);
            } else if (line.contains("Sent: Dropping Agent") || line.contains("Received: Dropping Agent")) {
                report.addTimeInState("DROPPING_AGENT", entry.timestamp - report.assignedTime);
            } else if (line.contains("Sent: Returning to Base") || line.contains("Received: Returning to Base")) {
                report.addTimeInState("RETURNING_TO_BASE", entry.timestamp - report.assignedTime);
            } else if (line.contains("Sent: Filling Tank") || line.contains("Received: Filling Tank")) {
                report.addTimeInState("FILLING_TANK", entry.timestamp - report.assignedTime);
            } else if (line.contains("Sent: Fault") || line.contains("Received: Fault")) {
                report.addTimeInState("FAULT", entry.timestamp - report.assignedTime);
            }

            // Handle fire extinguishing events
            if (line.contains("extinguished fire") && line.contains("Drone")) {
                Matcher m = Pattern.compile("Drone (\\d+) successfully extinguished fire: (\\d+)").matcher(line);
                if (m.find()) {
                    int fireId = Integer.parseInt(m.group(2));
                    int droneIdFromLog = Integer.parseInt(m.group(1));

                    if (droneIdFromLog != -1) {
                        DroneReport droneReport = droneStats.get(droneIdFromLog);
                        if (droneReport != null) {
                            // Mark the extinguishing time and increment fires extinguished
                            droneReport.extinguishedTime = entry.timestamp;
                            droneReport.incrementFiresExtinguished();

                            // Update overall performance metrics
                            totalFireExtinguishingTime += droneReport.getResponseTime();
                            totalFiresExtinguished++;
                        }
                    }
                }
            }
            // Update total time for each drone (either active or idle)
            totalTimeSpent = Math.max(totalTimeSpent, entry.timestamp);
        }

        // Print the final report for each drone
        for (DroneReport report : droneStats.values()) {
            if (report.droneId != -1) {
                // Calculate Drone Utilization Rate: (active time / total time) * 100
                long activeTime = report.timeInEnroute + report.timeInDroppingAgent + report.timeInReturningToBase +
                        report.timeInFillingTank + report.timeInFault;
                long totalDroneTime = activeTime + report.timeInIdle;

                double utilizationRate = (totalDroneTime > 0) ? ((double) activeTime / totalDroneTime) * 100 : 0;

                totalActiveTime += activeTime; // Update total active time for all drones

                long responseTime = report.getResponseTime();  // Calculate response time based on assigned and extinguished time
                System.out.println("\nDrone " + report.droneId + ":");
                System.out.println("  Fires extinguished: " + report.firesExtinguished);
                System.out.println("  Drone Utilization Rate: " + utilizationRate + " %");
                System.out.println("  Response time: " + responseTime + " ms");
                System.out.println("  Time in idle: " + report.timeInIdle + " ms");
                System.out.println("  Time in enroute: " + report.timeInEnroute + " ms");
                System.out.println("  Time in fault: " + report.timeInFault + " ms");
            }
        }
    }

    // Main method to drive the analysis process
    public static void analyzeLogs(String filePath) throws IOException {
        LogAnalyzer analyzer = new LogAnalyzer();
        analyzer.parseLogFile();
        analyzer.generateDroneReports();
    }
}
