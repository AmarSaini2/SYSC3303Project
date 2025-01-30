import java.util.HashMap;

public class Scheduler extends Thread{
    private FireIncident fireIncident;
    private HashMap<Integer, Drone> drones; //<ID, Drone>

    //Water in liters required to put out a fire of each severity
    private final int LOW = 10;
    private final int MODERATE = 20;
    private final int HIGH = 30;

    public Scheduler(FireIncident fireIncident){
        this.fireIncident = fireIncident;
        this.drones = new HashMap<Integer, Drone>();
    }

    public void newFireRequest(HashMap<Integer,HashMap<String, String>> zone){
        //TODO
        dispatchDrone(zone);
    }

    public void droneReturn(HashMap<Integer,HashMap<String, String>> zone){
        this.fireIncident.put(zone); //TODO signal the fireIncident that drone has returned
    }

    private Boolean dispatchDrone(HashMap<Integer,HashMap<String, String>> zone){
        Drone chosen_drone = chooseDrone();
        if (chosen_drone == null){
            System.out.print("There is no drone to send");
            return false;
        }
        chosen_drone.send(); //TODO signal the drone
        return true;
    }

    private Drone chooseDrone(){
        //Chooses the first drone
        for (Drone drone: this.drones.values()){
            return drone;
        }
        return null;
    }

    public void addDrone(Integer droneID, Drone drone){
        this.drones.put(droneID, drone);
    }
}

