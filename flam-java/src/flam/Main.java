package flam;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(new FlamComponent());
        
        frame.pack();
        frame.setVisible(true);
    }
}