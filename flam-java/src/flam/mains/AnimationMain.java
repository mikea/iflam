package flam.mains;

import flam.AnimationProvider;
import flam.FlamComponent;
import flam.FlamGenome;
import flam.GenomeProvider;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class AnimationMain {
    public static void main(String[] args) throws IOException, SAXException {
        final FlamGenome genome1 = FlamGenome.parse("flams/e_2.flam3");

        JFrame frame = new JFrame();

        frame.setPreferredSize(new Dimension(500, 500));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.getContentPane().setLayout(new BorderLayout());

        frame.getContentPane().add(new FlamComponent(new AnimationProvider(genome1)), BorderLayout.CENTER);
        
        frame.pack();
        frame.setVisible(true);
    }
}