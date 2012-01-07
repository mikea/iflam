package flam;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Random;

import static flam.MyMath.*;

/**
 * @author mike
 */
public class FlamComponent extends JComponent {
    static final Random random = new Random();

    // state
    private FlamGenome genome;
    private RenderState state;
    private RenderBuffer buffer;
    private GenomeProvider provider;
    private double fps = 10;
    public long iterTime = 0;
    public long renderTime = 0;
    public int batches = 0;

    public FlamComponent(final GenomeProvider provider) {
        this.provider = provider;
        setFocusable(true);

        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                reset();
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {
            }
        });
        addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                reset();
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
    }

    private void reset() {
        this.provider.reset();
        batches = 0;
        renderTime = 0;
        iterTime = 0;
    }

    public void setFps(double fps) {
        this.fps = fps;
    }

    private void resetState(FlamGenome newGenome) {
        genome = newGenome;
        if (buffer == null || buffer.width != getWidth() || buffer.height != getHeight()) {
            buffer = new RenderBuffer(getWidth(), getHeight());
        } else {
            buffer.reset();
        }
        state = new RenderState(genome, buffer);
    }


    @Override
    protected void paintComponent(Graphics graphics) {
        updateGenome();

        long start = System.currentTimeMillis();
        int i = iterateBatch(start);
        long batchFinish = System.currentTimeMillis();
        iterTime += (batchFinish - start);

        state.renderHistogram();

        long bltFinish = System.currentTimeMillis();

        renderTime += (bltFinish - batchFinish);
        batches++;

        System.out.println(i + " : " + (batchFinish - start) + " : " + (bltFinish - batchFinish));

        graphics.drawImage(buffer.image, 0, 0, null);
        repaint();
    }

    private int iterateBatch(long start) {
        double rotate1 = 0;
        double rotate2 = 0;

        if (genome.rotate != 0) {
            rotate1 = cos(genome.rotate * 2 * PI / 360.0);
            rotate2 = sin(genome.rotate * 2 * PI / 360.0);
        }

        double[] xyc2 = new double[3];

        int fuse = 5;
        int consequentErrors = 0;

        genome.createXformDistrib();

        int i = -4 * fuse;
        double allotedTimeMs = 1000.0 / fps;
        double avgRenderTime = batches == 0 ? 0 : renderTime / batches;

        for (; ; ++i, ++state.samples) {
            FlamGenome.Xform xform = state.pickRandomXform();
            if (!state.applyXform(xform)) {
                consequentErrors++;
                System.out.println("consequentErrors = " + consequentErrors);

                if (consequentErrors < 5) {
                    continue;
                }
            }

            consequentErrors = 0;

            if (i >= 0) {
                double[] xyc = state.xyc;
                double opacity = xform.opacity;

                if (opacity != 1.0) {
                    flam3.adjust_percentage(xform.opacity);
                }

                if (genome.finalxform != null) {
                    genome.finalxform.applyTo(xyc, xyc2);
                } else {
                    xyc2[0] = xyc[0];
                    xyc2[1] = xyc[1];
                    xyc2[2] = xyc[2];
                }
                xyc = xyc2;

                if (genome.rotate != 0) {
                    //todo: optimize
                    double x1 = xyc[0] - genome.center[0];
                    double y1 = xyc[1] - genome.center[1];
                    double x = rotate1 * x1 - rotate2 * y1 + genome.center[0];
                    double y = rotate2 * x1 + rotate1 * y1 + genome.center[1];
                    xyc[0] = x;
                    xyc[1] = y;
                }

                updateHistogram(xyc[0], xyc[1], xyc[2], opacity);
            }

            if (i % 1000 == 0 && i > 25000) {
                if ((System.currentTimeMillis() - start + avgRenderTime) > allotedTimeMs) {
                    return i;
                }
            }
        }
    }

    private void updateGenome() {
        FlamGenome newGenome = provider.getGenome();
        if (state == null || buffer.width != getWidth() || buffer.height != getHeight() || state.genome != newGenome) {
            resetState(newGenome);
        }
    }

    static double crnd() {
        return random.nextDouble() * 2 - 1;
    }

    private static double rnd() {
        return random.nextDouble();
    }

    private void updateHistogram(double x, double y, double cc, double opacity) {
        int x1 = (int) ((x - state.viewLeft) * buffer.width / state.viewWidth + 0.5);
        int y1 = (int) ((y - state.viewBottom) * buffer.height / state.viewHeight + 0.5);

//        System.out.println("(" + x1 + "," + y1 + ")");

        if (x1 < 0 || x1 >= buffer.width || y1 < 0 || y1 >= buffer.height) {
            return;
        }

        int offset = (x1 + buffer.width * y1) * 4;


        double[] color = genome.colors[((int) min(max(cc * 255.0, 0), 255))];
        buffer.accum[offset] += color[0];
        buffer.accum[offset + 1] += color[1];
        buffer.accum[offset + 2] += color[2];
        buffer.accum[offset + 3] += opacity;
    }

    private static class RenderBuffer {
        private final int width;
        private final int height;
        private final double[] accum; // (r, g, b, alpha)
        private final BufferedImage image;

        private RenderBuffer(int width, int height) {
            this.width = width;
            this.height = height;

            image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
            accum = new double[this.width * this.height * 4];
        }

        public void reset() {
            Arrays.fill(accum, 0);
        }
    }
    
    private static class RenderState {
        private double[] xyc = new double[3];
        private final FlamGenome genome;
        private final RenderBuffer buffer;
        private static final double PREFILTER_WHITE = 255;
        private final double scale;
        private final double ppux;
        private final double ppuy;
        public long samples = 0;
        private double viewHeight;
        private double viewWidth;
        private double viewLeft;
        private double viewBottom;
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

        public void reseed() {
            xyc[0] = crnd();
            xyc[1] = crnd();
            xyc[2] = rnd();
        }

        private FlamGenome.Xform pickRandomXform() {
            int k;
            if (genome.chaosEnabled) {
                k = genome.xformDistrib[lastxf][random.nextInt(FlamGenome.CHOOSE_XFORM_GRAIN)];
                lastxf = k + 1;
            } else {
                k = genome.xformDistrib[0][random.nextInt(FlamGenome.CHOOSE_XFORM_GRAIN)];
            }
            return genome.xforms.get(k);
        }


        private void renderHistogram() {
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
            double g = 1.0 / (genome.gamma / vib_gam_n);
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
                        alpha = flam3.flam3_calc_alpha(tmp, g, linrange);
                        ls = vibrancy * 256.0 * alpha / tmp;
                        if (alpha < 0.0) alpha = 0.0;
                        if (alpha > 1.0) alpha = 1.0;
                    }

                    double[] t = {cr, cg, cb, freq};
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

}