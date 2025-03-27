import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

/**
 * TODO for me
 *
 * Add super state that that handles fault?
 * Add a moveTo method moveTo(targetX, targetY) to be used for return to base and to fire
 * Fix issue with how much agent to drop
 * Add states to Scheduler
 *
 */

public class Drone extends Thread {
    private static int idCounter = 0;
    private int id;

    private Event assignedFire;
    private final HashMap<String, Double> attributes;
    private double carryingVolume;
    private double agentDropAmount;

    private final Random random;

    private DroneState currentState;
    private DroneFSM droneFSM;

    private DatagramSocket socket;
    private InetAddress schedulerAddress;
    private int schedulerPort;

    private boolean finish;

    public Drone(int schedulerPort) {
        this.id = idCounter++;

        this.random = new Random();
        this.attributes = new HashMap<>();

        // Setting up drone attributes
        attributes.put("takeoffSpeed", 3.0);
        attributes.put("travelSpeed", 12.0);
        attributes.put("flowRate", 1.25);
        attributes.put("maxCapacity", 15.0);

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

    //TODO add a startup state and set this as the action
    protected void sendWakeupMessage() {
        try {
            byte[] data = ("ONLINE:"+this.id).getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"),
                    this.schedulerPort);
            socket.send(packet);
            System.out.println("[Drone " + this.id + "] Wakeup message sent");

            DatagramPacket response = new DatagramPacket(new byte[2048], 2048);
            socket.receive(response);
            System.out.println("[Drone " + this.id + "] Received confirmation from Scheduler");
            schedulerAddress = response.getAddress();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Assigns a fire event to the drone if it's idle.
     */
    public synchronized boolean assignFire(Event fire) {
        if (this.assignedFire == null) {
            this.assignedFire = fire;
            currentState = DroneFSM.getState("EnRoute");
            //System.out.println("[Drone " + this.id + "] Fire assigned: " + fire);
            notifyAll(); // Wake up the thread
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        sendWakeupMessage();
        while (!finish) {
            // Execute the function for the current state
            currentState.action(this);
            // Move to next state
            //currentState.goNextState(this); //TODO needs to be moved into the action
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
        DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
        try {
            socket.receive(packet);

            String message = new String(packet.getData(), 0, packet.getLength());
            String[] splitMessage = message.split(":");

            switch (splitMessage[0].toUpperCase()){
                case "NEW_EVENT":
                    Event event = Event.deserializeEvent(Arrays.copyOfRange(packet.getData(),10, packet.getLength()));
                    System.out.println("[Drone "+this.id+"] Received: " +event);
                    this.assignFire(event);
                    break;
                case "FINISH":
                    System.out.println("[Drone "+this.id+"]: Received: FINISH");
                    this.finish = true;
                    break;
                default:
                    System.out.println("Invalid message: "+message);
            }
        } catch (SocketTimeoutException e) {

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
            //System.out.printf("[Drone %d] Traveling... Current position: (%.2f, %.2f)%n", id, currentX, currentY);

            try {
                // Sleep for 1 second to simulate the drone's travel time
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        String response = sendReceive();
        String[] splitMessage = response.split(":");

        switch (splitMessage[0].toUpperCase()){
            case "FINISH":
                this.finish = true;
                break;
            case "FAULT":
                currentState.handleFault(this);
                break;
            case "DROP":
                this.agentDropAmount = Double.parseDouble(splitMessage[1]);
                currentState.goNextState(this);
                break;
            default:
                System.out.println("Invalid message: "+response);
        }

    }

    public void extinguishFire() {
        //System.out.println("[Drone " + id + "] Dropping firefighting agent...");
        try {
            //TODO check if this makes sense
            double timeRequired = this.agentDropAmount/this.attributes.get("flowRate");
            Thread.sleep((int)timeRequired);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.carryingVolume -= this.agentDropAmount;
        this.agentDropAmount = 0.0;

        String response = sendReceive();
        String[] splitMessage = response.split(":");

        switch (splitMessage[0].toUpperCase()){
            case "FINISH":
                this.finish = true;
                break;
            case "FAULT":
                currentState.handleFault(this);
                break;
            case "OK":
                currentState.goNextState(this);
                break;
            default:
                System.out.println("Invalid message: "+response);
        }
    }

    public void returnToBase() {
        //System.out.println("[Drone " + id + "] Returning to base...");
        try {
            Thread.sleep(getTravelTime(assignedFire) /* *1000 */);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //System.out.println("[Drone " + id + "] Reached base.");

        String response = sendReceive();
        String[] splitMessage = response.split(":");
        switch (splitMessage[0].toUpperCase()){
            case "FINISH":
                this.finish = true;
                break;
            case "FAULT":
                currentState.handleFault(this);
                break;
            case "OK":
                currentState.goNextState(this);
                break;
            default:
                System.out.println("Invalid message: "+response);
        }
    }

    public void refillTank() {
        //System.out.println("[Drone " + id + "] Refilling tank...");
        carryingVolume = attributes.get("maxCapacity");

        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String response = sendReceive();
        String[] splitMessage = response.split(":");
        switch (splitMessage[0].toUpperCase()){
            case "FINISH":
                this.finish = true;
                break;
            case "FAULT":
                currentState.handleFault(this);
                break;
            case "OK":
                currentState.goNextState(this);
                break;
            default:
                System.out.println("Invalid message: "+response);
        }
    }

    public void handleFault() {
        System.out.println("[Drone " + id + "] FAULT detected. Returning to base...");
        this.assignedFire = null;

        String response = sendReceive();
        String[] splitMessage = response.split(":");
        switch (splitMessage[0].toUpperCase()){
            case "FINISH":
                this.finish = true;
                break;
            case "OK":
                currentState.goNextState(this);
                break;
            default:
                System.out.println("Invalid message: "+response);
        }
    }

    //TODO probably want to remove this and add to to the extinguish fire state
    public void handleSuccess() {
        //System.out.println("[Drone " + id + "] Fire extinguished successfully!");
        this.assignedFire.setSeverity(Event.Severity.OUT);
        String response = sendReceive();
        this.assignedFire = null;
    }

    private String sendReceive(){
        try {
            String sendMessage;
            if(this.assignedFire == null){
                sendMessage = String.format("%s:%d:%f",this.getStateAsString(), this.id, this.carryingVolume);
            }else{
                sendMessage = String.format("%s:%d:%d:%f",this.getStateAsString(), this.id, this.assignedFire.getId(), this.carryingVolume);
            }
            System.out.println("[Drone " + id + "] Sent: " + sendMessage);
            DatagramPacket sendPacket = new DatagramPacket(sendMessage.getBytes(), sendMessage.getBytes().length, schedulerAddress, this.schedulerPort);
            socket.send(sendPacket);

            DatagramPacket receivePacket = new DatagramPacket(new byte[2048], 2048);
            socket.receive(receivePacket);
            String receiveMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("[Drone " + id + "] Received: " + receiveMessage);

            return receiveMessage;
        } catch (IOException e){
            throw new RuntimeException(e);
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
