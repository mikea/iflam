package flam.util;

import java.util.Random;

/**
 */
public class Rnd {
    private static final Random random = new Random();

    public static double crnd() {
        return random.nextDouble() * 2 - 1;
    }

    public static double rnd() {
        return random.nextDouble();
    }               
    
    public static int irnd(int n) {
        return random.nextInt(n);
    }

    public static boolean brnd() {
        return random.nextBoolean();
    }

    public static double grnd() {
        return random.nextGaussian();
    }
}
