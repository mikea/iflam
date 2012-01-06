package flam;

import static java.lang.Math.floor;
import static java.lang.Math.pow;
import static java.lang.StrictMath.log;

/**
 * @author mike
 */
public class flam3 {
    public static final int PREFILTER_WHITE = 255;

    static double flam3_calc_alpha(double density, double gamma, double linrange) {
        double dnorm = density;
        double funcval = pow(linrange, gamma);
        double frac, alpha;

        if (dnorm > 0) {
            if (dnorm < linrange) {
                frac = dnorm / linrange;
                alpha = (1.0 - frac) * dnorm * (funcval / linrange) + frac * pow(dnorm, gamma);
            } else
                alpha = pow(dnorm, gamma);
        } else
            alpha = 0;

        return (alpha);
    }

    static void flam3_calc_newrgb(double[] cbuf, double ls, double highpow, double[] newrgb) {
        int rgbi;
        double newls, lsratio;
        double newhsv[] = new double[3];
        double a, maxa = -1.0, maxc = 0;
        double adjhlp;

        if (ls == 0.0 || (cbuf[0] == 0.0 && cbuf[1] == 0.0 && cbuf[2] == 0.0)) {
            newrgb[0] = 0.0;
            newrgb[1] = 0.0;
            newrgb[2] = 0.0;
            return;
        }

        /* Identify the most saturated channel */
        for (rgbi = 0; rgbi < 3; rgbi++) {
            a = ls * (cbuf[rgbi] / PREFILTER_WHITE);
            if (a > maxa) {
                maxa = a;
                maxc = cbuf[rgbi] / PREFILTER_WHITE;
            }
        }

        /* If a channel is saturated and we have a non-negative highlight power */
        /* modify the color to prevent hue shift                                */
        if (maxa > 255 && highpow >= 0.0) {
            newls = 255.0 / maxc;
            lsratio = pow(newls / ls, highpow);

            /* Calculate the max-value color (ranged 0 - 1) */
            for (rgbi = 0; rgbi < 3; rgbi++)
                newrgb[rgbi] = newls * (cbuf[rgbi] / PREFILTER_WHITE) / 255.0;

            /* Reduce saturation by the lsratio */
            rgb2hsv(newrgb, newhsv);
            newhsv[1] *= lsratio;
            hsv2rgb(newhsv, newrgb);

            for (rgbi = 0; rgbi < 3; rgbi++)
                newrgb[rgbi] *= 255.0;

        } else {
            newls = 255.0 / maxc;
            adjhlp = -highpow;
            if (adjhlp > 1)
                adjhlp = 1;
            if (maxa <= 255)
                adjhlp = 1.0;

            /* Calculate the max-value color (ranged 0 - 1) interpolated with the old behaviour */
            for (rgbi = 0; rgbi < 3; rgbi++)
                newrgb[rgbi] = ((1.0 - adjhlp) * newls + adjhlp * ls) * (cbuf[rgbi] / PREFILTER_WHITE);
        }
    }


    /* rgb 0 - 1,
       h 0 - 6, s 0 - 1, v 0 - 1 */
    static void rgb2hsv(double[] rgb, double[] hsv) {
        double rd, gd, bd, h, s, v, max, min, del, rc, gc, bc;

        rd = rgb[0];
        gd = rgb[1];
        bd = rgb[2];

        /* compute maximum of rd,gd,bd */
        if (rd >= gd) {
            if (rd >= bd) max = rd;
            else max = bd;
        } else {
            if (gd >= bd) max = gd;
            else max = bd;
        }

        /* compute minimum of rd,gd,bd */
        if (rd <= gd) {
            if (rd <= bd) min = rd;
            else min = bd;
        } else {
            if (gd <= bd) min = gd;
            else min = bd;
        }

        del = max - min;
        v = max;
        if (max != 0.0) s = (del) / max;
        else s = 0.0;

        h = 0;
        if (s != 0.0) {
            rc = (max - rd) / del;
            gc = (max - gd) / del;
            bc = (max - bd) / del;

            if (rd == max) h = bc - gc;
            else if (gd == max) h = 2 + rc - bc;
            else if (bd == max) h = 4 + gc - rc;

            if (h < 0) h += 6;
        }

        hsv[0] = h;
        hsv[1] = s;
        hsv[2] = v;
    }


    /* h 0 - 6, s 0 - 1, v 0 - 1
       rgb 0 - 1 */
    static void hsv2rgb(double[] hsv, double[] rgb) {
        double h = hsv[0], s = hsv[1], v = hsv[2];
        int j;
        double rd, gd, bd;
        double f, p, q, t;

        while (h >= 6.0) h = h - 6.0;
        while (h < 0.0) h = h + 6.0;
        j = (int) floor(h);
        f = h - j;
        p = v * (1 - s);
        q = v * (1 - (s * f));
        t = v * (1 - (s * (1 - f)));

        switch (j) {
            case 0:
                rd = v;
                gd = t;
                bd = p;
                break;
            case 1:
                rd = q;
                gd = v;
                bd = p;
                break;
            case 2:
                rd = p;
                gd = v;
                bd = t;
                break;
            case 3:
                rd = p;
                gd = q;
                bd = v;
                break;
            case 4:
                rd = t;
                gd = p;
                bd = v;
                break;
            case 5:
                rd = v;
                gd = p;
                bd = q;
                break;
            default:
                rd = v;
                gd = t;
                bd = p;
                break;
        }

        rgb[0] = rd;
        rgb[1] = gd;
        rgb[2] = bd;
    }


    static double adjust_percentage(double in) {

       if (in==0.0)
          return(0.0);
       else
          return(pow(10.0, -log(1.0/in)/log(2)));

    }


}
