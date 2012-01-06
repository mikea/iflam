package flam;

import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, SAXException {
        final FlamGenome genome1 = FlamGenome.parse("flams/e_1.flam3");
        FlamGenome genome2 = FlamGenome.parse("flams/e_3.flam3");
        TransitionGenomeProvider provider = new TransitionGenomeProvider(genome2, genome1);

        JFrame frame = new JFrame();

        frame.setPreferredSize(new Dimension(500, 500));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.getContentPane().setLayout(new BorderLayout());

        final FlamGenome finalGenome = new FlamGenome(genome2, genome1, 1.0);

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