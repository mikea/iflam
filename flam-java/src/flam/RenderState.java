package flam;

import java.awt.*;
import java.awt.image.BufferedImage;

import static flam.MyMath.log;
import static flam.MyMath.pow;

/**
 */
class RenderState {
    double[] xyc = new double[3];
    final FlamGenome genome;
    private final RenderBuffer buffer;
    private static final double PREFILTER_WHITE = 255;
    private final double scale;
    private final double ppux;
    private final double ppuy;
    public long samples = 0;
    double viewHeight;
    double viewWidth;
    double viewLeft;
    double viewBottom;
    private int lastxf = 0;

    public RenderState(FlamGenome genome, RenderBuffer buffer) {
        this.genome = genome;
        this.buffer = buffer;

        scale = pow(2.0, genome.zoom);
        ppux = genome.pixelsPerUnit * scale;
        ppuy = genome.pixelsPerUnit * scale;

        viewLeft = genome.center[0] - buffer.width / ppux / 2.0;
        viewBottom = genome.center[1] - buffer.height / ppuy / 2.0;

        viewHeight = buffer.height / ppuy;
        viewWidth = buffer.width / ppux;

        reseed();
    }

    // density is the only variable that varies from pixel to pixel.
    static double calcAlpha(double density, double gamma, double linearRange, double linRangePowGamma) {
        if (density > 0) {
            if (density < linearRange) {
                double frac = density / linearRange;
                return (1.0 - frac) * density * linRangePowGamma + frac * pow(density, gamma);
            } else {
                return pow(density, gamma);
            }
        } else {
            return 0;
        }
    }

    static void calcNewRgb(double[] rgb, double[] newRgb, double ls, double highpow) {
        if (ls == 0.0 || (rgb[0] == 0.0 && rgb[1] == 0.0 && rgb[2] == 0.0)) {
            newRgb[0] = 0.0;
            newRgb[1] = 0.0;
            newRgb[2] = 0.0;
            return;
        }

        /* Identify the most saturated channel */
        double maxA = -1.0;
        double maxC = 0;
        for (int i = 0; i < 3; i++) {
            double a = ls * (rgb[i] / flam3.PREFILTER_WHITE);
            if (a > maxA) {
                maxA = a;
                maxC = rgb[i] / flam3.PREFILTER_WHITE;
            }
        }

        /* If a channel is saturated and we have a non-negative highlight power */
        /* modify the color to prevent hue shift                                */
        if (maxA > 255 && highpow >= 0.0) {
            double newls = 255.0 / maxC;
            /* Calculate the max-value color (ranged 0 - 1) */
            for (int i = 0; i < 3; i++)
                newRgb[i] = newls * (rgb[i] / flam3.PREFILTER_WHITE) / 255.0;

            /* Reduce saturation by the lsratio */
            double newHsv[] = new double[3];
            flam3.rgb2hsv(newRgb, newHsv);
            newHsv[1] *= pow(newls / ls, highpow);
            flam3.hsv2rgb(newHsv, newRgb);

            for (int i = 0; i < 3; i++) {
                newRgb[i] *= 255.0;
            }
        } else {
            double newLs = 255.0 / maxC;
            double adjHlp = -highpow;
            if (adjHlp > 1)
                adjHlp = 1;
            if (maxA <= 255)
                adjHlp = 1.0;

            /* Calculate the max-value color (ranged 0 - 1) interpolated with the old behaviour */
            for (int i = 0; i < 3; i++) {
                newRgb[i] = ((1.0 - adjHlp) * newLs + adjHlp * ls) * (rgb[i] / flam3.PREFILTER_WHITE);
            }
        }
    }

    public void reseed() {
        xyc[0] = FlamComponent.crnd();
        xyc[1] = FlamComponent.crnd();
        xyc[2] = FlamComponent.rnd();
    }

    FlamGenome.Xform pickRandomXform() {
        int k;
        if (genome.chaosEnabled) {
            k = genome.xformDistrib[lastxf][FlamComponent.random.nextInt(FlamGenome.CHOOSE_XFORM_GRAIN)];
            lastxf = k + 1;
        } else {
            k = genome.xformDistrib[0][FlamComponent.random.nextInt(FlamGenome.CHOOSE_XFORM_GRAIN)];
        }
        return genome.xforms.get(k);
    }


    void renderHistogram() {
        BufferedImage image = buffer.image;
        int width = buffer.width;
        int height = buffer.height;
        double[] accum = buffer.accum;

        {
            Graphics bg = image.getGraphics();
            bg.setColor(Color.BLACK);
            bg.fillRect(0, 0, image.getWidth(), image.getHeight());
        }

        int vib_gam_n = 1;
        double vibrancy = genome.vibrancy;
        vibrancy /= vib_gam_n;
        double linrange = genome.gammaLinearThreshold;
        double gamma = 1.0 / (genome.gamma / vib_gam_n);
        double highpow = genome.highlightPower;

        int nbatches = genome.nbatches;
        double oversample = 1.0; // genome.oversample
        // double sample_density = genome.quality * scale * scale;
        // double nsamples = sample_density * width * height;

        double sample_density = ((double) (samples)) / (width * height);
        double batch_filter = 1 / nbatches;

        double k1 = (genome.contrast * genome.brightness * PREFILTER_WHITE * 268.0 * batch_filter) / 256;
        double area = width * height / (ppux * ppuy);
        double sumfilt = 1;
        double k2 = (oversample * oversample * nbatches) /
                (genome.contrast * area * /* WHITE_LEVEL * */ sample_density * sumfilt);
        double linRangePowGamma = pow(linrange, gamma) / linrange;

        int[] line = new int[width];
        double[] newrgb = new double[4];

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int offset = (x + width * y) * 4;
                double cr = accum[offset];
                double cg = accum[offset + 1];
                double cb = accum[offset + 2];
                double freq = accum[offset + 3];

                if (freq != 0) {
                    double ls = (k1 * log(1.0 + freq * k2)) / freq;
                    freq *= ls;
                    cr *= ls;
                    cg *= ls;
                    cb *= ls;
                }

                double alpha, ls;

                if (freq <= 0) {
                    alpha = 0.0;
                    ls = 0.0;
                } else {
                    double tmp = freq / PREFILTER_WHITE;
                    alpha = calcAlpha(tmp, gamma, linrange, linRangePowGamma);
                    ls = vibrancy * 256.0 * alpha / tmp;
                    if (alpha < 0.0) alpha = 0.0;
                    if (alpha > 1.0) alpha = 1.0;
                }

                double[] t = {cr, cg, cb, freq};
                calcNewRgb(t, newrgb, ls, highpow);

                for (int rgbi = 0; rgbi < 3; rgbi++) {
                    double a = newrgb[rgbi];
                    a += (1.0 - vibrancy) * 256.0 * pow(t[rgbi] / PREFILTER_WHITE, gamma);

                    a += ((1.0 - alpha) * genome.background[rgbi]);


                    if (a > 255) a = 255;
                    if (a < 0) a = 0;
                    t[rgbi] = a;
                }


                t[3] = alpha;
                line[x] = ((int) (t[0]) << 16) | ((int) (t[1]) << 8) | (int) (t[2]);
            }

            setLine(image, width, line, y);
        }
    }

    private void setLine(BufferedImage image, int width, int[] line, int y) {
        image.setRGB(0, y, width, 1, line, 0, 1);
    }

    public boolean applyXform(FlamGenome.Xform xform) {
        return xform.applyTo(xyc, xyc);
    }
}
