import java.util.ArrayList;
import java.util.HashMap;

public class Scheduler extends Thread{
    Drone drone;
    FireIncidentSubsystem fireIncidentSubsystem;
    HashMap<Integer,HashMap<String, String>> zone;

    public Scheduler(FireIncidentSubsystem fireIncidentSubsystem){
        this.fireIncidentSubsystem = fireIncidentSubsystem;
    }

    public void reportFire(HashMap<Integer,HashMap<String, String>> zone){
        //TODO
        dispatchDrone();
    }

    private void dispatchDrone(){
        drone.someFunction(); //TODO
    }

    public void addDrone(Drone drone){
        //Will need to be redone later
        this.drone = drone;
    }
}
