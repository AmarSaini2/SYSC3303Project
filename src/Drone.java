import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Random;

public class Drone extends Thread {
    private static int idCounter = 0;
    private int id;
    private Event assignedFire;
    private final HashMap<String, Double> attributes;
    private double carryingVolume;
    private final Random random;
    private DroneState currentState;
    private DroneFSM droneFSM;
    private DatagramSocket socket;
    private InetAddress schedulerAddr;
    private int schedulerPort;
    private boolean finish;

    public Drone(int schedulerPort) {
        this.random = new Random();
        this.attributes = new HashMap<>();

        // Setting up drone attributes
        attributes.put("takeoffSpeed", 3.0);
        attributes.put("travelSpeed", 12.0);
        attributes.put("flowRate", 1.25);
        attributes.put("maxCapacity", 15.0);

        this.id = idCounter++;
        this.carryingVolume = attributes.get("maxCapacity");
        this.assignedFire = null;
        this.droneFSM = new DroneFSM();
        this.droneFSM.initialize(this);
        this.currentState = DroneFSM.getState("Idle");
        try {
            this.socket = new DatagramSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.schedulerPort = schedulerPort;

        this.finish = false;

    }

    public void setState(String stateName) {
        this.currentState = DroneFSM.getState(stateName);
    }

    public String getStateAsString() {
        return this.currentState.getStateString();
    }

    /**
     * Assigns a fire event to the drone if it's idle.
     */
    public synchronized boolean assignFire(Event fire) {
        if (this.assignedFire == null) {
            this.assignedFire = fire;
            currentState = DroneFSM.getState("EnRoute");
            System.out.println("[Drone " + this.id + "] Fire assigned: " + fire);
            notifyAll(); // Wake up the thread
            return true;
        }
        return false;
    }

    protected void sendWakeupMessage() {
        byte[] data = "ONLINE".getBytes();
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"),
                    this.schedulerPort);
            socket.setBroadcast(true);
            socket.send(packet);
            System.out.println("[Drone " + this.id + "] Wakeup message sent");
            byte[] incomingData = new byte[512];
            DatagramPacket response = new DatagramPacket(data, data.length);
            socket.receive(response);
            System.out.println("[Drone " + this.id + "] Received confirmation from Scheduler");
            schedulerAddr = response.getAddress();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void checkMessage() {
        byte[] data = new byte[1024];
        DatagramPacket packet = new DatagramPacket(data, data.length);
        try {
            socket.setSoTimeout(300);
            socket.receive(packet);
            String receiveMessage = new String(packet.getData(), 0, packet.getLength());
            if (receiveMessage.equals("FINISH")) {
                System.out.println("[Drone " + this.id + "]: Received FINISH");
                this.finish = true;
                return;
            }
            if (Event.deserializeEvent(packet.getData()) == null) {
                System.out.println("[DRONE]: Received: " + new String(packet.getData(), 0, packet.getLength()));
            }
            if (this.isFree()) {
                this.assignFire(Event.deserializeEvent(packet.getData()));// assign Fire to drone
                System.out.println("[Drone " + this.id + "] Received assignment of new event: " + this.assignedFire);
            }
            // System.out.println("CHECK: " +new String(packet.getData(),0,
            // packet.getLength()));

        } catch (SocketTimeoutException e) {
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        sendWakeupMessage();
        while (!finish) {
            checkMessage();

            if (finish) {
                return;
            }

            // Execute the function for the current state
            // Move to next state
            currentState.action(this);
            currentState.goNextState(this);
        }
    }

    public boolean isFree() {
        return "Idle".equals(currentState.getStateString()) && assignedFire == null;
    }

    /**
     * Determines the amount of water required to extinguish a fire.
     */
    private int getRequiredVolume(Event fire) {
        switch (fire.getSeverity()) {
            case HIGH:
                return 15;
            case MODERATE:
                return 10;
            case LOW:
                return 5;
            default:
                return 0;
        }
    }

    /**
     * Calculates estimated travel time to the fire zone.
     */
    private int getTravelTime(Event fire) {
        Zone zone = fire.getZone();
        double distance = Math.sqrt(zone.getStart()[0] * zone.getStart()[0] +
                zone.getEnd()[1] * zone.getEnd()[1]);
        return (int) (distance / attributes.get("travelSpeed"));
    }

    private int getExtinguishTime(int requiredVolume) {
        return (int) (requiredVolume / attributes.get("flowRate"));
    }

    // ========== STATE HANDLING FUNCTIONS ==========

    public void sleepMode() {
        System.out.println("[Drone " + id + "] IDLE - Waiting for assignment...");
        byte[] data = new byte[1024];
        DatagramPacket packet = new DatagramPacket(data, data.length);
        try {
            socket.setSoTimeout(0);
            socket.receive(packet);
            String receiveMessage = new String(packet.getData(), 0, packet.getLength());
            if (receiveMessage.equals("FINISH")) {
                System.out.println("[Drone " + this.id + "]: Received FINISH");
                this.finish = true;
                return;
            }
            if (Event.deserializeEvent(packet.getData()) == null) {
                System.out.println("[DRONE]: Received: " + new String(packet.getData(), 0, packet.getLength()));
            }
            if (this.isFree()) {
                this.assignFire(Event.deserializeEvent(packet.getData()));// assign Fire to drone
                System.out.println("[Drone " + this.id + "] Received assignment of new event: " + this.assignedFire);
            }
            // System.out.println("CHECK: " +new String(packet.getData(),0,
            // packet.getLength()));

        } catch (SocketTimeoutException e) {
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // public void travelToFire() {
    // System.out.println("[Drone " + id + "] Traveling to fire at Zone: " +
    // assignedFire.getZone().getId());

    // try {
    // Thread.sleep(getTravelTime(assignedFire) /* *1000 */);
    // } catch (InterruptedException e) {
    // e.printStackTrace();
    // }

    // }

    public void travelToFire() {
        System.out.println("[Drone " + id + "] Traveling to fire at Zone: " + assignedFire.getZone().getId());

        // Get the target coordinates
        Zone zone = assignedFire.getZone();
        double targetX = zone.getStart()[0];
        double targetY = zone.getEnd()[1];

        // Calculate the travel time
        int travelTimeSeconds = getTravelTime(assignedFire);

        // If the travel time is 0, the drone is already at the fire location
        if (travelTimeSeconds <= 0) {
            System.out.println("[Drone " + id + "] Already at the fire location.");
            return;
        }

        // Calculate the distance to travel per second in the x and y directions
        double dxPerSecond = targetX / travelTimeSeconds;
        double dyPerSecond = targetY / travelTimeSeconds;

        // Simulate the drone's travel to the fire location
        for (int currentSecond = 1; currentSecond <= travelTimeSeconds; currentSecond++) {
            // Calculate the current position of the drone
            double currentX = dxPerSecond * currentSecond;
            double currentY = dyPerSecond * currentSecond;
            System.out.printf("[Drone %d] Traveling... Current position: (%.2f, %.2f)%n", id, currentX, currentY);

            try {
                // Sleep for 1 second to simulate the drone's travel time
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public void extinguishFire() {
        System.out.println("[Drone " + id + "] Dropping firefighting agent...");

        try {
            Thread.sleep(getExtinguishTime(getRequiredVolume(assignedFire)) /* *1000 */);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        carryingVolume -= 10;

        if (carryingVolume <= 0) {
            System.out.println("[Drone " + id + "] OUT OF WATER! Returning to base.");
            sendResponse(DroneResponse.ResponseType.REFILL_REQUIRED);
        }
    }

    public void returnToBase() {
        System.out.println("[Drone " + id + "] Returning to base...");

        try {
            Thread.sleep(getTravelTime(assignedFire) /* *1000 */);
            System.out.println("[Drone " + id + "] Reached base.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void refillTank() {
        System.out.println("[Drone " + id + "] Refilling tank...");
        carryingVolume = attributes.get("maxCapacity");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void handleFault() {
        System.out.println("[Drone " + id + "] FAULT detected. Returning to base...");
        sendResponse(DroneResponse.ResponseType.FAILURE);
        this.assignedFire = null;
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void handleSuccess() {
        System.out.println("[Drone " + id + "] Fire extinguished successfully!");

        this.assignedFire.setSeverity(Event.Severity.OUT);
        sendResponse(DroneResponse.ResponseType.SUCCESS);
        this.assignedFire = null;
    }

    private void sendResponse(DroneResponse.ResponseType responseType) {
        DroneResponse response = new DroneResponse(assignedFire, id, responseType);
        System.out.println("[Drone " + id + "] Response sent: " + responseType);

        byte[] data = response.serializeResponse();
        DatagramPacket packet = new DatagramPacket(data, data.length, schedulerAddr, this.schedulerPort);
        try {
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "Drone " + id + " (State: " + currentState + ")";
    }

    public Event getAssignedFire() {
        return assignedFire;
    }

    // Testing Only
    public DatagramSocket getSocket() {
        return this.socket;
    }

    public static void main(String[] args) {
        Drone drone = new Drone(6000);
        drone.start();
    }
}
