public class Main {
    public static void main(String[] args) {
        Scheduler scheduler = new Scheduler(5000, 6000);
        scheduler.start();

        FireIncident fireIncident = new FireIncident("src/Event_File.csv", "src/Zone_File.csv", 5000);
        fireIncident.start();



        Drone drone = new Drone(6000, "src/droneFaultInjection_0.txt");
        drone.start();
        Drone drone1 = new Drone(6000);
        drone1.start();
        Drone drone2 = new Drone(6000);
        drone2.start();
    }
}