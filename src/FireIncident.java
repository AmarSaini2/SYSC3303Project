
import java.util.HashMap;



public class FireIncident {
    private int time,zone,id;
    public static enum type {FIRE_DETECTED, DRONE_REQUEST};
    private type fireType;
    public static enum severity {HIGH, MODERATE, LOW, OUT};
    private severity fireSeverity;

    private HashMap attributes;


    FireIncident(int eventTime, int eventZone, int eventId, type eventType, severity eventSeverity){
        this.time = eventTime;
        this.zone = eventZone;
        this.id = eventId;
        this.fireType = eventType;
        this.fireSeverity = eventSeverity;

        this.attributes = new HashMap<String, String>();
        attributes.put("time", this.time);
        attributes.put("zone", this.zone);
        attributes.put("id", this.id);
        attributes.put("type", this.fireType);
        attributes.put("severity", this.fireSeverity);
    }

    public synchronized void put (HashMap<String, String> data){//replace the value of a valid key
        for(String key: data.keySet()){
            if(attributes.get(key) != null){
                attributes.replace(key, data.get(key));
            }
        }
    }

    public synchronized String get(String key){//return the value of a given key
        return attributes.get(key).toString();
    }

    public static void main(String[] args){
        FireIncident test = new FireIncident(0, 0, 0, type.FIRE_DETECTED, severity.HIGH);
        System.out.println(test.get("severity"));
        HashMap data = new HashMap<String, String>();
        data.put("severity", severity.LOW);
        test.put(data);
        System.out.println(test.get("severity"));
    }
}
