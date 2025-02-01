public class Scheduler extends Thread{

    private Event event;
    private Event returnedEvent;

    public Scheduler(){
        this.event = null;
    }

    //Put: FireIncident puts an event
    public synchronized void newFireRequest(Event event){
        while (this.event != null) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }

        this.event = event;
        notifyAll();
    }

    //Get: Drone gets an event
    public synchronized Event requestForFire(){
        while (this.event == null) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }
        Event returnEvent = this.event;

        this.event = null;
        notifyAll();
        return returnEvent;

    }

    //Put: Drone puts the returnedEvent
    public synchronized void sendUpdate(Event event){
        while (this.returnedEvent != null) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }

        this.returnedEvent = event;
        notifyAll();
    }

    //Get: FireIncident gets the returnedEvent
    public synchronized Event receiveUpdates(){
        while (this.returnedEvent == null) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }

        Event returnEvent = this.event;
        this.returnedEvent = null;
        notifyAll();
        return returnEvent;
    }

}

