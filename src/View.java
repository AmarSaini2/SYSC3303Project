
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.*;
import javax.swing.text.DefaultCaret;

public class View extends Thread {

    private Scheduler scheduler;
    private JFrame frame;
    private JPanel panel, map, statusBars;
    private JTextArea log;
    private JMenuBar menuBar;
    private ImageIcon droneIcon;

    private GridBagConstraints gbc;
    private Set<Integer> existingDrones = new HashSet<>();
    private HashMap<Integer, Object[]> zoneMap = new HashMap<>();
    private HashMap<Integer, JLabel> droneImages = new HashMap<>();
    private HashMap<Integer, Double> droneAngles = new HashMap<>();
    private HashMap<Integer, Integer[]> lastKnownLocation = new HashMap<>();
    private final ReentrantLock logQueueLock = new ReentrantLock();

    public View(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.gbc = new GridBagConstraints();
        initializeUI();
    }

    private void initializeUI() {
        frame = new JFrame("Drone Control Panel");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addComponentListener(new ResizeListener());

        droneIcon = createScaledIcon("/assets/defaultDrone.png", 30, 30);
        panel = createPanel(new Color(240, 240, 240)); // Light gray background
        panel.setLayout(new GridBagLayout());

        menuBar = createMenuBar();
        frame.setJMenuBar(menuBar);

        map = createMapPanel();
        statusBars = createStatusBarsPanel();
        log = createLogComponent();

        addComponentsToPanel(panel, map, log, statusBars);
        frame.setContentPane(panel);
        frame.setSize(1200, 900);
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    private ImageIcon createScaledIcon(String path, int width, int height) {
        ImageIcon originalIcon = new ImageIcon(getClass().getResource(path));
        Image scaledImage = originalIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }

    private JPanel createMapPanel() {
        JPanel mapPanel = createPanel(Color.white);
        mapPanel.setLayout(null);
        mapPanel.setSize(1200, 450);
        mapPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        return mapPanel;
    }

    private JPanel createStatusBarsPanel() {
        JPanel statusPanel = createPanel(new Color(255, 255, 255)); // White background
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.setPreferredSize(new Dimension(300, 250));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return statusPanel;
    }

    private JTextArea createLogComponent() {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font("Arial", Font.PLAIN, 12)); // Modern font
        textArea.setBackground(new Color(240, 240, 240)); // Light gray background
        textArea.setForeground(Color.BLACK); // Black text
        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        return textArea;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JRadioButton cuteMode = new JRadioButton("cute mode");
        cuteMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Toggle cute mode on/off based on button state
                if (cuteMode.isSelected()) {
                    enableCuteMode();
                } else {
                    disableCuteMode();
                }
            }
        });
        menuBar.add(cuteMode);
        return menuBar;
    }

    private void enableCuteMode() {
        //drone images change
        //background images change
        droneIcon = createScaledIcon("/assets/altDrone0.png", 30, 30);
    }
    
    private void disableCuteMode() {
        // Restore default visual settings here
        // Example:
        // droneIcon = createScaledIcon("/assets/Drone.png", 30, 30);
        // panel.setBackground(new Color(240, 240, 240)); // Original light gray
        droneIcon = createScaledIcon("/assets/defaultDrone.png", 30, 30);
    }

    private JPanel createPanel(Color color) {
        JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(color);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return panel;
    }

    private void addComponentsToPanel(JPanel panel, JComponent map, JComponent log, JComponent statusBars) {
        // Create bottom panel
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2));
        bottomPanel.setBackground(new Color(240, 240, 240)); // Light gray background

        JScrollPane logScrollPane = new JScrollPane(log);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        logScrollPane.setPreferredSize(new Dimension(600, 250));
        logScrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1)); // Add a border to the log scroll pane
        bottomPanel.add(logScrollPane);
        bottomPanel.add(statusBars);

        // Create split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, map, bottomPanel);
        splitPane.setResizeWeight(0.7); // 70% of space goes to map
        splitPane.setDividerSize(5);
        splitPane.setBorder(BorderFactory.createEmptyBorder()); // Remove default border

        panel.add(splitPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    }

    private JPanel makeDroneTile(String droneName, String volume, Integer[] location) {
        //make info panel (right side)
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        infoPanel.setBackground(new Color(255, 255, 255)); // White background for tiles
        infoPanel.setMaximumSize(new Dimension(300, 100)); // Increased height to accommodate new text

        // Add info components
        JLabel nameLabel = new JLabel(droneName);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14)); // Modern font
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String details = String.format("Vol: %s | Loc: (%d,%d)", volume, location[0], location[1]);
        JLabel detailsLabel = new JLabel(details);
        detailsLabel.setFont(new Font("Arial", Font.PLAIN, 12)); // Modern font
        detailsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel stateText = new JLabel("Online");
        stateText.setFont(new Font("Arial", Font.PLAIN, 12)); // Modern font
        stateText.setAlignmentX(Component.CENTER_ALIGNMENT);

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Vertical spacing
        infoPanel.add(detailsLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Vertical spacing
        infoPanel.add(stateText);

        // Create the main panel with icon on left and info on right
        JPanel dronePanel = new JPanel();
        dronePanel.setLayout(new BoxLayout(dronePanel, BoxLayout.X_AXIS));
        dronePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.black, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        // Add drone icon (left side)
        int droneId = Integer.parseInt(droneName.substring(droneName.indexOf(" ") + 1));
        JLabel icon = new JLabel(droneImages.get(droneId).getIcon());
        icon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10)); // Right margin for the icon

        dronePanel.add(icon);
        dronePanel.add(infoPanel);

        return dronePanel;
    }

    private void updateLogs() {
        logQueueLock.lock();
        try {
            while (!scheduler.logQueue.isEmpty()) {
                String currentLog = scheduler.logQueue.remove();
                String[] splitLog = currentLog.split(":");

                // Check for specific log messages and update drone states accordingly
                if (currentLog.contains("LOCATION")) {
                    continue; // Skip location logs
                }

                // Append the log to the JTextArea
                log.append(currentLog + "\n");
                log.setCaretPosition(log.getDocument().getLength());
            }
        } finally {
            logQueueLock.unlock();
        }
    }

    private void updateMap() {
        // Calculate scaling factors based on the first zone (reference point)
        if (zoneMap.isEmpty() && !scheduler.eventQueue.isEmpty()) {
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

        // Repeat for events in the completed queue
        for (Event event : scheduler.fullyServicedEvents.values()) {
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
            ZoneLabel zl = getZoneLabel(zoneId);
            Object[] zoneData = zoneMap.get(zoneId);
            boolean rendered = (boolean) zoneData[2];

            Event.Severity severity = Event.Severity.OUT;
            for (Integer eventId : scheduler.allEvents.keySet()) {
                if (scheduler.allEvents.get(eventId).getZone().getId() == zoneId) {
                    severity = scheduler.allEvents.get(eventId).getSeverity();
                }
            }
            Color color = new Color(0, 100, 0, 150);
            switch (severity) {
                case HIGH:
                    color = new Color(255, 0, 0, 150);
                    break;
                case MODERATE:
                    color = new Color(255, 76, 0, 150);
                    break;
                case LOW:
                    color = new Color(255, 220, 0, 150);
                    break;
            }
            zl.setZoneColor(color);

            if (!rendered) {
                int[] start = (int[]) zoneData[0];
                int[] end = (int[]) zoneData[1];

                // Calculate scaled positions and dimensions
                int x = (int) ((start[0] - minX + widthPadding / 2) * scale);
                int y = (int) ((start[1] - minY + heightPadding / 2) * scale);
                int width = (int) ((end[0] - start[0]) * scale);
                int height = (int) ((end[1] - start[1]) * scale);

                zl.setBounds(x, y, width, height);
                map.add(zl);

                // Mark as rendered
                zoneData[2] = true;
            }
        }

        // Update drone positions
        for (int droneNum : scheduler.allDroneList.keySet()) {
            Object coordsObj = scheduler.allDroneList.get(droneNum).get("location");
            Integer[] coords = (Integer[]) coordsObj;

            // Get last known coordinates
            Integer[] lastCoords = lastKnownLocation.get(droneNum);
            if (lastCoords == null) {
                lastCoords = coords;
                lastKnownLocation.put(droneNum, lastCoords);
            }

            // Calculate angle based on movement vector
            double dx = coords[0] - lastCoords[0];
            double dy = coords[1] - lastCoords[1];

            // Only update angle if drone has actually moved
            if (Math.abs(dx) > 0.5 || Math.abs(dy) > 0.5) {
                double newAngle = Math.toDegrees(Math.atan2(dy, dx));
                if (newAngle < 0) {
                    newAngle += 360;
                }
                droneAngles.put(droneNum, newAngle);
                lastKnownLocation.put(droneNum, coords);
            }

            // Calculate scaled position
            int x = (int) ((coords[0] - minX + widthPadding / 2) * scale) - 15;
            int y = (int) ((coords[1] - minY + heightPadding / 2) * scale) - 15;

            if (!droneImages.containsKey(droneNum)) {
                // Create new drone image label
                JLabel droneLabel = new JLabel();
                droneLabel.setBounds(x, y, 30, 30);
                droneLabel.setOpaque(false);
                droneImages.put(droneNum, droneLabel);
                map.add(droneLabel);
                map.setComponentZOrder(droneLabel, 0);
            } else {
                // Update existing drone position
                JLabel droneLabel = droneImages.get(droneNum);
                droneLabel.setLocation(x, y);
            }

            // Always update the icon with current angle
            double currentAngle = droneAngles.getOrDefault(droneNum, 0.0);
            droneImages.get(droneNum).setIcon(createRotatedDroneImageWithNumber(droneNum, currentAngle));
        }

        // Remove any drones that are no longer active
        Set<Integer> activeDrones = new HashSet<>(scheduler.allDroneList.keySet());
        droneImages.keySet().removeIf(droneNum -> {
            if (!activeDrones.contains(droneNum)) {
                // Remove corresponding label from the map
                JLabel droneLabel = droneImages.get(droneNum);
                map.remove(droneLabel);
                return true;
            }
            return false;
        });

        map.revalidate();
        map.repaint();
    }

    private ZoneLabel getZoneLabel(int zoneId) {
        for (Component component : map.getComponents()) {
            if (component instanceof ZoneLabel) {
                ZoneLabel zoneLabel = (ZoneLabel) component;
                if (zoneLabel.getName().equals(String.valueOf(zoneId))) {
                    return zoneLabel;
                }
            }
        }
        return new ZoneLabel(zoneId, new Color(200, 50, 50, 150), 0.5f);
    }

    private ImageIcon createRotatedDroneImageWithNumber(int droneNum, double angle) {
        // Create base image (unrotated)
        BufferedImage baseImage = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dBase = baseImage.createGraphics();

        // Draw drone icon (centered)
        g2dBase.drawImage(droneIcon.getImage(), 0, 0, null);

        // Draw number (not rotated yet)
        g2dBase.setFont(new Font("Arial", Font.BOLD, 12)); // Modern font
        g2dBase.setColor(Color.BLACK);
        String droneNumberStr = String.valueOf(droneNum);
        FontMetrics fm = g2dBase.getFontMetrics();
        int textWidth = fm.stringWidth(droneNumberStr);
        int textHeight = fm.getAscent();
        int x = (30 - textWidth) / 2;
        int y = (30 + textHeight) / 2 - 2;
        g2dBase.drawString(droneNumberStr, x, y);
        g2dBase.dispose();

        // Now rotate the complete image
        BufferedImage rotatedImage = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dRotated = rotatedImage.createGraphics();

        // Set rendering hints for better quality
        g2dRotated.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2dRotated.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Rotate around center
        g2dRotated.rotate(Math.toRadians(angle + 90), 15, 15);
        g2dRotated.drawImage(baseImage, 0, 0, null);
        g2dRotated.dispose();

        return new ImageIcon(rotatedImage);
    }

    private void updateDrones() {
        existingDrones.clear();
        for (Component comp : statusBars.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel dronePanel = (JPanel) comp;
                // Check if this panel has our expected structure (icon + info panel)
                if (dronePanel.getComponentCount() == 2 && 
                    dronePanel.getComponent(1) instanceof JPanel) {
                    
                    try {
                        JPanel infoPanel = (JPanel) dronePanel.getComponent(1);
                        // The name label is the first component in the info panel
                        String name = ((JLabel) infoPanel.getComponent(0)).getText();
                        int droneNum = Integer.parseInt(name.replace("Drone ", ""));
                        existingDrones.add(droneNum);

                        JLabel icon = (JLabel) dronePanel.getComponent(0);
                        icon.setIcon(droneImages.get(droneNum).getIcon());
                    } catch (Exception e) {
                        // Handle potential parsing errors (malformed drone name or component structure)
                        System.err.println("Error parsing drone panel: " + e.getMessage());
                        // You might want to log this error or handle it differently
                    }
                }
            }
        }

        for (int droneNum : scheduler.allDroneList.keySet()) {
            String volume = scheduler.allDroneList.get(droneNum).get("volume").toString();
            Integer[] coords = (Integer[]) scheduler.allDroneList.get(droneNum).get("location");

            if (existingDrones.contains(droneNum)) {
                updateDroneTile(droneNum, volume, coords);
            } else {
                statusBars.add(makeDroneTile("Drone " + droneNum, volume, coords));
                statusBars.add(Box.createRigidArea(new Dimension(0, 5)));
            }
        }

        statusBars.revalidate();
        statusBars.repaint();
    }

    private void updateDroneTile(int droneNumber, String newVolume, Integer[] newLocation) {
        JPanel dronePanel = findDroneTile(droneNumber);
        if (dronePanel != null) {
            // The info panel is the second component (index 1) of the dronePanel
            JPanel infoPanel = (JPanel) dronePanel.getComponent(1);
            
            // Get all components from the info panel (which has the Y_AXIS layout)
            Component[] infoComponents = infoPanel.getComponents();
            
            // Update the labels - note these are now at indices 0, 2, 4 in the infoPanel
            ((JLabel) infoComponents[0]).setText("Drone " + droneNumber);  // Name
            ((JLabel) infoComponents[2]).setText(
                    String.format("Vol: %s | Loc: (%d,%d)", newVolume, newLocation[0], newLocation[1])
            );
            ((JLabel) infoComponents[4]).setText(scheduler.allDroneList.get(droneNumber).get("state").toString());
        }
    }
    
    private JPanel findDroneTile(int droneNumber) {
        for (Component comp : statusBars.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel dronePanel = (JPanel) comp;
                // Check if this panel has our expected structure (icon + info panel)
                if (dronePanel.getComponentCount() == 2 && 
                    dronePanel.getComponent(1) instanceof JPanel) {
                    
                    JPanel infoPanel = (JPanel) dronePanel.getComponent(1);
                    Component[] infoComponents = infoPanel.getComponents();
                    
                    // Check if the first component of infoPanel is our drone name label
                    if (infoComponents.length > 0 && infoComponents[0] instanceof JLabel) {
                        String labelText = ((JLabel) infoComponents[0]).getText();
                        if (labelText.equals("Drone " + droneNumber)) {
                            return dronePanel;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void run() {
        while (!scheduler.fireIncidentFinish || !scheduler.droneFinish) {
            updateLogs();
            updateMap();
            updateDrones();
        }
    }

    private class ResizeListener extends ComponentAdapter {

        @Override
        public void componentResized(ComponentEvent e) {
            // Recalculate all zone positions on resize
            for (Object[] zoneData : zoneMap.values()) {
                zoneData[2] = false; // Mark all zones for re-rendering
            }
            //Commented out since it was adding to many extra drones to the states bar
//            updateMap();
//            updateDrones();
//            updateLogs();
        }
    }

    public static void main(String[] args) {
        // Entry point for the application
        // You would typically initialize the Scheduler and start the View here

    }
}
