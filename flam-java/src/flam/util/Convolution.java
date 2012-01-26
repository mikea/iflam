package flam.util;

import java.awt.image.BufferedImage;

/**
 */
public class Convolution {
    public static void convolve(BufferedImage src, BufferedImage dst, FilterProvider filterProvider) {
        int height = src.getHeight();
        int width = src.getWidth();

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                double[][] filter = filterProvider.getFilter(x, y);
                if (filter == null) {
                    dst.setRGB(x, y, src.getRGB(x, y));
                    continue;
                }

                int filterWidth = filter.length;
                if (filterWidth <= 1) {
                    dst.setRGB(x, y, src.getRGB(x, y));
                } else {
                    double sr = 0;
                    double sg = 0;
                    double sb = 0;

                    for (int fx = 0; fx < filterWidth; fx++) {
                        for (int fy = 0; fy < filterWidth; fy++) {
                            int xx = x + fx - filterWidth / 2;
                            int yy = y + fy - filterWidth / 2;

                            if (xx < 0 || yy < 0 || xx >= width || yy >= height) {
                                continue;
                            }

                            int srcRgb = src.getRGB(xx, yy);
                            double r = (srcRgb & 0xFF0000) >> 16;
                            double g = (srcRgb & 0xFF00) >> 8;
                            double b = (srcRgb & 0xFF);
                            sr += r * filter[fx][fy];
                            sg += g * filter[fx][fy];
                            sb += b * filter[fx][fy];
                        }
                    }

                    int rgb = ((int) (sr) << 16) | ((int) (sg) << 8) | (int) (sb);
                    dst.setRGB(x, y, rgb);
                }
            }
        }
    }

    public static void convolve(View2D src, View2D dst, double[][] filter) {
        int filterWidth = filter.length;
        int width = src.getWidth();
        int height = src.getHeight();

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                double d = 0;

                for (int fx = 0; fx < filterWidth; fx++) {
                    for (int fy = 0; fy < filterWidth; fy++) {
                        int xx = x + fx - filterWidth / 2;
                        int yy = y + fy - filterWidth / 2;

                        if (xx < 0 || yy < 0 || xx >= width || yy >= height) {
                            continue;
                        }

                        d += src.get(xx, yy) * filter[fx][fy];
                    }
                }

                dst.set(x, y, d);
            }
        }
    }

    interface FilterProvider {
        double[][] getFilter(int x, int y);
    }
}
