package flam.mains;

import flam.FlamComponent;
import flam.Genome;
import flam.GenomeProvider;
import flam.util.Rnd;
import flam.Xform;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class SlideshowMain {
    private static Genome prevGenome;
    private static Genome genome;
    private static final JFrame frame = new JFrame();
    private static long lastChangeTime;
    public static final int DURATION_SEC = 60;
    public static final int TRANSITION_SEC = 5;

    public static void main(String[] args) throws IOException, SAXException {
        openRandomGenome();
        lastChangeTime = System.currentTimeMillis();
        
        frame.setPreferredSize(new Dimension(500, 500));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.getContentPane().setLayout(new BorderLayout());

         FlamComponent flamComponent = new FlamComponent(new GenomeProvider() {
            @Override
            public Genome getGenome() {
                long time = System.currentTimeMillis();
                if (time - lastChangeTime > (DURATION_SEC + TRANSITION_SEC) *1000) {
                    reset(time);
                } /*else if (time - lastChangeTime > 1 && time - lastChangeTime < TRANSITION_SEC * 1000 && prevGenome != null) {
                    double t = (time - lastChangeTime) / 1000.0 / TRANSITION_SEC;
                    System.out.println("t = " + t);
                    return new Genome(prevGenome, genome, t);
                }   */
                return genome;
            }

            @Override
            public void reset() {
                long time = System.currentTimeMillis();
                reset(time);
            }

             private void reset(long time) {
                 lastChangeTime = time;
                 openRandomGenome();
             }
         });
        flamComponent.setFps(10);
        frame.getContentPane().add(flamComponent, BorderLayout.CENTER);

        frame.pack();
        frame.setVisible(true);
    }

    private static void openRandomGenome() {
        prevGenome = genome;
        File[] sheeps = new File("sheeps").listFiles();
        
        while (true) {
            File f = sheeps[Rnd.irnd(sheeps.length)];
            if (!f.getName().endsWith(".flam3")) {
                continue;
            }
            
            if (openGenome(f.getAbsolutePath())) {
                return;
            }
        }
    }

    private static boolean openGenome(String path) {
        try {
            genome = Genome.parse(path);
            for (Xform xform : genome.xforms) {
                checkXform(xform);
            }
            
            if (genome.finalxform != null) {
                checkXform(genome.finalxform);
            }
            
            genome.zoom *= 2;
            frame.setTitle(path);
            return true;
        } catch (Exception e) {
            System.err.println("***** BAD FLAM: " + path + " : " + e.getMessage());
            return false;
        }
    }

    private static void checkXform(Xform xform) {
        xform.applyTo(new double[]{Rnd.crnd(), Rnd.crnd(), Rnd.crnd()}, new double[3]);
    }
}