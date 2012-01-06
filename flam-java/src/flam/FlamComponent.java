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

    private static final double GAMMA = 1.5;

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

        for (int i = 0; i < 500000; ++i) {
            state.applyXform(pickRandomXform());

            double[] xyc = state.xyc;

            if (genome.finalxform != null) {
                double[] xyc2 = new double[3];
                genome.finalxform.applyTo(xyc, xyc2);
                xyc = xyc2;
            }

            if (genome.rotate != 0) {
                //todo: optimize
                double c = cos(genome.rotate * 2 * PI / 360.0);
                double s = sin(genome.rotate * 2 * PI / 360.0);

                double x = c * xyc[0] - s * xyc[1];
                double y = s * xyc[0] + c * xyc[1];
                xyc[0] = x;
                xyc[1] = y;
            }

            if (i > 20) {
                updateHistogram(xyc[0], xyc[1], xyc[2]);
            }
        }

        blt(graphics);
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
        double cornerX = genome.center[0] - state.width / state.ppux / 2.0;
        double cornerY = genome.center[1] - state.height / state.ppuy / 2.0;

        double top = cornerY + state.height / state.ppuy, bottom = cornerY, left = cornerX, right = cornerX + state.width / state.ppux;

        double height = top - bottom;
        double width = right - left;

        int x1 = (int) ((x - left) * state.width / width + 0.5);
        int y1 = (int) ((y - bottom) * state.height / height + 0.5);

        if (x1 < 0 || x1 >= state.width || y1 < 0 || y1 >= state.height) {
            return;
        }

        int offset = x1 + state.width * y1;

        ++state.freqHistogram[offset];

        double[] color = genome.colors[((int) Math.min(Math.max(cc * 255.0, 0), 255))];
        state.colorHistogram[offset * 3] = (state.colorHistogram[(offset * 3)] + color[0]);
        state.colorHistogram[offset * 3 + 1] = (state.colorHistogram[offset * 3 + 1] + color[1]);
        state.colorHistogram[offset * 3 + 2] = (state.colorHistogram[offset * 3 + 2] + color[2]);
        state.samples++;
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
        }

        private void renderHistogram() {
            {
                Graphics bg = image.getGraphics();
                bg.setColor(Color.BLACK);
                bg.fillRect(0, 0, image.getWidth(), image.getHeight());
            }

            int maxFreq = 0;
            for (int aFreqHistogram : freqHistogram) {
                if (maxFreq < aFreqHistogram) {
                    maxFreq = aFreqHistogram;
                }
            }

            double maxColor = 0;
            for (double c : colorHistogram) {
                maxColor = Math.max(c, maxColor);
            }
            double maxColorLog = log(maxColor);
/*
            double k1 = (genome.contrast * genome.brightness *
                    PREFILTER_WHITE * 268.0 */
/* * batch_filter[batch_num] *//*
) / 256;
            double area = width * height / (ppux * ppuy);
            double k2 = samples /
                    (genome.contrast * area * 255 */
/* * sumfilt *//*
);
*/

            int vib_gam_n = 1;
            double vibrancy = genome.vibrancy;
            vibrancy /= vib_gam_n;
            double linrange = genome.gammaLinearThreshold;
            double g = 1.0 / (genome.gamma / vib_gam_n);
            double highpow = genome.highlightPower;

            int nchan = 3;
            int transp = 0;

            for (int x = 0; x < width; ++x) {
                for (int y = 0; y < height; ++y) {
                    int offset = x + width * y;
                    int freq = freqHistogram[offset];
                    double cr = colorHistogram[offset * 3];
                    double cg = colorHistogram[offset * 3 + 1];
                    double cb = colorHistogram[offset * 3 + 2];

                    double tmp = freq / PREFILTER_WHITE;
                    double alpha, ls;

                    if (freq <= 0) {
                        alpha = 0.0;
                        ls = 0.0;
                    } else {
                        alpha = flam3.flam3_calc_alpha(tmp, g, linrange);
                        ls = vibrancy * 256.0 * alpha / tmp;
                        if (alpha < 0.0) alpha = 0.0;
                        if (alpha > 1.0) alpha = 1.0;
                    }

                    double[] t = {cr, cg, cb, freq};
                    double[] newrgb = new double[4];
                    flam3.flam3_calc_newrgb(
                            t, ls, highpow, newrgb);

                    for (int rgbi=0;rgbi<3;rgbi++) {
                       double a = newrgb[rgbi];
                       a += (1.0-vibrancy) * 256.0 * pow( t[rgbi] / PREFILTER_WHITE, g);
                       if (nchan<=3 || transp==0)
                          a += ((1.0 - alpha) * genome.background[rgbi]);
                       else {
                          if (alpha>0)
                             a /= alpha;
                          else
                             a = 0;
                       }

                       /* Clamp here to ensure proper filter functionality */
                       if (a>255) a = 255;
                       if (a<0) a = 0;

                       /* Replace values in accumulation buffer with these new ones */
                       t[rgbi] = a;
                    }
                    t[3] = alpha;

                    int rgb = ((int) (t[0]) << 16) |
                            ((int) (t[1]) << 8) |
                            (int) (t[2]);


                    image.setRGB(x, y, rgb);

                    /*
            if (freq == 0) continue;
            double alpha = Math.log(1 + freq) / maxFreqLog;
            alpha = Math.pow(alpha, 1 / GAMMA);

            r = calcColor(r, freq, maxFreq);
            g = calcColor(g, freq, maxFreq);
            b = calcColor(b, freq, maxFreq);
            r = Math.min(Math.max(r, 0), 1);
            g = Math.min(Math.max(g, 0), 1);
            b = Math.min(Math.max(b, 0), 1);

            int rgb = ((int) (r * 255) << 16) |
                    ((int) (g * 255) << 8) |
                    (int) (b * 255);


            image.setRGB(x, y, rgb);      */
                }
            }
        }

        private double calcColor(double color, double freq, double maxFreq) {
            double alpha = Math.log(1 + freq) / log(maxFreq);
            alpha = Math.pow(alpha, 1 / GAMMA);
            return color * alpha;
        }

        public void applyXform(FlamGenome.Xform xform) {
            xform.applyTo(xyc, xyc);
        }
    }

}