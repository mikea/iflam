package flam.ria;


import static flam.MyMath.*;

/**
 * @author mike
 */
public class flam3 {
    public static final int PREFILTER_WHITE = 255;


    static double adjust_percentage(double in) {

        if (in == 0.0)
            return (0.0);
        else
            return (pow(10.0, -log(1.0 / in) / log(2)));

    }


}
