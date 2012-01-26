package flam.mains;

import flam.Genome;
import flam.util.Rnd;
import flam.Xform;

import java.io.File;

/**
 * @author mike
 */
public class VerifyFlams {
    private static Genome genome;

    public static void main(String[] args) {
        int invalid = 0;
        int valid = 0;
        for (File sheep : new File("sheeps").listFiles()) {
            if (sheep.getName().endsWith("flam3")) {
                if (!verifyGenome(sheep.getAbsolutePath())) {
                    invalid++;
                } else {
                    valid++;
                }
            }
        }

        System.out.println("valid = " + valid);
        System.out.println("invalid = " + invalid);
    }

    private static boolean verifyGenome(String path) {
        try {
            genome = Genome.parse(path);
            for (Xform xform : genome.xforms) {
                checkXform(xform);
            }
            
            if (genome.finalxform != null) {
                checkXform(genome.finalxform);
            }
            
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
