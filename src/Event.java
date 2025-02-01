public class Event {
    private int time,zone,id;
    public static enum Type {FIRE_DETECTED, DRONE_REQUEST};
    private Type type;

    public static enum Severity {HIGH, MODERATE, LOW, OUT};
    private Severity severity;

    Event(int time, int zone, int id, Type type, Severity severity){
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

    public int getTime() {
        return time;
    }

    public int getZone() {
        return zone;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Event [time: " + this.time + ", zone:" + this.id + ", type: " + this.type + ", severity: " + this.severity + "]";
    }
}