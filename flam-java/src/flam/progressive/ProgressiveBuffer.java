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
        this.buffer1 = new double[width * height * 3];
        this.buffer2 = new double[width * height * 3];
        Arrays.fill(buffer1, 1.0);
        image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
    }

    private int getOffset(int x, int y) {
        return (x + y * width) * 3;
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

    public void get(int x, int y, double[] d) {
        int offset = getOffset(x, y);
        d[0] = buffer1[offset];
        d[1] = buffer1[offset + 1];
        d[2] = buffer1[offset + 2];
    }

    public void add(int x, int y, double[] d) {
        int offset = getOffset(x, y);

        {
            double density = buffer2[offset] + d[0];
            if (density > 1) density = 1;
            buffer2[offset] = density;
        }
        {
            double density = buffer2[offset + 1] + d[1];
            if (density > 1) density = 1;
            buffer2[offset + 1] = density;
        }
        {
            double density = buffer2[offset + 2] + d[2];
            if (density > 1) density = 1;
            buffer2[offset + 2] = density;
        }
    }

    public void render(Genome genome) {
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int offset = getOffset(x, y);
                int d1 = (int) (buffer1[offset] * 255.0);
                int d2 = (int) (buffer1[offset + 1] * 255.0);
                int d3 = (int) (buffer1[offset + 2] * 255.0);

                // double[] color = genome.getColor(c);
//                image.setRGB(x, y, Colors.packRgb(255 * color[0], 255 * color[1], 255 * color[2]));
                image.setRGB(x, y, Colors.packRgb(d1, d2, d3));
            }
        }
    }

    public void endCopy() {
        double[] tmp = buffer1;
        buffer1 = buffer2;
        buffer2 = tmp;
    }

    public void startCopy() {
        Arrays.fill(buffer2, 0);
    }

}
