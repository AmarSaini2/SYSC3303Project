public class Main { 
    public static void main(String[] args) {
        Scheduler scheduler = new Scheduler();
        FireIncident fireIncident = new FireIncident("src\\Event_File.csv", "src\\Zone_File.csv", scheduler);
        //FireIncident fireIncident = new FireIncident("src\\Event_File.csv", "src\\Zone_File.csv", scheduler);
        Drone drone = new Drone(scheduler);
        scheduler.start();
        fireIncident.start();
        drone.start();
    }
}