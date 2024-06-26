package flam.util;

import static flam.util.MyMath.floor;

/**
 */
public class Colors {
    /* rgb 0 - 1,
h 0 - 6, s 0 - 1, v 0 - 1 */
    public static void rgb2hsv(double[] rgb, double[] hsv) {
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
    public static void hsv2rgb(double[] hsv, double[] rgb) {
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

    public static int packRgb(double r, double g, double b) {
        return ((int) (r) << 16) | ((int) (g) << 8) | (int) (b);
    }

    public static int packRgb(int r, int g, int b) {
        return (r << 16) | (g << 8) | b;
    }
}
