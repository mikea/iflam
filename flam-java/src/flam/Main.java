package flam;

import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, SAXException {
        FlamGenome flamGenome = FlamGenome.parse("flams/e_3.flam3");

        JFrame frame = new JFrame();

        frame.setPreferredSize(new Dimension(500, 500));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(new FlamComponent(flamGenome), BorderLayout.CENTER);
        
        frame.pack();
        frame.setVisible(true);
    }
}