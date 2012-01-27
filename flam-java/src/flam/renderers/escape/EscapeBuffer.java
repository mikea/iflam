package flam.renderers.escape;

import flam.RenderBuffer;
import flam.util.Colors;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 */
public class EscapeBuffer implements RenderBuffer {
    private final int width;
    private final int height;
    private BufferedImage image;

    public EscapeBuffer(int width, int height) {
        this.width = width;
        this.height = height;
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
        image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
    }

    @Override
    public Image getImage() {
        return image;
    }

    public void set(int x, int y, int i) {
        image.setRGB(x, y, Colors.packRgb(i, i, i));
    }
}
