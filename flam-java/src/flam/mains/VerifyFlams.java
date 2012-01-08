package flam.mains;

import flam.FlamComponent;
import flam.FlamGenome;

import java.io.File;

/**
 * @author mike
 */
public class VerifyFlams {
    private static FlamGenome genome;

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
            genome = FlamGenome.parse(path);
            for (FlamGenome.Xform xform : genome.xforms) {
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

    private static void checkXform(FlamGenome.Xform xform) {
        xform.applyTo(new double[]{FlamComponent.crnd(), FlamComponent.crnd(), FlamComponent.crnd()}, new double[3]);
    }

}
