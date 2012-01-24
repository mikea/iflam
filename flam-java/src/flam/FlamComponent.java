package flam;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Random;

import static flam.MyMath.*;

/**
 * @author mike
 */
public class FlamComponent extends JComponent {
    public static final Random random = new Random();

    // state
    private Genome genome;
    private RenderState state;
    private RenderBuffer buffer;
    private GenomeProvider provider;
    private double fps = 10;
    public long iterTime = 0;
    public long renderTime = 0;
    public long samples = 0;
    public int batches = 0;
    private int minimumSamples = 25000;

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

    public void setMinimumSamples(int minimumSamples) {
        this.minimumSamples = minimumSamples;
    }

    private void reset() {
        this.provider.reset();
        batches = 0;
        renderTime = 0;
        iterTime = 0;
        samples = 0;
    }

    public void setFps(double fps) {
        this.fps = fps;
    }

    private void resetState(Genome newGenome) {
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
        samples += iterateBatch(start);
        long batchFinish = System.currentTimeMillis();
        iterTime += (batchFinish - start);

        state.renderHistogram();

        long bltFinish = System.currentTimeMillis();

        renderTime += (bltFinish - batchFinish);
        batches++;

        if (batches % 10 == 0) {
            System.out.println("samples = " + samples);
        }
//        System.out.println(i + " : " + (batchFinish - start) + " : " + (bltFinish - batchFinish));

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
            Xform xform = state.pickRandomXform();
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
                    opacity = flam3.adjust_percentage(xform.opacity);
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

            if (i % 1000 == 0 && i > minimumSamples) {
                if ((System.currentTimeMillis() - start + avgRenderTime) > allotedTimeMs) {
                    return i;
                }
            }
        }
    }

    private void updateGenome() {
        Genome newGenome = provider.getGenome();
        if (state == null || buffer.width != getWidth() || buffer.height != getHeight() || state.genome != newGenome) {
            resetState(newGenome);
        }
    }

    public static double crnd() {
        return random.nextDouble() * 2 - 1;
    }

    static double rnd() {
        return random.nextDouble();
    }

    private void updateHistogram(double x, double y, double cc, double opacity) {
        double ws = buffer.width / state.viewWidth;
        double hs = buffer.height / state.viewHeight;

        int x1 = (int) ((x - state.viewLeft) * ws + 0.5);
        int y1 = (int) ((y - state.viewBottom) * hs + 0.5);

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

}