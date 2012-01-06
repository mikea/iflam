package flam;

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

        GenomeProvider staticProvider = new GenomeProvider() {
            @Override
            public FlamGenome getGenome() {
                return genome1;
            }

            @Override
            public void reset() {
            }
        };
        frame.getContentPane().add(new FlamComponent(new AnimationProvider(genome1)), BorderLayout.CENTER);
        
        frame.pack();
        frame.setVisible(true);
    }
}