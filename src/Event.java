//Information wrapper class for fire events. Feel free to add other fields as needed
public class Event {
    public int time,zone,id;
    enum type {FIRE_DETECTED, DRONE_REQUEST};
    enum severity {HIGH, MODERATE, LOW};
}
