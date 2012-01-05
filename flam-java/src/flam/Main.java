package flam;

import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, SAXException {
        FlamGenome flamGenome = FlamGenome.parse("flams/test2.flam3");

        JFrame frame = new JFrame();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(new FlamComponent(flamGenome));
        
        frame.pack();
        frame.setVisible(true);
    }
}