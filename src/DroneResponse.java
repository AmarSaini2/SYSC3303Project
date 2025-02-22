// Updated code 
public class DroneResponse {
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
}
