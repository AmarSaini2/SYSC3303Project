import java.time.Duration;

public class Event {
    private int id;
    Zone zone;
    public static enum Type {FIRE_DETECTED, DRONE_REQUEST};
    private Type type;
    private Duration time;

    public static enum Severity {HIGH, MODERATE, LOW, OUT};
    private Severity severity;

    Event(Duration time, Zone zone, int id, Type type, Severity severity){
        this.time = time;
        this.zone = zone;
        this.id = id;
        this.type = type;
        this.severity = severity;
    }

    public Severity getSeverity() {
        return severity;
    }

    public Type getType() {
        return type;
    }

    public Duration getTime() {
        return time;
    }

    public Zone getZone() {
        return zone;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        //convert timestamp duration obj into readable string as part of this printout
        return "Event [time: " + String.format("%02d:%02d:%02d", this.time.toHours(),this.time.toMinutesPart(),this.time.toSecondsPart())+ ", zone:" + this.id + ", type: " + this.type + ", severity: " + this.severity + "]";
    }
    
}