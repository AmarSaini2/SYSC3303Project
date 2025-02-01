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
        System.out.println("Put Event: " +this.event.toString());
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

        System.out.println("Get Event: " +returnEvent.toString());
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
        System.out.println("Put Returned Event: " +this.returnedEvent.toString());
        notifyAll();
    }

    //Get: FireIncident gets the returnedEvent
    public synchronized Event receiveUpdates(){
        while (this.returnedEvent == null) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }

        Event returnEvent = this.returnedEvent;
        this.returnedEvent = null;

        System.out.println("Get Returned Event: " +returnEvent.toString());
        notifyAll();
        return returnEvent;
    }

}

