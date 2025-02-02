public class Zone {
    private int id;
    private int[] start;
    private int[] end;

    Zone(int zoneID, int startX, int startY, int endX, int endY){
        this.id = zoneID;
        this.start = new int[]{startX, startY};
        this.end = new int[]{endX, endY};
    }

    public int getId() {
        return id;
    }

    public int[] getStart() {
        return start;
    }

    public int[] getEnd() {
        return end;
    }
}