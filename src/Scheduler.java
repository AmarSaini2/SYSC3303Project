public class Scheduler extends Thread{

    private Event event;
    private Event returnedEvent;

    /**
     * Constructor for Scheduler
     */
    public Scheduler(){
        this.event = null;
    }

    /**
     * Synchronized method run by FireIncident to PUT a new Event to Scheduler
     * @param event the Event being passed
     */
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

    /**
     * Synchronized method run by Drone to get a GET an Event from Scheduler
     * @return the Event passed from the FireIncident
     */
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

    /**
     * Synchronized method run by Drone to PUT a completed Event to Scheduler
     * @param event the Event that was put out
     */
    public synchronized void sendUpdate(Event event){
        while (this.returnedEvent != null) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }

        this.returnedEvent = event;
        System.out.println("Put Completed Event: " +this.returnedEvent.toString());
        notifyAll();
    }

    /**
     * Synchronized method run by FireIncident to GET a completed Event from Scheduler
     * @return the Event that was put out
     */
    public synchronized Event receiveUpdates(){
        while (this.returnedEvent == null) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }

        Event returnEvent = this.returnedEvent;
        this.returnedEvent = null;

        System.out.println("Get Completed Event: " +returnEvent.toString());
        notifyAll();
        return returnEvent;
    }

    
}

