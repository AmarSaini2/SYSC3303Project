import java.io.Serializable;

/**
 * The Zone class represents a geographical zone in which fire incidents may occur.
 * Each zone has an identifier and boundary coordinates.
 */
public class Zone implements Serializable {
    private int id;
    private int[] start; // Start coordinates (x, y)
    private int[] end; // End coordinates (x, y)
    private static final long serialVersionUID = 1L;//added for serialization compatibility

    /**
     * Constructs a Zone instance.
     *
     * @param zoneID The unique identifier of the zone.
     * @param startX The x-coordinate of the starting position.
     * @param startY The y-coordinate of the starting position.
     * @param endX The x-coordinate of the ending position.
     * @param endY The y-coordinate of the ending position.
     */
    Zone(int zoneID, int startX, int startY, int endX, int endY){
        this.id = zoneID;
        this.start = new int[]{startX, startY};
        this.end = new int[]{endX, endY};
    }

    /**
     * Retrieves the unique identifier of the zone.
     *
     * @return The zone ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Retrieves the start coordinates of the zone.
     *
     * @return An array containing the x and y coordinates of the starting position.
     */
    public int[] getStart() {
        return start;
    }

    /**
     * Retrieves the end coordinates of the zone.
     *
     * @return An array containing the x and y coordinates of the ending position.
     */
    public int[] getEnd() {
        return end;
    }
}