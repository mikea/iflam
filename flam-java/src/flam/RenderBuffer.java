package flam;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
*/
class RenderBuffer {
    final int width;
    final int height;
    final double[] accum; // (r, g, b, alpha)
    final BufferedImage image;
    final BufferedImage filteredImage;
    final List<double[][]> filters = new ArrayList<double[][]>();

    RenderBuffer(int width, int height) {
        this.width = width;
        this.height = height;

        image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
        filteredImage = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
        accum = new double[this.width * this.height * 4];
    }

    public void reset() {
        Arrays.fill(accum, 0);
    }

    public double[][] getFilter(int filterWidth) {
        while (filters.size() <= filterWidth) {
            int width = filters.size();
            if (width <= 2 || width % 2 == 0) {
                filters.add(new double[0][]);
                continue;
            }

            double sigma = filterWidth / 6.0;
            
            double[][] filter = new double[filterWidth][];

            //double cx = filterWidth / 2.0;
            //double cy = filterWidth / 2.0;

            for (int x = 0; x < filterWidth; x++) {
                filter[x] = new double[filterWidth];
            }
            
            for (int x = 0; x < filterWidth; x++) {
                for (int y = 0; y< filterWidth; y++) {
                    double dx = (x - filterWidth / 2);
                    double dy = (y - filterWidth / 2);
                    
                    filter[x][y] = MyMath.exp(- (dx*dx + dy*dy)/(2 * sigma * sigma)) / (2 * MyMath.PI * sigma * sigma);
                }
            }
            
            filters.add(filter);
        }
        
        return filters.get(filterWidth);
    }
}
