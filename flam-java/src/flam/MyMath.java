package flam;

import org.apache.commons.math.util.FastMath;

/**
 */
public class MyMath {
    public static final double PI = Math.PI;

    public static double floor(double d) {
        return FastMath.floor(d);
    }

    public static double atan2(double y, double x) {
        return fastAtan2(y, x);
    }

    public static double sin(double x) {
        return FastMath.sin(x);
    }

    public static double cos(double x) {
        return FastMath.cos(x);
    }

    public static double min(double x, double y) {
        return FastMath.min(x, y);
    }

    public static double max(double x, double y) {
        return FastMath.max(x, y);
    }

    public static double pow(double x, double y) {
        return Math.pow(x, y);
    }

    public static double sqrt(double x) {
        return FastMath.sqrt(x);
    }

    public static double log(double x) {
        return FastMath.log(x);
    }

    public static double abs(double x) {
        return FastMath.abs(x);
    }
    public static double fmod(double x, double y) {
        return FastMath.IEEEremainder(x, y);
    }

    //http://dspguru.com/dsp/tricks/fixed-point-atan2-with-self-normalization
    static double fastAtan2(double x, double y) {
        double coeff_1 = PI / 4;
        double coeff_2 = 3 * coeff_1;
        double abs_y = abs(x) + 1e-10;      // kludge to prevent 0/0 condition

        double r;
        double angle;

        if (y >= 0) {
            r = (y - abs_y) / (y + abs_y);
            angle = coeff_1 - coeff_1 * r;
        } else {
            r = (y + abs_y) / (abs_y - y);
            angle = coeff_2 - coeff_1 * r;
        }
        if (x < 0)
            return (-angle);     // negate if in quad III or IV
        else
            return (angle);
    }
}
