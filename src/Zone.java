public class Zone {
    private int id;
    private int start;
    private int end;

    Zone(int zoneID, int zoneStart, int zoneEnd){
        this.id = zoneID;
        this.start = zoneStart;
        this.end = zoneEnd;
    }

    public int getId() {
        return id;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}