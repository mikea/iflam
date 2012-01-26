package flam.ria;

import flam.Constants;
import flam.Genome;
import flam.GenomeView;
import flam.RenderBuffer;
import flam.util.Colors;
import flam.util.Convolution;
import flam.util.GaussianFilter;
import flam.util.View2D;

import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static flam.util.MyMath.*;

/**
 */
class RIABuffer implements RenderBuffer {
    private static final double PREFILTER_WHITE = 255;

    final int width;
    final int height;
    final double[] accum; // (r, g, b, alpha)
    final double[] densityEstimate; // (estimated densityEstimate)
    final BufferedImage image;
    final BufferedImage filteredImage;
    private long samples = 0;

    RIABuffer(int width, int height) {
        this.width = width;
        this.height = height;

        image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
        filteredImage = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
        accum = new double[this.width * this.height * 4];
        densityEstimate = new double[this.width * this.height];
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void reset() {
        Arrays.fill(accum, 0);
    }


    public BufferedImage getImage() {
        if (Constants.FILTER_IMAGE) {
            return filteredImage;
        }
        return image;
    }

    void renderHistogram(Genome genome) {
        double vibrancy = genome.vibrancy;
        double gamma = 1.0 / (genome.gamma);
        double highpow = genome.highlightPower;

        double scale = pow(2.0, genome.zoom);
        double ppux = genome.pixelsPerUnit * scale;
        double ppuy = genome.pixelsPerUnit * scale;

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
                line[x] = Colors.packRgb(t[0], t[1], t[2]);
            }

            setLine(image, width, line, y);
        }

        if (Constants.FILTER_IMAGE) {
            filterImage(genome);
        }
    }

    private void filterImage(Genome genome) {
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
        blurredImages.add(image);

        float radiusStep = 1;
        for (float radius = 2; radius <= genome.estimatorRadius; radius += radiusStep) {
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            blurredImages.add(img);
            new ConvolveOp(GaussianFilter.makeKernel(radius)).filter(image, img);
        }


        BufferedImage dstImage = filteredImage;

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                double radius = calcFilterRadius(genome, densityEstimate, x, y, width);
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

    private void renderFilterWidth(Genome genome, int width, int height, double[] densityEstimate, BufferedImage dstImage) {
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int fw = (int) (calcFilterRadius(genome, densityEstimate, x, y, width) / genome.estimatorRadius * 255.0);
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

    private double calcFilterRadius(Genome genome, double[] densityEstimate, int x, int y, int width) {
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
            Colors.rgb2hsv(newRgb, newHsv);
            newHsv[1] *= pow(newls / ls, highpow);
            Colors.hsv2rgb(newHsv, newRgb);

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
    
    void updateHistogram(Genome genome, GenomeView view, double x, double y, double cc, double opacity) {
        // TODO: speed up

//        System.out.println("(" + x1 + "," + y1 + ")");

        int[] v = new int[2];
        double[] c = new double[] {x, y};
        if (!view.coordsToView(c, v)) {
            return;
        }
        
        int offset = (v[0] + width * v[1]) * 4;


        double[] color = genome.getColor(((int) min(max(cc * 255.0, 0), 255)));
        accum[offset] += color[0];
        accum[offset + 1] += color[1];
        accum[offset + 2] += color[2];
        accum[offset + 3] += opacity;
        ++samples;
    }
    
}
