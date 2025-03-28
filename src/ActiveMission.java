import java.time.Duration;
import java.time.LocalTime;
import java.io.Serializable;

public class ActiveMission implements Serializable {
    private static final long serialVersionUID = 3L;
    private LocalTime startTime;
    private Duration expectedDuration;
    private Event event;

    public ActiveMission(LocalTime startTime, Duration expectedDuration, Event event) {
        this.startTime = startTime;
        this.expectedDuration = expectedDuration;
        this.event = event;
    }

    public boolean isExpired(LocalTime currentTime) {
        return currentTime.isAfter(startTime.plus(expectedDuration));
    }

    public Event getEvent() { return event; }
}