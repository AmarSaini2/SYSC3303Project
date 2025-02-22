import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * The Event class represents a fire incident event, storing details such as
 * time, zone, type, and severity.
 */
public class Event {
    private int id;
    private static int counter = 0;
    Zone zone;

    public static enum Type {
        FIRE_DETECTED, DRONE_REQUEST
    }; // Enum representing event types

    private Type type;
    private LocalTime time;

    public static enum Severity {
        HIGH, MODERATE, LOW, OUT
    }; // Enum representing severity levels of a fire incident.

    private Severity severity;

    /**
     * Constructs an Event instance. The identifier is created from an internal
     * static variable to ensure unique id values.
     *
     * @param time     The timestamp of the event.
     * @param zone     The zone in which the event occurred.
     * @param type     The type of event (e.g., fire detected, drone request).
     * @param severity The severity level of the event.
     */
    Event(LocalTime time, Zone zone, Type type, Severity severity) {
        this.time = LocalTime.parse(time.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
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
    public LocalTime getTime() {
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
     * 
     * @param time the new time of the Event
     */
    public void setTime(LocalTime time) {
        this.time = LocalTime.parse(time.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    /**
     * Sets the severity of an Event object
     * 
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
        // convert timestamp duration obj into readable string as part of this printout
        return "Event [time: " + this.time + ", zone:" + this.zone.getId() + ", type: " + this.type + ", severity: "
                + this.severity + "]";
    }

}