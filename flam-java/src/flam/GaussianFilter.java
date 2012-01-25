package flam;

import java.awt.image.Kernel;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class GaussianFilter {
    final static List<double[][]> filters = new ArrayList<double[][]>();

    public static synchronized double[][] getFilter(int filterWidth) {
        if (filterWidth % 2 == 0) {
            throw new IllegalArgumentException();
        }
        while (filters.size() <= filterWidth) {
            int width = filters.size();
            if (width <= 2 || width % 2 == 0) {
                filters.add(new double[0][]);
                continue;
            }

            filters.add(computeFilter(filterWidth, filterWidth / 3.0));
        }

        return filters.get(filterWidth);
    }

    private static double[][] computeFilter(int filterWidth, double sigma) {
        double[][] filter = new double[filterWidth][];
        for (int x = 0; x < filterWidth; x++) {
            filter[x] = new double[filterWidth];
        }

        for (int x = 0; x < filterWidth; x++) {
            for (int y = 0; y < filterWidth; y++) {
                double dx = (x - filterWidth / 2);
                double dy = (y - filterWidth / 2);

//                    filter[x][y] = 1.0 / (filterWidth * filterWidth);
                filter[x][y] = MyMath.exp(-(dx * dx + dy * dy) / (2 * sigma * sigma)) / (2 * MyMath.PI * sigma * sigma);
            }
        }
        return filter;
    }

    public static double[][] getFilter(double radius) {
        double sigma = radius / 3;
        int filterWidth = (int) radius;
        if (filterWidth % 2 == 0) {
            filterWidth++;
        }

        return computeFilter(filterWidth, sigma);
    }

    public static Kernel makeKernel(float radius) {
        int r = (int) Math.ceil(radius);
        int rows = r * 2 + 1;
        float[] matrix = new float[rows * rows];
        float sigma = radius / 3;
        float sigma22 = 2 * sigma * sigma;
        float sigmaPi2 = (float) (2 * Math.PI * sigma);
        float sqrtSigmaPi2 = (float) Math.sqrt(sigmaPi2);
        float radius2 = radius * radius;
        float total = 0;
        int index = 0;
        for (int col = -r; col <= r; col++) {
            for (int row = -r; row <= r; row++) {
                float distance = row * row + col * col;
                if (distance > radius2)
                    matrix[index] = 0;
                else
                    matrix[index] = (float) Math.exp(-(distance) / sigma22) / sqrtSigmaPi2;
                total += matrix[index];
                index++;
            }
        }
        for (int i = 0; i < matrix.length; i++)
            matrix[i] /= total;

        return new Kernel(rows, rows, matrix);
    }
}
