import java.time.Duration;

/**
 * The Event class represents a fire incident event, storing details such as time, zone, type, and severity.
 */
public class Event {
    private int id;
    private static int counter = 0;
    Zone zone;
    public static enum Type {FIRE_DETECTED, DRONE_REQUEST}; // Enum representing event types
    private Type type;
    private Duration time;

    public static enum Severity {HIGH, MODERATE, LOW, OUT}; // Enum representing severity levels of a fire incident.
    private Severity severity;

    /**
     * Constructs an Event instance. The identifier is created from an internal static variable to ensure unique id values.
     *
     * @param time The timestamp of the event.
     * @param zone The zone in which the event occurred.
     * @param type The type of event (e.g., fire detected, drone request).
     * @param severity The severity level of the event.
     */
    Event(Duration time, Zone zone, Type type, Severity severity){
        this.time = time;
        this.zone = zone;
        this.id = counter;
        counter++;
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

    /**
     * Retrieves the zone where the event occurred.
     *
     * @return The zone ID.
     */
    public Zone getZone() {
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
     * Sets the time of an Event object
     * @param time the new time of the Event
     */
    public void setTime(Duration time) {
        this.time = time;
    }

    /**
     * Sets the severity of an Event object
     * @param severity the new severity of the Event
     */
    public void setSeverity(Severity severity) {
        this.severity = severity;
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