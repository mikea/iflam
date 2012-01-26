package flam.util;

import java.util.Random;

/**
 */
public class Rnd {
    public static final Random random = new Random();

    public static double crnd() {
        return random.nextDouble() * 2 - 1;
    }

    public static double rnd() {
        return random.nextDouble();
    }
}
