import java.time.Duration;

/**
 * The Event class represents a fire incident event, storing details such as time, zone, type, and severity.
 */
public class Event {
    private int id;
    Zone zone;
    public static enum Type {FIRE_DETECTED, DRONE_REQUEST};
    private int zone,id;
    public static enum Type {FIRE_DETECTED, DRONE_REQUEST}; // Enum representing event types
    private Type type;
    private Duration time;

    public static enum Severity {HIGH, MODERATE, LOW, OUT}; // Enum representing severity levels of a fire incident.
    private Severity severity;

    Event(Duration time, Zone zone, int id, Type type, Severity severity){
    /**
     * Constructs an Event instance.
     *
     * @param time The timestamp of the event.
     * @param zone The zone in which the event occurred.
     * @param id The unique identifier for the event.
     * @param type The type of event (e.g., fire detected, drone request).
     * @param severity The severity level of the event.
     */
    Event(Duration time, int zone, int id, Type type, Severity severity){
        this.time = time;
        this.zone = zone;
        this.id = id;
        this.type = type;
        this.severity = severity;
    }

    /**
     * Retrieves the severity level of the event.
     *
     * @return The severity of the event.
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Retrieves the type of the event.
     *
     * @return The event type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Retrieves the timestamp of the event.
     *
     * @return The event's timestamp as a Duration object.
     */
    public Duration getTime() {
        return time;
    }

    public Zone getZone() {
    /**
     * Retrieves the zone where the event occurred.
     *
     * @return The zone ID.
     */
    public int getZone() {
        return zone;
    }

    /**
     * Retrieves the unique identifier of the event.
     *
     * @return The event ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns a string representation of the event.
     *
     * @return A formatted string containing event details.
     */
    @Override
    public String toString() {
        //convert timestamp duration obj into readable string as part of this printout
        return "Event [time: " + String.format("%02d:%02d:%02d", this.time.toHours(),this.time.toMinutesPart(),this.time.toSecondsPart())+ ", zone:" + this.id + ", type: " + this.type + ", severity: " + this.severity + "]";
    }
    
}