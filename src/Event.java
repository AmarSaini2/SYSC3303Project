import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * The Event class represents a fire incident event, storing details such as
 * time, zone, type, and severity.
 */
public class Event implements Comparable<Event>, Serializable {
    private int id;
    private static int counter = 0;
    Zone zone;
    private ArrayList<Object[]> assignedDrones;

    public static enum Type {
        FIRE_DETECTED, DRONE_REQUEST
    }; // Enum representing event types

    private Type type;
    private LocalTime time;

    public static enum Severity {
        HIGH, MODERATE, LOW, OUT
    }; // Enum representing severity levels of a fire incident.

    private Severity severity;

    private static final long serialVersionUID = 1L;//added for serialization compatibility

    private double agentRequired;

    private double agentSent;

    private double agentDropping;
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
        this.assignedDrones = new ArrayList<>();

        switch (this.getSeverity()) {
            case HIGH:
                this.agentRequired = 30.0;
                break;
            case MODERATE:
                this.agentRequired = 20.0;
                break;
            case LOW:
                this.agentRequired = 10.0;
                break;
        }

        this.agentSent = 0.0;
    }

    /**
     * Gets the amount of agent currently being sent to this event
     * @return the amount of agent being sent to the event
     */
    public double getAgentSent(){
        return this.agentSent;
    }

    /**
     * Sets the amount of agent currently being sent to this event
     * @param agentSent the amount of agent being sent to the event
     */
    public void setAgentSent(Double agentSent){
        this.agentSent = agentSent;
    }

    /**
     * Gets the amount of agent currently required by the event
     * @return the amount of agent currently required by the event to extinguish the fire
     */
    public double getAgentRequired(){
        return this.agentRequired;
    }

    /**
     * Changes the amount of agent that is needed to extinguish the fire
     * @param agentRequired the new amount of agent required to extinguish the fire
     */
    public void setAgentRequired(Double agentRequired){
        this.agentRequired = agentRequired;
    }

    /**
     * Gets the amount of agent currently being dropped on the fire
     * @return the amount of agent currently being dropped on the fire
     */
    public double getAgentDropping(){
        return this.agentDropping;
    }

    /**
     * Changes the amount of agent currently being dropped on the fire
     * @param agentDropping the new amount of agent being dropped on the fire
     */
    public void setAgentDropping(Double agentDropping){
        this.agentDropping = agentDropping;
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
     * Adds a new drone to the arraylist of assigned drones
     * @param entry the drone to be added
     */
    public void addAssignedDrone(Object[] entry){
        this.assignedDrones.add(entry);
    }

    /**
     * Removes a drone from the arraylist of assigned drones
     * @param entry the drone to be removed
     */
    public void removeAssignedDrone(Object[] entry){
        this.assignedDrones.remove(entry);
    }

    /**
     * Gets the Drones that are assigned to this event
     * @return an arraylist of Drones that are assigned to this event
     */
    public ArrayList<Object[]> getAssignedDrones(){
        return this.assignedDrones;
    }

    /**
     * Creates a message that will be passed using rpc
     * @param command the command associated with the message
     * @return the message which is a combination of the command and serialized event
     */
    public byte[] createMessage(String command){
        byte[] commandBytes = command.getBytes();
        byte[] serializedEvent = this.serializeEvent();

        byte[] message = new byte[commandBytes.length+serializedEvent.length];

        for(int i = 0; i < commandBytes.length; i++){
            message[i] = commandBytes[i];
        }

        for(int i = 0; i < serializedEvent.length; i++){
            message[commandBytes.length + i] = serializedEvent[i];
        }

        return message;
    }

    /**
     * Returns a string representation of the event.
     *
     * @return A formatted string containing event details.
     */
    @Override
    public String toString() {
        // convert timestamp duration obj into readable string as part of this printout
        return "Event [id: " + this.id + ", time: " + this.time + ", zone:" + this.zone.getId() + ", type: " + this.type + ", severity: "
                + this.severity + "]";
    }

    /**
     * Compares this event with other events
     * @param other the object to be compared.
     * @return returns whether this event has greater value then the other eventw
     */
    @Override
    public int compareTo(Event other){
        return this.severity.ordinal() - other.severity.ordinal();
    }

    /**
     * Serialized this event
     * @return a byte array of the serialized event
     */
    public byte[] serializeEvent(){
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try{
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(this);
            out.flush();
            return byteOut.toByteArray();
        }catch(IOException e){
            e.printStackTrace();
        }
        return null;//return null for failure
    }

    /**
     * Deserializes a byte array into an event object
     * @param data the byte array being deserialized
     * @return the event from the byte array
     */
    public static Event deserializeEvent(byte[] data){
        ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
        try{
            ObjectInputStream in = new ObjectInputStream(byteIn);
            return (Event) in.readObject();
        }catch(IOException e){
            e.printStackTrace();
        }catch( ClassNotFoundException f){
            f.printStackTrace();
        }
        return null;//return null for failure
    }

}