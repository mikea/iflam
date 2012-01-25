package flam;

import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.util.ArrayList;
import java.util.List;

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

        if (Constants.FILTER_IMAGE) {
            filterImage();
        }
    }

    private void filterImage() {
        final int width = buffer.width;
        final int height = buffer.height;
        final double[] accum = buffer.accum;
        final double[] densityEstimate = buffer.densityEstimate;

        System.out.println("Density estimation");
        // Estimate density first.
        double[][] estimatorFilter = GaussianFilter.getFilter((int) genome.estimatorRadius);
        Convolution.convolve(new View2D() {
                    @Override
                    public int getHeight() {
                        return height;
                    }

                    @Override
                    public int getWidth() {
                        return width;
                    }

                    @Override
                    public void set(int x, int y, double d) {
                        throw new UnsupportedOperationException("Method set not implemented in  .");
                    }

                    @Override
                    public double get(int x, int y) {
                        int offset = (x + width * y) * 4;
                        return accum[offset + 3];
                    }
                }, new View2D() {
                    @Override
                    public int getHeight() {
                        return height;
                    }

                    @Override
                    public int getWidth() {
                        return width;
                    }

                    @Override
                    public void set(int x, int y, double d) {
                        int offset = (x + width * y);
                        densityEstimate[offset] = d;
                    }

                    @Override
                    public double get(int x, int y) {
                        throw new UnsupportedOperationException("Method get not implemented in  .");
                    }
                }, estimatorFilter
        );

        System.out.println("Filtering");
        List<BufferedImage> blurredImages = new ArrayList<BufferedImage>();
        blurredImages.add(buffer.image);

        float radiusStep = 1;
        for (float radius = 2; radius <= genome.estimatorRadius; radius += radiusStep) {
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            blurredImages.add(img);
            new ConvolveOp(GaussianFilter.makeKernel(radius)).filter(buffer.image, img);
        }


        BufferedImage dstImage = buffer.filteredImage;

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                double radius = calcFilterRadius(densityEstimate, x, y, width);
                if (radius < 1) radius = 1;

                double f = Math.floor(radius);
                double c = Math.ceil(radius);

                if (f == c) {
                    dstImage.setRGB(x, y, blurredImages.get((int) (f / radiusStep - 1)).getRGB(x, y));
                    continue;
                }

                BufferedImage fimg = blurredImages.get((int) (f / radiusStep - 1));
                BufferedImage cimg = blurredImages.get((int) (c / radiusStep - 1));

                int frgb = fimg.getRGB(x, y);
                double fr = (frgb & 0xFF0000) >> 16;
                double fg = (frgb & 0xFF00) >> 8;
                double fb = (frgb & 0xFF);

                int crgb = cimg.getRGB(x, y);
                double cr = (crgb & 0xFF0000) >> 16;
                double cg = (crgb & 0xFF00) >> 8;
                double cb = (crgb & 0xFF);

                double r = fr * (c - radius) + cr * (radius - f);
                double g = fg * (c - radius) + cg * (radius - f);
                double b = fb * (c - radius) + cb * (radius - f);

                int rgb = ((int) (r) << 16) | ((int) (g) << 8) | (int) (b);
                dstImage.setRGB(x, y, rgb);
            }
        }

/*
        Convolution.convolve(srcImage, dstImage, new Convolution.FilterProvider() {
            @Override
            public double[][] getFilter(int x, int y) {
                double radius = calcFilterRadius(densityEstimate, x, y, width);
                return GaussianFilter.getFilter(radius);
            }
        });
*/
//        renderFilterWidth(width, height, densityEstimate, dstImage);
    }

    private void renderFilterWidth(int width, int height, double[] densityEstimate, BufferedImage dstImage) {
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int fw = (int) (calcFilterRadius(densityEstimate, x, y, width) / genome.estimatorRadius * 255.0);
//                int fw = (int) densityEstimate[x + y * width];

                int r = fw;
                int g = fw;
                int b = fw;

                if (fw > 255) {
                    g = 0;
                    b = 0;
                    r = 255;
                }

                int rgb = ((int) (r) << 16) | ((int) (g) << 8) | b;
                dstImage.setRGB(x, y, rgb);
            }
        }
    }

    private double calcFilterRadius(double[] densityEstimate, int x, int y, int width) {
        double freq = densityEstimate[x + width * y];
        double radius = genome.estimatorRadius;

        if (freq > 0) {
            radius = genome.estimatorRadius / pow(freq, genome.estimatorCurve);
            if (radius < genome.estimatorMinimum) {
                radius = genome.estimatorMinimum;
            } else if (radius > genome.estimatorRadius) {
                radius = genome.estimatorRadius;
            }
        }

        return radius;
    }

    private void setLine(BufferedImage image, int width, int[] line, int y) {
        image.setRGB(0, y, width, 1, line, 0, 1);
    }

    public boolean applyXform(Xform xform) {
        return xform.applyTo(xyc, xyc);
    }
}
