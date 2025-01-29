import java.util.ArrayList;
import java.util.HashMap;

public class Scheduler extends Thread{
    private Drone drone; //Only one drone for iteration 1
    private FireIncidentSubsystem fireIncidentSubsystem;
    private HashMap<Integer,HashMap<String, String>> zone;

    /**
     * Constructor for new Scheduler object
     *
     * @param fireIncidentSubsystem
     */
    public Scheduler(FireIncidentSubsystem fireIncidentSubsystem){
        this.fireIncidentSubsystem = fireIncidentSubsystem;
    }

    public void newFireRequest(HashMap<Integer,HashMap<String, String>> zone){
        //TODO
        dispatchDrone();
    }

    private void dispatchDrone(){
        Drone drone = chooseDrone();
        drone.someFunction(); //TODO
    }

    /**
     * Run by the Scheduler to choose which drone will be sent to the fire zone
     *
     * @return the drone that will be sent to the fire zone
     */
    private Drone chooseDrone(){
        return drone;
    }

    /**
     * Sets the drone for the Scheduler
     *
     * @param drone the drone the Scheduler will use to send to fires zones
     */
    public void addDrone(Drone drone){
        this.drone = drone;
    }
}
