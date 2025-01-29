import java.util.ArrayList;
import java.util.HashMap;

public class Scheduler extends Thread{
    private FireIncident fireIncident;

    //Water in liters required to put out a fire of each severity
    private final int LOW = 10;
    private final int MODERATE = 20;
    private final int HIGH = 30;

    private HashMap<Integer, Drone> drones; //<ID, Drone>
    private HashMap<Integer, HashMap<String, String>> fireEvents; //<ID, <Time, ZoneID, EventType, Severity>>
    private HashMap<Integer, HashMap<String, String>> zones; //<ID, <ZoneStart, ZoneEnd>>

    /**
     * Constructor for Scheduler
     *
     * @param fireIncident
     */
    public Scheduler(FireIncident fireIncident){
        this.fireIncident = fireIncident;

        this.drones = new HashMap<Integer, Drone>();
        this.fireEvents = new HashMap<Integer, HashMap<String, String>>();
        this.zones = new HashMap<Integer, HashMap<String, String>>();
    }

    /**
     *
     *
     * @param zone
     */
    public synchronized void newFireRequest(HashMap<Integer,HashMap<String, String>> zone){
        //TODO
        dispatchDrone(zone);
    }

    /**
     *
     *
     */
    public synchronized void droneReturn(){
        this.fireIncident.put(); //TODO signal the fireIncident that drone has returned
    }

    /**
     * Runs the dispatch function for the chosen drone
     */
    private Boolean dispatchDrone(HashMap<Integer,HashMap<String, String>> zone){
        Drone chosen_drone = chooseDrone();
        if (chosen_drone == null){
            System.out.print("There is no drone to send");
            return false;
        }
        chosen_drone.put(); //TODO signal the drone
        return true;
    }

    /**
     * Run by the Scheduler to choose which drone will be sent to the fire zone
     *
     * @return the drone that will be sent to the fire zone
     */
    private Drone chooseDrone(){
        //Chooses the first drone
        for (Drone drone: this.drones.values()){
            return drone;
        }
        return null;
    }

    /**
     * Sets the drone for the Scheduler
     *
     * @param drone the drone the Scheduler will use to send to fires zones
     */
    public void addDrone(Integer droneID, Drone drone){
        this.drones.put(droneID, drone);
    }
}

