import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
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

    Set<Integer> existingDrones = new HashSet<>();
    HashMap<Integer, Object[]> zoneMap;
    HashMap<Integer, JLabel> droneImages = new HashMap<>();
    ImageIcon droneIcon;
    HashMap<Integer, Double> droneAngles = new HashMap<>();


    View(Scheduler s){
        this.scheduler = s;
        this.gbc = new GridBagConstraints();
        this.frame = new JFrame("GUI NAME HERE");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Recalculate all zone positions on resize
                for (Object[] zoneData : zoneMap.values()) {
                    zoneData[2] = false; // Mark all zones for re-rendering
                }
                map.removeAll();
                droneImages.clear();
                updateMap();
            }
        });

        ImageIcon originalIcon = new ImageIcon(getClass().getResource("/assets/Drone.png"));
        Image scaledImage = originalIcon.getImage().getScaledInstance(30,30,Image.SCALE_SMOOTH);
        this.droneIcon = new ImageIcon(scaledImage);
        
        this.panel = makePanel(Color.white);
        panel.setLayout(new GridBagLayout());

        this.menuBar = new JMenuBar();
        this.menu = new JMenu("Controls");
        this.start = new JMenuItem("Start");
        menu.add(start);
        menuBar.add(menu);
        frame.setJMenuBar(menuBar);

        this.zoneMap = new HashMap();
        this.map = makePanel(Color.white);
        map.setLayout(null);
        map.setSize(1200,750);
        map.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        this.statusBars = makePanel(Color.red);
        statusBars.setLayout(new BoxLayout(statusBars, BoxLayout.Y_AXIS));
        statusBars.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        JScrollPane statusBarScroll = new JScrollPane(statusBars);
        statusBarScroll.setPreferredSize(new Dimension(600,150));
        statusBarScroll.setBorder(BorderFactory.createTitledBorder("Drone Status"));

        this.log = makeLogComp();
        JScrollPane logScroll = new JScrollPane(log);
        logScroll.setPreferredSize(new Dimension(600,150));
        logScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        logScroll.setBorder(BorderFactory.createTitledBorder("Event Log"));
        
        addComp(panel, map, 0, 0, 1, 0.7, 2, 1, GridBagConstraints.BOTH);
        addComp(panel, logScroll, 0, 1, 0.5, 0.3, 1, 1, GridBagConstraints.BOTH);
        addComp(panel, statusBarScroll, 1, 1, 0.25, 0.3, 1, 1, GridBagConstraints.BOTH);

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

    private void updateMap() {

        // Calculate scaling factors based on the first zone (reference point)
        if (zoneMap.isEmpty() && !scheduler.eventQueue.isEmpty()) {
            // Initialize with first zone's coordinates
            Zone firstZone = scheduler.eventQueue.peek().getZone();
            int[] start = firstZone.getStart();
            int[] end = firstZone.getEnd();
            zoneMap.put(firstZone.getId(), new Object[]{start, end, false});
        }
    
        // Add new zones while maintaining relative positioning
        for (Event event : scheduler.eventQueue) {
            Zone zone = event.getZone();
            zoneMap.computeIfAbsent(zone.getId(), k -> {
                // For new zones, store coordinates and "not rendered" status
                return new Object[]{zone.getStart(), zone.getEnd(), false};
            });
        }
    
        // Find the most extreme coordinates to determine scaling
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        
        for (Object[] zoneData : zoneMap.values()) {
            int[] start = (int[]) zoneData[0];
            int[] end = (int[]) zoneData[1];
            
            minX = Math.min(minX, start[0]);
            maxX = Math.max(maxX, end[0]);
            minY = Math.min(minY, start[1]);
            maxY = Math.max(maxY, end[1]);
        }
    
        // Calculate scaling factors with 10% padding
        double widthPadding = (maxX - minX) * 0.1;
        double heightPadding = (maxY - minY) * 0.1;
        double scaleX = map.getWidth() / (maxX - minX + widthPadding);
        double scaleY = map.getHeight() / (maxY - minY + heightPadding);
        double scale = Math.min(scaleX, scaleY);
    
        // Add only unrendered zones to the map
        for (Integer zoneId : zoneMap.keySet()) {
            Object[] zoneData = zoneMap.get(zoneId);
            boolean rendered = (boolean) zoneData[2];
            
            if (!rendered) {
                int[] start = (int[]) zoneData[0];
                int[] end = (int[]) zoneData[1];
                
                // Calculate scaled positions and dimensions
                int x = (int) ((start[0] - minX + widthPadding/2) * scale);
                int y = (int) ((start[1] - minY + heightPadding/2) * scale);
                int width = (int) ((end[0] - start[0]) * scale);
                int height = (int) ((end[1] - start[1]) * scale);
    
                ZoneLabel zl = new ZoneLabel(zoneId, new Color(200, 50, 50, 150), 0.5f);
                zl.setBounds(x, y, width, height);
                map.add(zl);
                
                // Mark as rendered
                zoneData[2] = true;
            }
        }


        //Update drone positions
        for(int droneNum : scheduler.allDroneList.keySet()){
            Integer[] coords = (Integer[]) scheduler.allDroneList.get(droneNum).get("location");

            //calculate scaled position
            int x = (int) ((coords[0] - minX + widthPadding/2) * scale) - 15;
            int y = (int) ((coords[0] - minY + heightPadding/2) * scale) - 15;

            //calculate angle
            Double angle = droneAngles.get(droneNum);
            if(angle != null){
                Image rotatedImage = rotateImage(((ImageIcon)droneIcon).getImage(), angle);
                droneImages.get(droneNum).setIcon(new ImageIcon(rotatedImage));
            }

            if(!droneImages.containsKey(droneNum)){
                //create new drone image label
                JLabel droneLabel = new JLabel(droneIcon);
                droneLabel.setBounds(x,y, 30, 30);
                droneLabel.setOpaque(false);
                map.add(droneLabel);
                map.add(droneLabel);
                droneImages.put(droneNum, droneLabel);
                map.setComponentZOrder(droneLabel, 0);//bring to the front
            }else{
                //update existing drone position
                JLabel droneLabel = droneImages.get(droneNum);
                droneLabel.setLocation(x,y);
            }
        }

        //remove any drones that are no longer active
        Set<Integer> activeDrones = new HashSet<>(scheduler.allDroneList.keySet());
        droneImages.keySet().removeIf(droneNum -> !activeDrones.contains(droneNum));
    
        map.revalidate();
        map.repaint();
    }

    private Image rotateImage(Image image, double angle){
        BufferedImage buffered = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = buffered.createGraphics();
        g2d.rotate(Math.toRadians(angle), image.getWidth(null)/2, image.getHeight(null)/2);
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return buffered;
    }

    private void updateDrones(){
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
        }
        
        //System.exit(0);
    }

    public static void main(String[] args) {
    }

}