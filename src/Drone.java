
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Drone extends Thread {

    private static int idCounter = 0;
    private int id;

    private Event assignedFire;
    private final HashMap<String, Double> attributes;
    private double carryingVolume;
    private double agentDropAmount;
    private final int SLEEPMULTIPLIER = 50;

    private final Random random;

    private DroneState currentState;
    private DroneFSM droneFSM;

    private DatagramSocket socket;
    private InetAddress schedulerAddress;
    private int schedulerPort;

    private boolean finish;

    private double[] currentLocation;

    private Map<String, FaultEvent.Type> faultInstructions = new HashMap<>();

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
        this.currentState = DroneFSM.getState("StartUp");

        try {
            this.socket = new DatagramSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.schedulerPort = schedulerPort;

        this.finish = false;

        this.currentLocation = new double[]{0, 0};

    }

    public Drone(int schedulerPort, String faultFileName) {
        this(schedulerPort);
        if (faultFileName != null && !faultFileName.isEmpty()) {
            loadFaultInstructions(faultFileName);
        }

    }

    private void loadFaultInstructions(String faultFileName) {
        File file = new File(faultFileName);
        if (!file.exists()) {
            System.out.println("Fault injection file " + faultFileName + " does not exist.");
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                // String[] parts = line.split("\\s+");
                String[] parts = line.split("\\s*,\\s*");
                if (parts.length >= 2) {
                    String stage = parts[0].toUpperCase();
                    // Use the provided FaultEvent.Type enum.
                    FaultEvent.Type faultType = FaultEvent.Type.valueOf(parts[1].toUpperCase());
                    System.out
                            .println("[Drone " + id + "] Fault injection for stage: " + stage + ", type: " + faultType);
                    faultInstructions.put(stage, faultType);

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private FaultEvent.Type getFaultForStage(String stage) {
        stage = stage.toUpperCase();
        if (faultInstructions.containsKey(stage)) {
            return faultInstructions.remove(stage);
        }
        return null;
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
            notifyAll(); // Wake up the thread
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        while (!finish || this.currentState != DroneFSM.getState("Idle")) {
            // Execute the function for the current state
            currentState.action(this);
        }

        //Signal scheduler that the drone is finished
        String sendMessage = "FINISHED:"+this.id;
        System.out.println("[Drone " + id + "] Sent: " + sendMessage);
        DatagramPacket sendPacket = new DatagramPacket(sendMessage.getBytes(), sendMessage.getBytes().length,
                schedulerAddress, this.schedulerPort);
        try {
            socket.send(sendPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Calculates estimated travel time to the fire zone.
     */
    private int getTravelTime(Event fire) {
        Zone zone = fire.getZone();
        double distance = Math.sqrt(zone.getStart()[0] * zone.getStart()[0]
                + zone.getEnd()[1] * zone.getEnd()[1]);
        return (int) (distance / attributes.get("travelSpeed"));
    }

    // ========== STATE HANDLING FUNCTIONS ==========
    protected void sendWakeupMessage() {
        try {
            String sendMessage = "ONLINE:" + this.id;
            System.out.println("[Drone " + id + "] Sent: " + sendMessage);
            DatagramPacket sendPacket = new DatagramPacket(sendMessage.getBytes(), sendMessage.getBytes().length,
                    InetAddress.getByName("255.255.255.255"), this.schedulerPort);
            socket.send(sendPacket);

            DatagramPacket receivePacket = new DatagramPacket(new byte[2048], 2048);
            socket.receive(receivePacket);
            String receiveMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("[Drone " + id + "] Received: " + receiveMessage);
            schedulerAddress = receivePacket.getAddress();

            currentState.goNextState(this);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void sleepMode() {
        System.out.println("[Drone " + id + "] IDLE - Waiting for assignment...");
        DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
        try {
            socket.receive(packet);

            String message = new String(packet.getData(), 0, packet.getLength());
            String[] splitMessage = message.split(":");

            switch (splitMessage[0].toUpperCase()) {
                case "NEW_EVENT":
                    Event event = Event.deserializeEvent(Arrays.copyOfRange(packet.getData(), 10, packet.getLength()));
                    System.out.println("[Drone " + this.id + "] Received: " + event);
                    this.assignFire(event);
                    currentState.goNextState(this);
                    break;
                case "FINISH":
                    System.out.println("[Drone " + this.id + "]: Received: FINISH");
                    this.finish = true;
                    break;
                default:
                    System.out.println("Invalid message: " + message);
            }
        } catch (SocketTimeoutException e) {

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void moveTo(double[] targetLocation) {
        System.out.println(String.format("Drone %d moving to (%.2f,%.2f)", this.id, targetLocation[0], targetLocation[1]));

        //Get x and y distance from target
        double xDistance = targetLocation[0] - this.currentLocation[0];
        double yDistance = targetLocation[1] - this.currentLocation[1];

        //Get the x and y ratios
        double totalDistance = Math.sqrt(xDistance * xDistance + yDistance * yDistance);
        double xRatio = xDistance / totalDistance;
        double yRatio = yDistance / totalDistance;

        //Calculate number of seconds it would take to reach zone
        int secondsRequired = (int) Math.floor(totalDistance / this.attributes.get("travelSpeed"));

        //Move the drone
        for (int i = 0; i < secondsRequired; i++) {
            this.currentLocation[0] += this.attributes.get("travelSpeed") * xRatio;
            this.currentLocation[1] += this.attributes.get("travelSpeed") * yRatio;
            String s = String.format("LOCATION:%d:%d:%d", this.id, (int)this.currentLocation[0], (int)this.currentLocation[1]);
            DatagramPacket packet = new DatagramPacket(s.getBytes(), s.getBytes().length, schedulerAddress, schedulerPort);
            //sendReceive(String.format("LOCATION:%d:%d:%d", this.id, (int)this.currentLocation[0], (int)this.currentLocation[1]));
            try {
                socket.send(packet);
                sleep(SLEEPMULTIPLIER);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void travelToFire() {
        // System.out.println("[Drone " + id + "] Traveling to fire at Zone: " +
        // assignedFire.getZone().getId());

        double[] center = assignedFire.getZone().getCenter();
        moveTo(center);

        String response = sendReceive(String.format("%s:%d:%d:%.2f", this.getStateAsString(), this.id,
                this.assignedFire.getId(), this.carryingVolume));
        String[] splitMessage = response.split(":");

        switch (splitMessage[0].toUpperCase()) {
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
                System.out.println("Invalid message: " + response);
        }
    }

    private void injectFault(FaultEvent.Type faultType) {
        FaultEvent faultEvent = new FaultEvent(LocalTime.now(), faultType, this.id, this.assignedFire);
        byte[] faultData = faultEvent.createMessage("FAULT_EVENT:" + this.carryingVolume + ":");
        try {
            DatagramPacket faultPacket = new DatagramPacket(faultData, faultData.length, schedulerAddress,
                    schedulerPort);
            socket.send(faultPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
        currentState.handleFault(this);
    }

    public void extinguishFire() {
        // System.out.println("[Drone " + id + "] Dropping firefighting agent...");
        FaultEvent.Type faultToInject = getFaultForStage(DroneFSM.getState("DroppingAgent").getStateString());
        if (faultToInject != null) {
            System.out.println("[Drone " + id + "] Fault injection triggered for ExtinguishFire: " + faultToInject);
            injectFault(faultToInject);
            return;
        }

        try {
            // TODO check if this makes sense
            double timeRequired = this.agentDropAmount / this.attributes.get("flowRate");
            Thread.sleep((int) timeRequired * SLEEPMULTIPLIER);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.carryingVolume -= this.agentDropAmount;

        String response = sendReceive(String.format("%s:%d:%d:%.2f:%.2f", this.getStateAsString(), this.id,
                this.assignedFire.getId(), this.agentDropAmount, this.carryingVolume));
        this.agentDropAmount = 0.0;

        String[] splitMessage = response.split(":");

        switch (splitMessage[0].toUpperCase()) {
            case "FINISH":
                this.finish = true;
                break;
            case "FAULT":
                currentState.handleFault(this);
                break;
            case "OK":
                currentState.goNextState(this);
                this.assignedFire = null;
                break;
            default:
                System.out.println("Invalid message: " + response);
        }
    }

    public void returnToBase() {
        // System.out.println("[Drone " + id + "] Returning to base...");
        moveTo(new double[]{0.0, 0.0});
        // System.out.println("[Drone " + id + "] Reached base.");

        String response = sendReceive(String.format("%s:%d", this.getStateAsString(), this.id));
        String[] splitMessage = response.split(":");
        switch (splitMessage[0].toUpperCase()) {
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
                System.out.println("Invalid message: " + response);
        }
    }

    public void refillTank() {
        // System.out.println("[Drone " + id + "] Refilling tank...");
        carryingVolume = attributes.get("maxCapacity");

        try {
            Thread.sleep(20 * SLEEPMULTIPLIER);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String response = sendReceive(String.format("%s:%d", this.getStateAsString(), this.id));
        String[] splitMessage = response.split(":");
        switch (splitMessage[0].toUpperCase()) {
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
                System.out.println("Invalid message: " + response);
        }
    }

    public void handleFault() {
        System.out.println("[Drone " + id + "] FAULT detected. Returning to base...");

        String response = sendReceive(String.format("%s:%d:%d:%.2f", this.getStateAsString(), this.id,
                this.assignedFire.getId(), this.carryingVolume));
        String[] splitMessage = response.split(":");
        switch (splitMessage[0].toUpperCase()) {
            case "FINISH":
                this.finish = true;
                break;
            case "OK":
                currentState.goNextState(this);
                break;
            default:
                System.out.println("Invalid message: " + response);
        }
        this.assignedFire = null;
    }

    private String sendReceive(String sendMessage) {
        try {
            System.out.println("[Drone " + id + "] Sent: " + sendMessage);
            DatagramPacket sendPacket = new DatagramPacket(sendMessage.getBytes(), sendMessage.getBytes().length,
                    schedulerAddress, this.schedulerPort);
            socket.send(sendPacket);

            DatagramPacket receivePacket = new DatagramPacket(new byte[2048], 2048); //this is erroneous, because it allocates too much space in the byte array, which causes excess garbage data to be assigned to the response
            socket.receive(receivePacket);
            String receiveMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("[Drone " + id + "] Received: " + receiveMessage);

            return receiveMessage;
        } catch (IOException e) {
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
        Drone drone = new Drone(6000, "src/droneFaultInjection_0.txt");
        drone.start();
        Drone drone1 = new Drone(6000);
        drone1.start();
        Drone drone2 = new Drone(6000);
        drone2.start();
    }
}
