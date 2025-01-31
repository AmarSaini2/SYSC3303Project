import java.util.HashMap;

public class Scheduler extends Thread{
    private FireIncidentSubsystem fireIncidentSubsystem;
    private HashMap<Integer, Drone> drones; //<ID, Drone>

    //Water in liters required to put out a fire of each severity
    private final int LOW = 10;
    private final int MODERATE = 20;
    private final int HIGH = 30;

    /**
     * Constructor for Scheduler class
     *
     * @param fireIncidentSubsystem the FireIncidentSubsystem that the Scheduler will send messages to
     */
    public Scheduler(FireIncidentSubsystem fireIncidentSubsystem){
        this.fireIncidentSubsystem = fireIncidentSubsystem;
        this.drones = new HashMap<Integer, Drone>();
    }

    /**
     * Scheduler receives a fire request and runs dispatchDrone method
     *
     * @param fireIncident the FireIncident passed by the FireIncidentSubsystem
     */
    public void newFireRequest(FireIncident fireIncident){
        //TODO input parameters may need to be changed
        dispatchDrone(fireIncident);
    }

    /**
     * Scheduler receives data from drone which is then sent back to the FireIncidentSubsystem
     *
     * @param fireIncident the FireIncident passed by the Drone
     */
    public void droneReturn(FireIncident fireIncident){
        //TODO input parameters may need to be changed
        //TODO FireIncident method needs to be added
        this.fireIncidentSubsystem.put(fireIncident);
    }

    /**
     * Scheduler runs chooseDrone method runs then sends the FireIncident information to the drone
     *
     * @param fireIncident the FireIncident passed from the FireIncidentSubsystem
     */
    private void dispatchDrone(FireIncident fireIncident){
        Drone chosen_drone = chooseDrone();
        if (chosen_drone == null){
            System.out.print("There is no drone to send");
        }
        chosen_drone.send(fireIncident); //TODO Drone method needs be added
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

