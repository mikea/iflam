package flam.mains;

import flam.FlamComponent;
import flam.Genome;
import flam.GenomeProvider;
import flam.MyMath;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class TransitionAnimateMain {
    public static void main(String[] args) throws IOException, SAXException {
        final Genome genome1 = Genome.parse("flams/e_4.flam3");
        final Genome genome2 = Genome.parse("sheeps/1006.flam3");

        final JFrame frame = new JFrame();

        frame.setPreferredSize(new Dimension(500, 500));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.getContentPane().setLayout(new BorderLayout());


        final GenomeProvider provider = new GenomeProvider() {
            @Override
            public Genome getGenome() {
                double t = MyMath.sin(System.currentTimeMillis() / 10000.0);
                t *= t;
                return new Genome(genome1, genome2, t);
            }

            @Override
            public void reset() {
            }
        };

        frame.getContentPane().add(new FlamComponent(provider), BorderLayout.CENTER);

        frame.pack();
        frame.setVisible(true);
    }
}