package flam;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Random;

/**
 * @author mike
 */
public class FlamComponent extends JComponent {
    private static final Random random = new Random();

    private static final int V_LEN = 6;

    private static final int F_LEN = 6 + V_LEN;
    private static final int WIDTH = 300;
    private static final int HEIGHT = 300;
    private static final double GAMMA = 1.5;

    // function
    private int numberOfFunctions;
    private double[] flamFunctionCoefficients;
    private double[] flamFunctionColors;

    // state
    private int[] freqHistogram = new int[WIDTH * HEIGHT];
    private double[] colorHistogram = new double[WIDTH * HEIGHT * 3];
    private double lastX = 0;
    private double lastY = 0;
    private double lastR = 0;
    private double lastG = 0;
    private double lastB = 0;
    private final BufferedImage buffer;

    public FlamComponent() {
        setFocusable(true);

        buffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        resetAll();


        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                if (keyEvent.getKeyChar() == 'c') {
                    resetColors();
                } else {
                    resetAll();
                }
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

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(WIDTH, HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(WIDTH, HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(WIDTH, HEIGHT);
    }

    private void resetAll() {
        numberOfFunctions = random.nextInt(10) + 1;
        flamFunctionCoefficients = crndVector(F_LEN * numberOfFunctions);
        resetColors();
        resetState();
    }

    private void resetState() {
        Arrays.fill(colorHistogram, 0.0);
        Arrays.fill(freqHistogram, 0);

        lastX = crnd();
        lastY = crnd();
        lastR = 0;
        lastG = 0;
        lastB = 0;
    }

    private void resetColors() {
        flamFunctionColors = rndVector(3 * numberOfFunctions);
    }


    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        double x = lastX;
        double y = lastY;
        double cr = lastR, cg = lastG, cb = lastB; // r,g,b

        for (int i = 0; i < 500000; ++i) {
            int k = Math.abs(random.nextInt() % numberOfFunctions);
            int idx = k * F_LEN;
            final double a = flamFunctionCoefficients[idx + 0];
            final double b = flamFunctionCoefficients[idx + 1];
            final double c = flamFunctionCoefficients[idx + 2];
            final double d = flamFunctionCoefficients[idx + 3];
            final double e = flamFunctionCoefficients[idx + 4];
            final double f = flamFunctionCoefficients[idx + 5];

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

                for (int j = 0; j < V_LEN; ++j) {
                    double dx;
                    double dy;

                    switch (j) {
                        default:
                            throw new IllegalArgumentException();
                        case 0:
                            dx = x; dy = y; break;
                        case 1:
                            dx = Math.sin(x); dy = Math.sin(y); break;
                        case 2:
                            dx = x / r2; dy = y / r2; break;
                        case 3:
                            dx = x * Math.sin(r2) - y * Math.cos(r2); dy = x * Math.cos(r2) + y * Math.sin(r2); break;
                        case 4:
                            dx = (x - y) * (x + y) / r; dy = 2 * x * y / r; break;
                        case 5:
                            dx = theta / Math.PI; dy = r - 1; break;
                        case 6:
                            dx = r * Math.sin(theta + r); dy = r * Math.cos(theta - r); break;
                    }

                    x2 += flamFunctionCoefficients[idx + 6 + j] * dx;
                    y2 += flamFunctionCoefficients[idx + 6 + j] * dy;
                }

                x = x2;
                y = y2;
            }

            cr = (cr + flamFunctionColors[k * 3]) / 2;
            cg = (cg + flamFunctionColors[k * 3 + 1]) / 2;
            cb = (cb + flamFunctionColors[k * 3 + 2]) / 2;

            if (i > 20) {
                updateHistogram(x, y, cr, cg, cb);
            }
        }

        blt(graphics);

        lastX = x;
        lastY = y;
        lastR = cr;
        lastG = cg;
        lastB = cb;

        repaint();
    }

    private double crnd() {
        return random.nextDouble() * 2 - 1;
    }

    private void updateHistogram(double x, double y, double cr, double cg, double cb) {
        if (x < 0 || x > 1 || y < 0 || y > 1) {
            return;
        }
        float x1 = (float) (x * WIDTH);
        float y1 = (float) (y * HEIGHT);

        int offset = (int) x1 + HEIGHT * (int) y1;

        if (offset > freqHistogram.length) {
            return;
        }
        ++freqHistogram[offset];
        colorHistogram[offset * 3] = (colorHistogram[offset * 3] + cr) / 2;
        colorHistogram[offset * 3 + 1] = (colorHistogram[offset * 3 + 1] + cg) / 2;
        colorHistogram[offset * 3 + 2] = (colorHistogram[offset * 3 + 2] + cb) / 2;
    }

    private void blt(Graphics graphics) {
        {
            Graphics bg = buffer.getGraphics();
            bg.setColor(Color.BLACK);
            bg.fillRect(0, 0, getWidth(), getHeight());
        }

        int maxFreq = 0;
        for (int i = 0; i < freqHistogram.length; ++i) {
            if (maxFreq < freqHistogram[i]) {
                maxFreq = freqHistogram[i];
            }
        }

        double maxFreqLog = Math.log(maxFreq);
        for (int x = 0; x < WIDTH; ++x) {
            for (int y = 0; y < HEIGHT; ++y) {
                int offset = x + HEIGHT * y;
                double alpha = Math.log(freqHistogram[offset]) / maxFreqLog;
                if (alpha > 1) {
                    System.out.println();
                }
                alpha = Math.pow(alpha, 1 / GAMMA);
                double r = colorHistogram[offset * 3];
                double g = colorHistogram[offset * 3 + 1];
                double b = colorHistogram[offset * 3 + 2];

                int rgb = ((int) (r * alpha * 255) << 16) |
                        ((int) (g * alpha * 255) << 8) |
                        (int) (b * alpha * 255);


                buffer.setRGB(x, y, rgb);
            }
        }

        graphics.drawImage(buffer, 0, 0, null);
    }


    private double[] crndVector(int len) {
        double[] result = new double[len];

        for (int i = 0; i < len; ++i) {
            result[i] = random.nextDouble() * 2 - 1.0;
        }

        return result;
    }

    private double[] rndVector(int len) {
        double[] result = new double[len];

        for (int i = 0; i < len; ++i) {
            result[i] = random.nextDouble();
        }

        return result;
    }
}