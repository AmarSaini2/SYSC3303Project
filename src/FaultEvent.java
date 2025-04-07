import java.io.*;
import java.time.LocalTime;

public class FaultEvent implements Serializable {
    private LocalTime timestamp;
    private Type faultType;
    private int droneID;
    private Event event;

    public static enum Type {
        STUCK_IN_FLIGHT, NOZZLE_JAM, PACKET_LOSS
    }

    // Required for serialization compatibility
    private static final long serialVersionUID = 2L;

    /**
     * Constructor for a new fault event object
     * @param timestamp the time of the fault
     * @param faultType the type of fault
     * @param droneID the id of the Drone associated with the fault
     * @param event the event associated with the fault
     */
    public FaultEvent(LocalTime timestamp, Type faultType, int droneID, Event event) {
        this.timestamp = timestamp;
        this.faultType = faultType;
        this.droneID = droneID;
        this.event = event;
    }

    // Getters

    /**
     * Gets the time stamp of the FaultEvent
     * @return the time stamp of the FaultEvent
     */
    public LocalTime getTimestamp() { return timestamp; }

    /**
     * Gets the type of the FaultEvent
     * @return the type of the FaultEvent
     */
    public Type getFaultType() { return faultType; }

    /**
     * Gets the id of the Drone associated with the FaultEvent
     * @return an int representing the Drone
     */
    public int getDroneID() { return droneID; }

    /**
     * Gets the Event associated with the FaultEvent
     * @return the Event associated with the FaultEvent
     */
    public Event getEvent() { return event; }

    /**
     * Creates a message that contains the command and serialized FaultEvent that will be sent using rpc
     * @param command the command selected by the user
     * @return a byte array containg the command and the serialized FaultEvent
     */
    public byte[] createMessage(String command){
        byte[] commandBytes = command.getBytes();
        byte[] serializedEvent = this.serializeFaultEvent();

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
     * Serializes this Event into a byte array
     * @return the byte array representing this Event
     */
    public byte[] serializeFaultEvent() {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(byteOut)) {
            out.writeObject(this);
            return byteOut.toByteArray();
        } catch (IOException e) {
            System.err.println("Serialization error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Deserializes a byte array into an Event
     * @param data the byte array which will be deserialized
     * @return the Event from the byte array
     */
    public static FaultEvent deserializeFaultEvent(byte[] data) {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
             ObjectInputStream in = new ObjectInputStream(byteIn)) {
            return (FaultEvent) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Deserialization error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns a string representation of this FaultEvent object
     * @return a string representation of the FaultEvent
     */
    @Override
    public String toString() {
        return String.format("[%s] Drone %d: %s", timestamp, droneID, faultType);
    }
}