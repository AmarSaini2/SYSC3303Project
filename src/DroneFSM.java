import java.util.HashMap;
import java.util.Map;

interface DroneState {

    public void goNextState(Drone drone);

    public void action(Drone drone);

    public String getStateString();

    // by default, this event should not change state
    default public void handleFault(Drone drone) {
        drone.setState("ReturningToBase");
    };

    // by default, this event should not change state
    default public void handleNewEvent(Drone drone) {};

}

class DroneIdle implements DroneState {
    @Override
    public void goNextState(Drone drone) {
        drone.setState("EnRoute");
    }

    @Override
    public void action(Drone drone) {
        drone.sleepMode();
    }

    @Override
    public String getStateString() {
        return "Idle";
    }
}

abstract class DroneActive implements DroneState {
    public void handleFault(Drone drone) {
        drone.setState("ReturningToBase");
    };
}

class DroneStartUp extends DroneActive {

    @Override
    public void goNextState(Drone drone) {
        drone.setState("Idle");
    }

    @Override
    public void action(Drone drone) {
        drone.sendWakeupMessage();
    }

    @Override
    public String getStateString() {
        return "Start Up";
    }
}

class DroneEnRoute extends DroneActive {
    @Override
    public void goNextState(Drone drone) {
        drone.setState("DroppingAgent");
    }

    @Override
    public void action(Drone drone) {
        drone.travelToFire();
    }

    @Override
    public String getStateString() {
        return "En Route";
    }
}

class DroneDroppingAgent extends DroneActive {
    @Override
    public void goNextState(Drone drone) {
        drone.setState("ReturningToBase");
    }

    @Override
    public void action(Drone drone) {
        drone.extinguishFire();
    }

    @Override
    public String getStateString() {
        return "Dropping Agent";
    }
}

class DroneReturningToBase extends DroneActive {
    @Override
    public void goNextState(Drone drone) {
        drone.setState("FillingTank");
    }

    @Override
    public void handleNewEvent(Drone drone){drone.setState("EnRoute");}

    @Override
    public void action(Drone drone) {
        drone.returnToBase();
    }

    @Override
    public String getStateString() {
        return "Returning To Base";
    }
}

class DroneFillingTank extends DroneActive {
    @Override
    public void goNextState(Drone drone) {
        drone.setState("Idle");
    }

    @Override
    public void action(Drone drone) {
        drone.refillTank();
    }

    @Override
    public String getStateString() {
        return "Filling Tank";
    }
}

class DroneFault implements DroneState {
    @Override
    public void goNextState(Drone drone) {
        drone.setState("ReturningToBase");
    }

    @Override
    public void action(Drone drone) {
        drone.handleFault();
    }

    @Override
    public String getStateString() {
        return "Fault";
    }
}

public class DroneFSM {
    public static final Map<String, DroneState> stateTable = new HashMap<>();

    public void initialize(Drone drone) {
        stateTable.put("StartUp", new DroneStartUp());
        stateTable.put("Idle", new DroneIdle());
        stateTable.put("EnRoute", new DroneEnRoute());
        stateTable.put("DroppingAgent", new DroneDroppingAgent());
        stateTable.put("ReturningToBase", new DroneReturningToBase());
        stateTable.put("FillingTank", new DroneFillingTank());
        stateTable.put("Fault", new DroneFault());
    }

    public static DroneState getState(String stateName) {
        return stateTable.get(stateName);
    }
}
