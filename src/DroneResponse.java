// Updated code 

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class DroneResponse implements Serializable {
    public enum ResponseType {
        SUCCESS, FAILURE, REFILL_REQUIRED
    }

    private final Event event;
    private final int droneId;
    private final ResponseType responseType;

    /**
     * Constructor for DroneResponse.
     * 
     * @param event        The event that the drone responded to.
     * @param droneId      The ID of the drone sending the response.
     * @param responseType The type of response (SUCCESS, FAILURE, TIMEOUT,
     *                     REFILL_REQUIRED).
     */
    public DroneResponse(Event event, int droneId, ResponseType responseType) {
        this.event = event;
        this.droneId = droneId;
        this.responseType = responseType;
    }

    /**
     * Gets the event associated with this response.
     * 
     * @return The event.
     */
    public Event getEvent() {
        return event;
    }

    /**
     * Gets the ID of the drone that sent this response.
     * 
     * @return The drone ID.
     */
    public int getDroneId() {
        return droneId;
    }

    /**
     * Gets the type of response from the drone.
     * 
     * @return The response type.
     */
    public ResponseType getResponseType() {
        return responseType;
    }

    /**
     * Returns a string representation of the DroneResponse.
     * 
     * @return A formatted string.
     */
    @Override
    public String toString() {
        return "[DroneResponse] Drone ID: " + droneId + " | Event: " + event +
                " | Response: " + responseType;
    }

    public byte[] serializeResponse(){
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

    public static DroneResponse deserializeResponse(byte[] data){
        ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
        try{
            ObjectInputStream in = new ObjectInputStream(byteIn);
            return (DroneResponse) in.readObject();
        }catch(IOException e){
            e.printStackTrace();
        }catch( ClassNotFoundException f){
            f.printStackTrace();
        }
        return null;//return null for failure
    }
}
