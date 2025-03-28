import java.util.HashMap;
import java.util.Map;

interface SchedulerState{

    public default void handleOn(Scheduler scheduler) {}

    public void action(Scheduler scheduler);

    public String getStateString();
}

class SchedulerIdle implements SchedulerState{

    @Override
    public void handleOn(Scheduler scheduler) {
        scheduler.setState("Active");
    }

    @Override
    public void action(Scheduler scheduler) {
        scheduler.idleAction();
    }

    @Override
    public String getStateString() {
        return "Idle";
    }
}

class SchedulerActive implements SchedulerState{
    @Override
    public void action(Scheduler scheduler) {
        scheduler.activeAction();
    }

    @Override
    public String getStateString() {
        return "Active";
    }
}

public class SchedulerFSM {
    public static final Map<String, SchedulerState> stateTable = new HashMap<>();

    public void initialize(){
        stateTable.put("Idle", new SchedulerIdle());
        stateTable.put("Active", new SchedulerActive());
    }

    public static SchedulerState getState(String stateName){
        return  stateTable.get(stateName);
    }
}
