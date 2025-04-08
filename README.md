
# Firefighting Drone Swarm

## Project Overview
This project focuses on developing a firefighting drone swarm control system that can detect and respond to fire incidents in different zones. It involves simulating drones that receive fire alerts, travel to affected areas, extinguish fires using water or foam, and handle any faults that may occur effectively. The system is designed to coordinate multiple drones efficiently using a central scheduler, ensuring timely responses to fire emergencies.

There are three main components of project:
1. **Scheduler** (central controller)
2. **Drone Subsystem** (drones responding to fires)
3. **Fire Incident Subsystem** (simulating fire events and requests)

## File Descriptions

- **Main.java**: Entry point for the system, initializes and starts all subsystems.
- **Drone.java**: Simulates a drone responding to fire incidents. Handles travel, water/foam release, and communication with the scheduler.
- **DroneFSM.java**: Manages the state of the drone.
- **DroneTest.java**: Unit tests for the `Drone` class to verify correct behavior.
- **Event.java**: Represents a fire event, storing details like severity, time, and location.
- **FaultEvent.java**: Represents an event during which a drone fault occurred.
- **FireIncident.java**: Simulates fire incidents, sending fire requests to the scheduler, and reading input files.
- **FireIncidentTest.java**: Unit tests for `FireIncident` to validate event handling.
- **Scheduler.java**: Central component responsible for receiving fire reports, assigning drones, and managing event queues.
- **SchedulerFSM.java**: Manages the state of the scheduler.
- **SchedulerTest.java**: Unit tests for the `Scheduler` class.
- **Zone.java**: Defines geographical fire zones.
- **Event_File.csv**: Sample input file containing fire event data.
- **Zone_File.csv**: Sample input file defining fire zones.

## Setup Instructions

### Prerequisites
- **Java 17+** (JDK installed)
- **JUnit 5** (for running test cases)
- **IntelliJ IDEA** (or another Java IDE)

### Steps to Run the Project

1. **Clone or Download the Project**
    ```
    git clone https://github.com/AmarSaini2/SYSC3303Project.git
    cd SYSC3303Project
    ```

2. **Open in IntelliJ IDEA**
- Select **File → Open** and choose the project directory.

3. **Compile the Code**
- Open the **Terminal** in IntelliJ and run:
  ```
  javac *.java
  ```

4. **Run the Program**
- Execute the **Main** class:
  ```
  java Main
  ```
- This starts the **Scheduler**, **Fire Incident**, and **Drone** threads.

5. **Run Unit Tests**
- Execute the test suite using **JUnit**:
  ```
  java <test_file.java>
  ```
- Alternatively, run tests from the **JUnit tab** in IntelliJ.

## Expected Behavior

1. **FireIncident** reads fire events from `Event_File.csv` and sends them to **Scheduler**.
2. **Scheduler** receives fire reports and forwards them to **Drone** Drones.
3. Drones pick up fire assignments and simulate firefighting.
4. **Scheduler** updates event status and confirms fire extinguishment, if a drone fault occurs, it stops the drone and re-queues the event.
5. The system communicates via UDP

## Team Members

- Amar, Saini
- John, Guo
- Kaitlyn, Conron
- Samuel, Lo
- Yacine, Djaou
