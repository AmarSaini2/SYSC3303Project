import java.util.HashMap;
import java.util.Map;

public class DroneFSM {
    public enum DroneState {
        IDLE, EN_ROUTE, DROPPING_AGENT, RETURNING_TO_BASE, FILLING_TANK, FAULT, SUCCESS
    }

    public static class StateTransition {
        DroneState nextState;
        Runnable action; // Function reference

        public StateTransition(DroneState nextState, Runnable action) {
            this.nextState = nextState;
            this.action = action;
        }
    }

    public static final Map<DroneState, StateTransition> stateTable = new HashMap<>();

    public static void initialize(Drone drone) {
        stateTable.put(DroneState.IDLE, new StateTransition(DroneState.EN_ROUTE, drone::sleepMode));
        stateTable.put(DroneState.EN_ROUTE, new StateTransition(DroneState.DROPPING_AGENT, drone::travelToFire));
        stateTable.put(DroneState.DROPPING_AGENT,
                new StateTransition(DroneState.RETURNING_TO_BASE, drone::extinguishFire));
        stateTable.put(DroneState.RETURNING_TO_BASE, new StateTransition(DroneState.FILLING_TANK, drone::returnToBase));
        stateTable.put(DroneState.FILLING_TANK, new StateTransition(DroneState.SUCCESS, drone::refillTank));
        stateTable.put(DroneState.SUCCESS, new StateTransition(DroneState.IDLE, drone::handleSuccess));
        stateTable.put(DroneState.FAULT, new StateTransition(DroneState.RETURNING_TO_BASE, drone::handleFault));
    }

    public static StateTransition getNextState(DroneState currentState) {
        return stateTable.getOrDefault(currentState, new StateTransition(DroneState.IDLE, () -> {
        }));
    }
}
