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
    private static final Random random = new Random();

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
        state = new RenderState(getWidth(), getHeight());

    }


    @Override
    protected void paintComponent(Graphics graphics) {
        if (state == null || state.width !=  getWidth() || state.height != getHeight()) {
            resetState();
        }
        
        for (int i = 0; i < 500000; ++i) {
            state.applyXform(pickRandomXform());

            if (genome.finalxform != null) {
                state.applyXform(genome.finalxform);
            }

            if (i > 20) {
                updateHistogram(state.x, state.y, state.cc);
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
        double top = 1, bottom = -1, left = -1, right = 1;

        if (x < left || x > right || y < bottom || y > top) {
            return;
        }

        x -= genome.center[0];
        y -= genome.center[0];

        double height = top - bottom;
        double width = right - left;


        int x1 = (int) ((x - left) * state.width / width);
        int y1 = (int) ((y - bottom) * state.height / height);

        if (x1 < 0 || x1 >= state.width || y1 < 0 || y1 >= state.height) {
            return;
        }
        
        int offset = x1 + state.width * y1;

        ++state.freqHistogram[offset];

        double[] color = genome.colors[((int) Math.min(Math.max(cc * 255.0, 0), 255))];
        state.colorHistogram[offset * 3] = (state.colorHistogram[(offset * 3)] + color[0]) / 2;
        state.colorHistogram[offset * 3 + 1] = (state.colorHistogram[offset * 3 + 1] + color[1]) / 2;
        state.colorHistogram[offset * 3 + 2] = (state.colorHistogram[offset * 3 + 2] + color[2]) / 2;
    }

    private void blt(Graphics graphics) {
        state.renderHistogram();
        graphics.drawImage(state.image, 0, 0, null);
    }


    private static class RenderState {
        private double x;
        private double y;
        private double cc;
        private final int width;
        private final int height;
        private final int[] freqHistogram;
        private final double[] colorHistogram;
        private final BufferedImage image;

        public RenderState(int width, int height) {
            this.width = width;
            this.height = height;

            this.x = crnd();
            this.y = crnd();
            this.cc = rnd();

            image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
            freqHistogram = new int[this.width * this.height];
            colorHistogram = new double[this.width * this.height * 3];
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

            double maxFreqLog = Math.log(maxFreq);
            for (int x = 0; x < width; ++x) {
                for (int y = 0; y < height; ++y) {
                    int offset = x + width * y;
                    if (freqHistogram[offset] == 0) continue;

                    double alpha = Math.log(freqHistogram[offset]) / maxFreqLog;
                    alpha = Math.pow(alpha, 1 / GAMMA);
                    double r = colorHistogram[offset * 3];
                    double g = colorHistogram[offset * 3 + 1];
                    double b = colorHistogram[offset * 3 + 2];

                    int rgb = ((int) (r * alpha * 255) << 16) |
                            ((int) (g * alpha * 255) << 8) |
                            (int) (b * alpha * 255);


                    image.setRGB(x, y, rgb);
                }
            }
        }

        public void applyXform(FlamGenome.Xform xform) {
            final double a = xform.coefs[0];
            final double b = xform.coefs[2];
            final double c = xform.coefs[4];
            final double d = xform.coefs[1];
            final double e = xform.coefs[3];
            final double f = xform.coefs[5];

            {   // Affine transform
                double x1, y1;
                x1 = x * a + y * b + c;
                y1 = x * d + y * e + f;

                x = x1;
                y = y1;
            }

            {   // Nonlinear transform
                double x2 = 0, y2 = 0;

                double r2 = x * x + y * y;
                double r = Math.sqrt(r2);
                double theta = Math.atan(x / y);

                for (int j = 0; j < FlamGenome.variationNames.length; ++j) {
                    double dx;
                    double dy;

                    if (xform.variations[j] == 0) continue;

                    switch (j) {
                        default:
                            throw new IllegalArgumentException("Unimplemented variation: " + j + " : " + FlamGenome.variationNames[j]);
                        case 0: // linear
                            dx = x;
                            dy = y;
                            break;
                        case 1: // sinusoidal
                            dx = sin(x);
                            dy = sin(y);
                            break;
                        case 2: // spherical
                            dx = x / r2;
                            dy = y / r2;
                            break;
                        case 3: // swirl
                            dx = x * sin(r2) - y * cos(r2);
                            dy = x * cos(r2) + y * sin(r2);
                            break;
                        case 6: // handkerchief
                            dx = r * sin(theta + r);
                            dy = r * cos(theta - r);
                            break;
                        case 11: // diamond
                            dx = sin(theta) * cos(r);
                            dy = cos(theta) * sin(r);
                            break;
                        case 13: // julia
                            double omega = random.nextBoolean() ? 0 : PI;
                            dx = sqrt(r) * cos(theta / 2 + omega);
                            dy = sqrt(r) * sin(theta / 2 + omega);
                            break;
                        case 15: // waves
                            dx = x + b * sin(y / (c * c));
                            dy = y + e * sin(x / (f * f));
                            break;
                        case 27: // eyefish
                            dx = 2 * x / (r + 1);
                            dy = 2 * y / (r + 1);
                            break;
                        case 29: // cylinder
                            dx = sin(x);
                            dy = y;
                            break;
                    }

                    x2 += xform.variations[j] * dx;
                    y2 += xform.variations[j] * dy;
                }

                x = x2;
                y = y2;
            }

            double colorSpeed = xform.colorSpeed;
            cc = cc * (1-colorSpeed)  + xform.color * colorSpeed;
        }
    }

}