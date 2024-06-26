package flam.mains;

import flam.FlamComponent;
import flam.Genome;
import flam.GenomeProvider;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class TransitionMain {
    public static void main(String[] args) throws IOException, SAXException {
        final Genome genome1 = Genome.parse("flams/e_4.flam3");
        final Genome genome2 = Genome.parse("sheeps/1006.flam3");

        final JFrame frame = new JFrame();

        frame.setPreferredSize(new Dimension(500, 500));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.getContentPane().setLayout(new BorderLayout());


        final JSlider slider = new JSlider(0, 1024, 0);
        frame.getContentPane().add(slider, BorderLayout.NORTH);

        final GenomeProvider provider = new GenomeProvider() {
            public int oldValue = slider.getValue();
            public Genome genome = genome1;

            @Override
            public Genome getGenome() {
                if (slider.getValue() != oldValue) {
//                    System.out.println("slider.getValue() = " + slider.getValue());
                    genome = new Genome(genome1, genome2, slider.getValue() / 1024.0);

//                    System.out.println(genome.toString());

                    oldValue = slider.getValue();
                }
                return genome;
            }

            @Override
            public void reset() {
            }
        };
        System.out.println(genome1.toString());
        System.out.println(genome2.toString());
        frame.getContentPane().add(new FlamComponent(provider), BorderLayout.CENTER);

        frame.pack();
        frame.setVisible(true);
    }
}