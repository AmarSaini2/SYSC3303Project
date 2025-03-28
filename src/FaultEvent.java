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

    public FaultEvent(LocalTime timestamp, Type faultType, int droneID, Event event) {
        this.timestamp = timestamp;
        this.faultType = faultType;
        this.droneID = droneID;
        this.event = event;
    }

    // Getters
    public LocalTime getTimestamp() { return timestamp; }
    public Type getFaultType() { return faultType; }
    public int getDroneID() { return droneID; }
    public Event getEvent() { return event; }

    // Serialization method
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

    // Deserialization method
    public static FaultEvent deserializeFaultEvent(byte[] data) {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
             ObjectInputStream in = new ObjectInputStream(byteIn)) {
            return (FaultEvent) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Deserialization error: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("[%s] Drone %d: %s", timestamp, droneID, faultType);
    }
}