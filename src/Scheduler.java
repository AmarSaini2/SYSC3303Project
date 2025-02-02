public class Scheduler extends Thread{

    private Event event;
    private Event returnedEvent;
    private Boolean finish;

    /**
     * Constructor for Scheduler
     */
    public Scheduler(){
        this.event = null;
        this.returnedEvent = null;
        this.finish = false;
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
        System.out.println("\nScheduler: Put Event-> " +this.event.toString());
        notifyAll();
    }

    /**
     * Synchronized method run by Drone to get a GET an Event from Scheduler
     * @return the Event passed from the FireIncident
     */
    public synchronized Event requestForFire(){
        while (this.event == null) {
            try {
                if(this.finish){
                    return null;
                }
                wait();
            } catch (InterruptedException e) {}
        }
        Event returnEvent = this.event;
        this.event = null;

        System.out.println("Drone: Get Event-> " +returnEvent.toString());
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
        System.out.println("Drone: Put Returned Event-> " +this.returnedEvent.toString());
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

        System.out.println("FireIncidentSubsystem: Get Returned Event-> " +returnEvent.toString());
        notifyAll();
        return returnEvent;
    }


    /**
     * Called by FireIncident when there are no more events to set finish to true and to stop Drone
     */
    public synchronized void finishEvents(){
        this.finish = true;
        notifyAll();
    }


}

