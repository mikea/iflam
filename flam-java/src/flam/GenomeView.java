package flam;

import static flam.util.MyMath.pow;

/**
 */
public class GenomeView {
    private final Genome genome;
    private final int width;
    private final int height;
    private final double viewLeft;
    private final double ws;
    private final double viewBottom;
    private final double hs;

    public GenomeView(Genome genome, int width, int height) {
        this.genome = genome;
        this.width = width;
        this.height = height;

        double genomeHeight = genome.size[1];
        double genomeWidth = genomeHeight * (width * 1.0 / height);
        double scale = pow(2.0, genome.zoom);
        double ppux = genome.pixelsPerUnit * scale;
        double ppuy = genome.pixelsPerUnit * scale;

        viewLeft = genome.center[0] - genomeWidth / ppux / 2.0;
        viewBottom = genome.center[1] - genomeHeight / ppuy / 2.0;

        double viewHeight = genomeHeight / ppuy;
        double viewWidth = genomeWidth / ppux;

        ws = width / viewWidth;
        hs = height / viewHeight;
    }

    public void viewToCoords(int[] v, double[] c) {
        c[0] = v[0] / ws + viewLeft;
        c[1] = v[1] / hs + viewBottom;
    }

    public boolean coordsToView(double[] c, int[] v) {
        int x = (int) ((c[0] - viewLeft) * ws + 0.5);
        int y = (int) ((c[1] - viewBottom) * hs + 0.5);
        v[0] = x;
        v[1] = y;

        if (x < 0 || x >= width || y < 0 || y >= height) {
            return false;
        }
        
        return true;
        
    }
}
