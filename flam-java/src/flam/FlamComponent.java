package flam;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.Random;

import static java.lang.Math.*;

/**
 * @author mike
 */
public class FlamComponent extends JComponent {
    static final Random random = new Random();

    // state
    private final FlamGenome genome;
    private RenderState state;

    public FlamComponent(FlamGenome flamGenome) {
        genome = flamGenome;
        setFocusable(true);

        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                resetState();
                resetState();
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {
            }
        });
    }

    private void resetState() {
        state = new RenderState(genome, getWidth(), getHeight());

    }


    @Override
    protected void paintComponent(Graphics graphics) {
        if (state == null || state.width != getWidth() || state.height != getHeight()) {
            resetState();
        }
        int batchSize = 100000;

        long start = System.nanoTime();

        double rotate1 = 0;
        double rotate2 = 0;

        if (genome.rotate != 0) {
            rotate1 = cos(genome.rotate * 2 * PI / 360.0);
            rotate2 = sin(genome.rotate * 2 * PI / 360.0);
        }

        double[] xyc2 = new double[3];

        for (int i = 0; i < batchSize; ++i, ++state.samples) {
            state.applyXform(pickRandomXform());

            double[] xyc = state.xyc;

            if (genome.finalxform != null) {
                genome.finalxform.applyTo(xyc, xyc2);
                xyc = xyc2;
            }

            if (genome.rotate != 0) {
                //todo: optimize
                double x = rotate1 * xyc[0] - rotate2 * xyc[1];
                double y = rotate2 * xyc[0] + rotate1 * xyc[1];
                xyc[0] = x;
                xyc[1] = y;
            }

            if (state.samples > 20) {
                updateHistogram(xyc[0], xyc[1], xyc[2]);
            }
        }
        long batchFinish = System.nanoTime();

        blt(graphics);

        long bltFinish = System.nanoTime();
        System.out.println((batchFinish - start) / 1e9 + " : " + (bltFinish - batchFinish) / 1e9);

        repaint();
    }

    private FlamGenome.Xform pickRandomXform() {
        int k = Math.abs(random.nextInt() % genome.xforms.size());
        return genome.xforms.get(k);
    }

    private static double crnd() {
        return random.nextDouble() * 2 - 1;
    }

    private static double rnd() {
        return random.nextDouble();
    }

    private void updateHistogram(double x, double y, double cc) {
        int x1 = (int) ((x - state.viewLeft) * state.width / state.viewWidth + 0.5);
        int y1 = (int) ((y - state.viewBottom) * state.height / state.viewHeight + 0.5);

        if (x1 < 0 || x1 >= state.width || y1 < 0 || y1 >= state.height) {
            return;
        }

        int offset = x1 + state.width * y1;

        ++state.freqHistogram[offset];

        double[] color = genome.colors[((int) Math.min(Math.max(cc * 255.0, 0), 255))];
        state.colorHistogram[offset * 3] += color[0];
        state.colorHistogram[offset * 3 + 1] += color[1];
        state.colorHistogram[offset * 3 + 2] += color[2];
    }

    private void blt(Graphics graphics) {
        state.renderHistogram();
        graphics.drawImage(state.image, 0, 0, null);
    }


    private static class RenderState {
        private double[] xyc = new double[3];
        private final FlamGenome genome;
        private final int width;
        private final int height;
        private final int[] freqHistogram;
        private final double[] colorHistogram;
        private final BufferedImage image;
        private static final double PREFILTER_WHITE = 255;
        private final double scale;
        private final double ppux;
        private final double ppuy;
        public long samples = 0;
        private double viewHeight;
        private double viewWidth;
        private double viewLeft;
        private double viewBottom;

        public RenderState(FlamGenome genome, int width, int height) {
            this.genome = genome;
            this.width = width;
            this.height = height;

            xyc[0] = crnd();
            xyc[1] = crnd();
            xyc[2] = rnd();

            image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
            freqHistogram = new int[this.width * this.height];
            colorHistogram = new double[this.width * this.height * 3];

            scale = Math.pow(2.0, genome.zoom);
            ppux = genome.pixelsPerUnit * scale;
            ppuy = genome.pixelsPerUnit * scale;

            viewLeft = genome.center[0] - width / ppux / 2.0;
            viewBottom = genome.center[1] - height / ppuy / 2.0;

            viewHeight = height / ppuy;
            viewWidth = width / ppux;
        }

        private void renderHistogram() {
            {
                Graphics bg = image.getGraphics();
                bg.setColor(Color.BLACK);
                bg.fillRect(0, 0, image.getWidth(), image.getHeight());
            }

            int vib_gam_n = 1;
            double vibrancy = genome.vibrancy;
            vibrancy /= vib_gam_n;
            double linrange = genome.gammaLinearThreshold;
            double g = 1.0 / (genome.gamma / vib_gam_n);
            double highpow = genome.highlightPower;

            int nchan = 3;
            int transp = 0;


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

            int[] line = new int[width];
            for (int y = 0; y < height; ++y) {
                for (int x = 0; x < width; ++x) {
                    int offset = x + width * y;
                    double freq = freqHistogram[offset];
                    double cr = colorHistogram[offset * 3];
                    double cg = colorHistogram[offset * 3 + 1];
                    double cb = colorHistogram[offset * 3 + 2];

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
                        alpha = flam3.flam3_calc_alpha(tmp, g, linrange);
                        ls = vibrancy * 256.0 * alpha / tmp;
                        if (alpha < 0.0) alpha = 0.0;
                        if (alpha > 1.0) alpha = 1.0;
                    }

                    double[] t = {cr, cg, cb, freq};
                    double[] newrgb = new double[4];
                    flam3.flam3_calc_newrgb(t, ls, highpow, newrgb);

                    for (int rgbi = 0; rgbi < 3; rgbi++) {
                        double a = newrgb[rgbi];
                        a += (1.0 - vibrancy) * 256.0 * pow(t[rgbi] / PREFILTER_WHITE, g);

                        a += ((1.0 - alpha) * genome.background[rgbi]);


                        if (a > 255) a = 255;
                        if (a < 0) a = 0;
                        t[rgbi] = a;
                    }


                    t[3] = alpha;
                    line[x] = ((int) (t[0]) << 16) | ((int) (t[1]) << 8) | (int) (t[2]);
                }

                image.setRGB(0, y, width, 1, line, 0, 1);
            }
        }

        public void applyXform(FlamGenome.Xform xform) {
            xform.applyTo(xyc, xyc);
        }
    }

}