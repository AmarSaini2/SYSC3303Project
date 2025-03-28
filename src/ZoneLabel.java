import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

public class ZoneLabel extends JPanel {
    private final int zoneNumber;
    private final Color zoneColor;
    private final float transparency;
    
    public ZoneLabel(int zoneNumber, Color zoneColor, float transparency){
        this.zoneNumber = zoneNumber;
        this.zoneColor = zoneColor;
        this.transparency = transparency;
        setOpaque(false); // Make panel transparent
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // Enable anti-aliasing for smoother edges
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Set composite for transparency
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transparency));

        // Draw semi-transparent rounded rectangle
        int rectWidth = getWidth() - 10;
        int rectHeight = getHeight() - 10;
        RoundRectangle2D rect = new RoundRectangle2D.Float(5, 5, rectWidth, rectHeight, 20, 20);
        g2d.setColor(zoneColor);
        g2d.fillRect(0,0,getWidth(),getHeight());
        g2d.setColor(Color.BLACK);
        g2d.drawRect(0, 0, getWidth()-1, getHeight()-1); //border

        // Draw white circle in center
        g2d.setColor(Color.WHITE);
        // Calculate circle size based on current dimensions
        int circleSize = Math.min(getWidth(), getHeight()) / 2;
        g2d.fillOval((getWidth() - circleSize)/2, (getHeight() - circleSize)/2, circleSize, circleSize);

        // Draw number
        g2d.setColor(Color.BLACK);
        Font font = new Font("Arial", Font.BOLD, circleSize/2);
        g2d.setFont(font);
        
        String text = Integer.toString(zoneNumber);
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (getWidth() - fm.stringWidth(text)) / 2;
        int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        
        g2d.drawString(text, textX, textY);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Zone Label Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(200, 150);

        // Create zone label with:
        // - Zone number 1
        // - Blue color
        // - 50% transparency
        ZoneLabel zoneLabel = new ZoneLabel(1, new Color(255, 0, 0), 0.5f);
        //zoneLabel.setPreferredSize(new Dimension(120, 80));

        frame.add(zoneLabel);
        frame.setVisible(true);
    }
}