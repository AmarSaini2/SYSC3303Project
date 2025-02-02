import java.time.Duration;
import java.util.HashMap;

public class Drone extends Thread{
    private static int idCounter = 0;
    private Event assignedFire;
    private Scheduler scheduler;
    private HashMap<String, Double> attributes;
    private double carryingVolume; 
    private int id;

    //This can be modified to represent different types of drones down the road
    public static enum types {X1400, FREEFLY_ALTAX, T_DRONES_M1500, JUNO_M175, XAG_V40, GWD_415H};
    private types type;

    /**
     * Baseline constructor for Drone class.
     * @param scheduler A scheduler is required for thread-safe running.
     */
    Drone(Scheduler scheduler){
        this.attributes = new HashMap<String, Double>();
        //arbitrarily assuming takeoff speed as 3.0m/s
        attributes.put("takeoffSpeed", 3.0);
        //arbitrarily assuming speed as 3.0m/s
        attributes.put("travelSpeed", 3.0);
        //flow rate as 1.25L/s
        attributes.put("flowRate", 1.25);
        //max capacity at 15L
        attributes.put("maxCapacity", 15.0);
        //assume all drones to be of this type. Can be reassigned later i guess.
        type = types.X1400;
        //initialize at an empty tank
        carryingVolume = 0.0;
        this.scheduler = scheduler;
        this.id = idCounter;
        idCounter++;
    }

    /**
     * Alternative constructor for Drone class.
     * @param scheduler A scheduler is required for thread-safe running.
     * @param type Drone type, viewable under the public enumerated data type "Types".
     */
    Drone(Scheduler scheduler, types type){
        this(scheduler);
        this.type = type;
    }

    /**
     * A method to fill the tank. At the moment, it instantly refills the tank to full. This method can be modified later to reflect taking some amount of time to fill back the tank to full.
     */
    public void fillTank(){
        carryingVolume = attributes.get("maxCapacity");
    }

    /**
     * getter for ID.
     * @return the ID of the drone to be returned.
     */
    public int getID(){
        return this.id;
    }

    /**
     * An internal method that should only be called by send(). This calculates the amount of fire retardant needed to put out the fire.
     * @param fire the fire to put out.
     * @return the amount of fire retardant needed to put out the fire, in liters.
     */
    private int getRequiredVolume(Event fire){
        //I'll just have default behaviour assume the fire is HIGH - better safe than sorry here.
        int requiredVolume = 15;
        Event.Severity severity = fire.getSeverity();

        //I'm pretty sure java is gonna force cast the severity to string from an enumerated type. Shame, woulda liked a swtich here.
        if(severity.equals(Event.Severity.HIGH)){requiredVolume = 15;}
        if(severity.equals(Event.Severity.MODERATE)){requiredVolume = 10;}
        if(severity.equals(Event.Severity.LOW)){requiredVolume = 5;}
        if(severity.equals(Event.Severity.OUT)){requiredVolume = 0;}

        return requiredVolume;
    }
    
    /**
     * An internal method that should only be called by send(). This calculates the amount of time needed to put out the fire.
     * @param requiredVolume the amount of fire retardant needed.
     * @param fire the fire to put out.
     * @return the amount of time needed to put out the fire, in seconds.
     */
    private int getRequiredTime(int requiredVolume, Event fire){
        int requiredTime = (int) (requiredVolume*attributes.get("flowRate"));
        //for now, I'm going to just assume it takes 3 seconds to get to the fire. We can implement this later when we actually have zones and positions to calculate movement.
        int travelTime = getTravelTime(fire);

        requiredTime += travelTime;

        return requiredTime;
    }

    /**
     * The main function to send a drone off to a fire. This function encapsulates all behaviour for putting out a fire, namely travelling to a fire, finding the required resources to put out the fire, putting out the fire, and returning an updated fire status.
     * @param fire the fire the drone should be sent towards.
     */
    public void send(Event fire){
        assignedFire = fire;

        //calculate the amount of water needed.
        int requiredVolume = getRequiredVolume(fire);

        if(requiredVolume>carryingVolume){fillTank();}

        //calculate the amount of time needed (assuming seconds)
        int requiredTime = getRequiredTime(requiredVolume, fire);

        try {
            Thread.sleep(requiredTime);
        } catch (Exception e) {}

        Event newFireStatus = new Event(fire.getTime().plusSeconds(requiredTime), fire.getZone(), fire.getId(), Event.Type.DRONE_REQUEST, Event.Severity.OUT);
        scheduler.sendUpdate(newFireStatus);
    }

    /**
     * an internal method to find the amount of time needed to travel to a fire. This returns a static value for now, it can be modified later to be calculating something.
     * @param fire the fire to travel to
     * @return the amount of time to get to a fire, in seconds.
     */
    private int getTravelTime(Event fire){
        //for now, I'm going to just assume it takes 3 seconds to get to the fire. We can implement this later when we actually have zones and positions to calculate movement.
        return 3;
    }

    /**
     * the run method implementation as required by the Runnable interface. The thread busy waits and then calls a request for fire.
     */
    public void run(){
        //busy wait the scheduler here for fires every second. I'd work with what exsists in scheduler.java but busy wait is required by the proj specs
        //Scheduler guaranteed to exsist because I require it as an argument when initing a drone.
        while(true){
            Event event = scheduler.requestForFire();
            if(event == null){
                break;
            }
            send(event);
        }
    }
}
