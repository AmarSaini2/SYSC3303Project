import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

public class View {
    JFrame frame;
    JPanel panel, map, statusBars;
    JTextArea log;
    JMenu menu;
    JMenuBar menuBar;
    JMenuItem start;

    GridBagConstraints gbc;

    View(){
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
        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                startFile("Main.java");
            }
        });


        this.map = makePanel(Color.blue);
        this.statusBars = makePanel(Color.red);
        this.log = makeLogComp();
        JScrollPane scrollPane = new JScrollPane(log);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        addComp(panel, map, 0, 0, 1, 0.40, 2, 1, GridBagConstraints.BOTH);
        addComp(panel, statusBars, 1, 1, 0.5, 0.40, 1, 1, GridBagConstraints.BOTH);
        addComp(panel, scrollPane, 0, 1, 0.5, 0.40, 1, 1, GridBagConstraints.BOTH);

        frame.setContentPane(panel);
        frame.setSize(600,600);
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    private void startFile(String filePath) {
        try{
            File file = new File("src/Main.java");
            if(file.exists()){
                Desktop.getDesktop().open(file);
            }else{
                System.out.println("file not found");
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private JTextArea makeLogComp(){
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        
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

    public static void main(String[] args) {
        View view = new View();
        for (int i = 0; i < 100; i++) { 
             view.log.append(">:3\n");
             view.frame.repaint();  
        }
    }

}
