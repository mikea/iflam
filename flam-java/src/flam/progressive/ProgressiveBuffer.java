package flam.progressive;

import flam.Genome;
import flam.RenderBuffer;
import flam.util.Colors;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 */
class ProgressiveBuffer implements RenderBuffer {
    private final int width;
    private final int height;

    private double[] buffer1;
    private double[] buffer2;
    private final BufferedImage image;
        
    public ProgressiveBuffer(int width, int height) {
        this.width = width;
        this.height = height;
        this.buffer1 = new double[width * height];
        this.buffer2 = new double[width * height];
        Arrays.fill(buffer1, 1.0);
        image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void reset() {
        Arrays.fill(buffer1, 1.0);
        Arrays.fill(buffer2, 1.0);
    }

    @Override
    public Image getImage() {
        return image;
    }

    public double get(int x, int y) {
        return buffer1[x + y * width];
    }

    public void endCopy() {
        double[] tmp = buffer1;
        buffer1 = buffer2;
        buffer2 = tmp;
    }

    public void startCopy() {
        Arrays.fill(buffer2, 0);
    }

    public void add(int x, int y, double s) {
        double d = buffer2[x + y * width] + s;
        if (d > 1) d = 1;
        buffer2[x + y * width] = d;
    }

    public void render(Genome genome) {
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int d = (int) (buffer1[x + y *width] * 255.0);

//                double[] color = genome.getColor(d);
//                image.setRGB(x, y, Colors.packRgb(255 * color[0], 255 * color[1], 255 * color[2]));
                image.setRGB(x, y, Colors.packRgb(d, d, d));
            }
        }
    }
}
