import java.util.HashMap;

public class Scheduler extends Thread{
    private FireIncident fireIncident;
    private HashMap<Integer, Drone> drones; //<ID, Drone>

    //Water in liters required to put out a fire of each severity
    private final int LOW = 10;
    private final int MODERATE = 20;
    private final int HIGH = 30;

    /**
     * Constructor for Scheduler class
     *
     * @param fireIncident the FireIncident subsystem that the Scheduler will send messages to
     */
    public Scheduler(FireIncident fireIncident){
        this.fireIncident = fireIncident;
        this.drones = new HashMap<Integer, Drone>();
    }

    /**
     * Scheduler receives a fire request and runs dispatchDrone method
     *
     * @param zone the fire zone passed by the FireIncident subsystem
     */
    public void newFireRequest(HashMap<Integer,HashMap<String, String>> zone){
        //TODO input parameters may need to be changed
        dispatchDrone(zone);
    }

    /**
     * Scheduler receives data from drone which is then sent back to the FireIncident subsystem
     *
     * @param zone the fire zone passed by the Drone
     */
    public void droneReturn(HashMap<Integer,HashMap<String, String>> zone){
        //TODO input parameters may need to be changed
        //TODO FireIncident method needs to be added
        this.fireIncident.put(zone);
    }

    /**
     * Scheduler runs chooseDrone method runs then sends the fire zone information to the drone
     *
     * @param zone the fire zone passed from the FireIncident
     */
    private void dispatchDrone(HashMap<Integer,HashMap<String, String>> zone){
        Drone chosen_drone = chooseDrone();
        if (chosen_drone == null){
            System.out.print("There is no drone to send");
        }
        chosen_drone.send(); //TODO Drone method needs be added
    }

    /**
     * Scheduler chooses a Drone out of the drone list
     *
     * @return the Drone chosen
     */
    private Drone chooseDrone(){
        //Chooses the first drone
        for (Drone drone: this.drones.values()){
            return drone;
        }
        return null;
    }

    /**
     * Adds a Drone to the drones hashmap
     *
     * @param droneID the ID of the Drone
     * @param drone the Drone object
     */
    public void addDrone(Integer droneID, Drone drone){
        this.drones.put(droneID, drone);
    }
}

