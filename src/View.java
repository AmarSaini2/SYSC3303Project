import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;
import javax.swing.text.DefaultCaret;

public class View extends Thread {
    Scheduler scheduler;
    JFrame frame;
    JPanel panel, map, statusBars;
    JTextArea log;
    JMenu menu;
    JMenuBar menuBar;
    JMenuItem start;

    GridBagConstraints gbc;

    View(Scheduler s){
        this.scheduler = s;
        this.gbc = new GridBagConstraints();
        this.frame = new JFrame("GUI NAME HERE");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        this.panel = makePanel(Color.gray);
        panel.setLayout(new GridBagLayout());

        this.menuBar = new JMenuBar();
        this.menu = new JMenu("Controls");
        this.start = new JMenuItem("Start");
        menu.add(start);
        menuBar.add(menu);
        frame.setJMenuBar(menuBar);


        this.map = makePanel(Color.blue);
        map.setSize(1200,450);
        this.statusBars = makePanel(Color.red);
        statusBars.setLayout(new BoxLayout(statusBars, BoxLayout.Y_AXIS));
        statusBars.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        JScrollPane statusBarScroll = new JScrollPane(statusBars);
        statusBarScroll.setPreferredSize(new Dimension(600,450));

        this.log = makeLogComp();
        JScrollPane logScroll = new JScrollPane(log);
        logScroll.setPreferredSize(new Dimension(600,450));
        logScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        addComp(panel, map, 0, 0, 1, 0.45, 2, 1, GridBagConstraints.BOTH);
        addComp(panel, statusBars, 1, 1, 0.5, 0.45, 1, 1, GridBagConstraints.BOTH);
        addComp(panel, logScroll, 0, 1, 0.5, 0.45, 1, 1, GridBagConstraints.BOTH);
        addComp(panel, statusBarScroll, 1, 1, 0, 1, 1, 1, GridBagConstraints.NORTH);

        frame.setContentPane(panel);
        frame.setSize(1200,900);
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    private JTextArea makeLogComp(){
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        DefaultCaret caret = (DefaultCaret)textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        
        return textArea;
    }


    private JPanel makePanel(Color c){
        JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(c);
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        return panel;
    }

    private void addComp(JPanel panel, JComponent comp, int gridX, int gridY, double weightX, double weightY, int gridWidth, int gridHeight, int fill){
        gbc.gridx = gridX;
        gbc.gridy = gridY;
        gbc.weightx = weightX;
        gbc.weighty = weightY;
        gbc.gridwidth = gridWidth;
        gbc.gridheight = gridHeight;
        gbc.fill = fill;

        panel.add(comp, gbc);
    }

    private JPanel makeDroneTile(String droneName, String volume, Integer[] location){
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
            BorderFactory.createEmptyBorder(10,10,10,10)));
        tile.setBackground(new Color(240,240,240));
        tile.setMaximumSize(new Dimension(Integer.MAX_VALUE,80));

        JLabel nameLabel = new JLabel(droneName);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String details = String.format("Vol: %s | Loc: (%d,%d)", volume, location[0], location[1]);
        JLabel detailsLabel = new JLabel(details);
        detailsLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        detailsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        tile.add(nameLabel);
        tile.add(Box.createRigidArea(new Dimension(0,5)));
        tile.add(detailsLabel);

        return tile;
    }

    private void updateLogs(){
        while(!scheduler.logQueue.isEmpty()){
            log.append(scheduler.logQueue.remove() + "\n");
        }
    }

    private void updateMap(){

    }

    private void updateDrones(){
        Set<Integer> existingDrones = new HashSet<>();
        for(Component comp : statusBars.getComponents()){
            if(comp instanceof JPanel){
                try {
                    String name = ((JLabel)((JPanel)comp).getComponent(0)).getText();
                    int droneNum = Integer.parseInt(name.replace("Drone ", ""));
                    existingDrones.add(droneNum);
                } catch (Exception e) {}
                }
            }
        

        for(int droneNum: scheduler.allDroneList.keySet()){
            String volume = scheduler.allDroneList.get(droneNum).get("volume").toString();
            Integer[] coords = (Integer[]) scheduler.allDroneList.get(droneNum).get("location");

            if(existingDrones.contains(droneNum)){
                updateDroneTile(droneNum, volume, coords);
            }else{
                statusBars.add(makeDroneTile("Drone " + droneNum,volume,coords));
                statusBars.add(Box.createRigidArea(new Dimension(0,5)));
            }

            
        }

        statusBars.revalidate();
        statusBars.repaint();
    }

    private void updateDroneTile(int droneNumber, String newVolume, Integer[] newLocation) {
        JPanel tile = findDroneTile(droneNumber);
        if (tile != null) {
            // Update the labels
            Component[] children = tile.getComponents();
            ((JLabel)children[0]).setText("Drone " + droneNumber);  // Name
            ((JLabel)children[2]).setText(
                String.format("Vol: %s | Loc: (%d,%d)", newVolume, newLocation[0], newLocation[1])
            );
        }
    }

    private JPanel findDroneTile(int droneNumber) {
        for (Component comp : statusBars.getComponents()) {
            if (comp instanceof JPanel) {
                Component[] children = ((JPanel)comp).getComponents();
                if (children.length > 0 && children[0] instanceof JLabel) {
                    String labelText = ((JLabel)children[0]).getText();
                    if (labelText.equals("Drone " + droneNumber)) {
                        return (JPanel)comp;
                    }
                }
            }
        }
        return null;
    }


    @Override
    public void run(){
        while(!scheduler.finish){

            updateLogs();
            updateMap();
            updateDrones();

            /*
            pending and completed events (need to only update new values)
            this.log.append("\nOngoing Events:\n");
            for(Event e: scheduler.eventQueue){
                this.log.append(e + "\n");
            }

            this.log.append("Completed Events:\n");
            for(Entry<Integer, Event> entry : scheduler.fullyServicedEvents.entrySet()){
                this.log.append(entry.toString() + "\n");
            }

            this.log.append("\n------------------------------------\n");

            */
            /*Drone data (volume is not being passed from drone to scheduler)
            for(int droneNum : scheduler.allDroneList.keySet()){
                String volume = scheduler.allDroneList.get(droneNum).get("volume").toString();
                Integer[] coords = (Integer[]) scheduler.allDroneList.get(droneNum).get("location");
                this.log.append("Drone: " + droneNum + " -> Volume: " + volume);
                this.log.append(  ", Location: " + coords[0] + "," + coords[1] + "\n");
            }
            */
            try {
                sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.log.append("Done\n");
        
        //System.exit(0);
    }

    public static void main(String[] args) {
    }

}