
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

    public View(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.gbc = new GridBagConstraints();
        initializeUI();
    }

    private void initializeUI() {
        frame = new JFrame("Drone Control Panel");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addComponentListener(new ResizeListener());

        droneIcon = createScaledIcon("/assets/Drone.png", 30, 30);
        panel = createPanel(Color.white);
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
        JPanel statusPanel = createPanel(Color.red);
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return statusPanel;
    }

    private JTextArea createLogComponent() {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        return textArea;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Controls");
        JMenuItem start = new JMenuItem("Start");
        menu.add(start);
        menuBar.add(menu);
        return menuBar;
    }

    private JPanel createPanel(Color color) {
        JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(color);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return panel;
    }

    private void addComponentsToPanel(JPanel panel, JComponent map, JComponent log, JComponent statusBars) {
        JScrollPane logScrollPane = new JScrollPane(log);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        addComponent(panel, map, 0, 0, 1, 0.7, 2, 1, GridBagConstraints.BOTH);
        addComponent(panel, logScrollPane, 0, 1, 0.5, 0.3, 1, 1, GridBagConstraints.BOTH);
        addComponent(panel, statusBars, 1, 1, 0.25, 0.3, 1, 1, GridBagConstraints.BOTH);
    }

    private void addComponent(JPanel panel, JComponent comp, int gridX, int gridY, double weightX, double weightY, int gridWidth, int gridHeight, int fill) {
        gbc.gridx = gridX;
        gbc.gridy = gridY;
        gbc.weightx = weightX;
        gbc.weighty = weightY;
        gbc.gridwidth = gridWidth;
        gbc.gridheight = gridHeight;
        gbc.fill = fill;

        panel.add(comp, gbc);
    }

    private JPanel makeDroneTile(String droneName, String volume, Integer[] location) {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        tile.setBackground(new Color(240, 240, 240));
        tile.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel nameLabel = new JLabel(droneName);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String details = String.format("Vol: %s | Loc: (%d,%d)", volume, location[0], location[1]);
        JLabel detailsLabel = new JLabel(details);
        detailsLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        detailsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        tile.add(nameLabel);
        tile.add(Box.createRigidArea(new Dimension(0, 5)));
        tile.add(detailsLabel);

        return tile;
    }

    private void updateLogs() {
        while (!scheduler.logQueue.isEmpty()) {
            log.append(scheduler.logQueue.remove() + "\n");
            //log.setCaretPosition(log.getDocument().getLength());//scroll to bottom
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

        //repeat for events in the completed queue
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
                //Remove corresponding label from the map
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
        ImageIcon originalIcon = createScaledIcon("/assets/Drone.png", 30, 30);
        g2dBase.drawImage(originalIcon.getImage(), 0, 0, null);

        // Draw number (not rotated yet)
        g2dBase.setFont(new Font("SansSerif", Font.BOLD, 12));
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
        for (Component comp : statusBars.getComponents()) {
            if (comp instanceof JPanel) {
                try {
                    String name = ((JLabel) ((JPanel) comp).getComponent(0)).getText();
                    int droneNum = Integer.parseInt(name.replace("Drone ", ""));
                    existingDrones.add(droneNum);
                } catch (Exception e) {
                    // Handle potential parsing errors
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
        JPanel tile = findDroneTile(droneNumber);
        if (tile != null) {
            // Update the labels
            Component[] children = tile.getComponents();
            ((JLabel) children[0]).setText("Drone " + droneNumber);  // Name
            ((JLabel) children[2]).setText(
                    String.format("Vol: %s | Loc: (%d,%d)", newVolume, newLocation[0], newLocation[1])
            );
        }
    }

    private JPanel findDroneTile(int droneNumber) {
        for (Component comp : statusBars.getComponents()) {
            if (comp instanceof JPanel) {
                Component[] children = ((JPanel) comp).getComponents();
                if (children.length > 0 && children[0] instanceof JLabel) {
                    String labelText = ((JLabel) children[0]).getText();
                    if (labelText.equals("Drone " + droneNumber)) {
                        return (JPanel) comp;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void run() {
        while (!scheduler.finish) {
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
            //map.removeAll();
            //droneImages.clear();
            updateMap();
            updateDrones();
            updateLogs();
        }
    }

    public static void main(String[] args) {
        // Entry point for the application
        // You would typically initialize the Scheduler and start the View here
    }
}
