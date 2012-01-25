package flam;

import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
*/
class RenderBuffer {
    final int width;
    final int height;
    final double[] accum; // (r, g, b, alpha)
    final double[] densityEstimate; // (estimated densityEstimate)
    final BufferedImage image;
    final BufferedImage filteredImage;

    RenderBuffer(int width, int height) {
        this.width = width;
        this.height = height;

        image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
        filteredImage = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
        accum = new double[this.width * this.height * 4];
        densityEstimate = new double[this.width * this.height];
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
}
