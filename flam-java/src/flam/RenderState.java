package flam;

import java.awt.image.BufferedImage;

import static flam.MyMath.floor;
import static flam.MyMath.log;
import static flam.MyMath.pow;

/**
 */
class RenderState {
    double[] xyc = new double[3];
    final Genome genome;
    private final RenderBuffer buffer;
    private static final double PREFILTER_WHITE = 255;
    private final double ppux;
    private final double ppuy;
    public long samples = 0;
    double viewHeight;
    double viewWidth;
    double viewLeft;
    double viewBottom;
    private int lastxf = 0;

    public RenderState(Genome genome, RenderBuffer buffer) {
        this.genome = genome;
        this.buffer = buffer;

        double scale = pow(2.0, genome.zoom);
        ppux = genome.pixelsPerUnit * scale;
        ppuy = genome.pixelsPerUnit * scale;

        double genomeHeight = genome.size[1];
        double genomeWidth = genomeHeight * (buffer.width * 1.0 / buffer.height);

        viewLeft = genome.center[0] - genomeWidth / ppux / 2.0;
        viewBottom = genome.center[1] - genomeHeight / ppuy / 2.0;

        viewHeight = genomeHeight / ppuy;
        viewWidth = genomeWidth / ppux;

        reseed();
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

    Xform pickRandomXform() {
        int k;
        if (genome.chaosEnabled) {
            k = genome.xformDistrib[lastxf][FlamComponent.random.nextInt(Genome.CHOOSE_XFORM_GRAIN)];
            lastxf = k + 1;
        } else {
            k = genome.xformDistrib[0][FlamComponent.random.nextInt(Genome.CHOOSE_XFORM_GRAIN)];
        }
        return genome.xforms.get(k);
    }


    void renderHistogram() {
        BufferedImage image = buffer.image;
        int width = buffer.width;
        int height = buffer.height;
        double[] accum = buffer.accum;

        double vibrancy = genome.vibrancy;
        double gamma = 1.0 / (genome.gamma);
        double highpow = genome.highlightPower;

        double samples_per_unit = ((double) samples) / (ppux * ppuy);
        double k1 = (genome.contrast * genome.brightness * PREFILTER_WHITE * 268.0) / 256;
        double k2 = 1.0 / (genome.contrast * samples_per_unit);

        int[] line = new int[width];
        double[] newrgb = new double[4];

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int offset = (x + width * y) * 4;
                double freq = accum[offset + 3];

                if (freq == 0) {
                    line[x] = ((int) (genome.background[0]) << 16) | ((int) (genome.background[1]) << 8) | (int) (genome.background[2]);
                    continue;
                }

                double ls = (k1 * log(1.0 + freq * k2)) / freq;

                freq *= ls;
                double r = accum[offset] * ls;
                double g = accum[offset + 1] * ls;
                double b = accum[offset + 2] * ls;

                double tmp = freq / PREFILTER_WHITE;
                double alpha = pow(tmp, gamma);
                ls = vibrancy * 256.0 * alpha / tmp;
                if (alpha < 0.0) alpha = 0.0;
                if (alpha > 1.0) alpha = 1.0;

                double[] t = {r, g, b, freq};
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

        // filterImage();
    }

    private void filterImage() {
        BufferedImage srcImage = buffer.image;
        BufferedImage dstImage = buffer.filteredImage;
        int width = buffer.width;
        int height = buffer.height;
        double[] accum = buffer.accum;

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int offset = (x + width * y) * 4;
                double freq = accum[offset + 3];

                double dFilterWidth = genome.estimatorRadius / pow((freq + 1), genome.estimatorCurve);
                if (dFilterWidth < genome.estimatorMinimum) {
                    dFilterWidth = genome.estimatorMinimum;
                }

                int filterWidth = (int) floor(dFilterWidth);
                if (filterWidth % 2 == 0) ++filterWidth;
                // filterWidth = 5;

                if (filterWidth <= 1) {
                    dstImage.setRGB(x, y, srcImage.getRGB(x, y));
                } else {
                    double[][] filter = buffer.getFilter(filterWidth);
                    double sr = 0;
                    double sg = 0;
                    double sb = 0;

                    for (int fx = 0; fx < filterWidth; fx++) {
                        for (int fy = 0; fy < filterWidth; fy++) {
                            int xx = x + fx - filterWidth / 2;
                            int yy = y + fy - filterWidth / 2;
                            
                            if (xx < 0 || yy < 0 || xx >= width || yy >= height) {
                                continue;
                            }

                            int srcRgb = srcImage.getRGB(xx, yy);
                            double r = (srcRgb & 0xFF0000) >> 16;
                            double g = (srcRgb & 0xFF00) >> 8;
                            double b = (srcRgb & 0xFF);
                            sr += r * filter[fx][fy];
                            sg += g * filter[fx][fy];
                            sb += b * filter[fx][fy];
                        }
                    }

                    int rgb = ((int) (sr) << 16) | ((int) (sg) << 8) | (int) (sb);
                    dstImage.setRGB(x, y, rgb);
                }
            }
        }
    }

    private void setLine(BufferedImage image, int width, int[] line, int y) {
        image.setRGB(0, y, width, 1, line, 0, 1);
    }

    public boolean applyXform(Xform xform) {
        return xform.applyTo(xyc, xyc);
    }
}
