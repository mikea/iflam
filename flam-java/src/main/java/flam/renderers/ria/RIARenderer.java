package flam.renderers.ria;

import flam.Genome;
import flam.RenderBuffer;
import flam.RenderState;
import flam.Renderer;
import flam.Xform;

import static flam.util.MyMath.*;

/**
 * Random iteration algorithm.
 */
public class RIARenderer implements Renderer {
    private int minimumSamples = 100000;

    @Override
    public RenderState newState(Genome genome, RenderBuffer buffer) {
        return new RIAState(genome, buffer);
    }

    @Override
    public RenderBuffer newBuffer(int width, int height) {
        return new RIABuffer(width, height);
    }

    @Override
    public void iterate(RenderState aState, RenderBuffer aBuffer, double allottedTimeMs) {
        RIAState state = (RIAState) aState;
        RIABuffer buffer = (RIABuffer) aBuffer;

        state.reseed();

        long start = System.currentTimeMillis();
        Genome genome = state.genome;

        double rotate1 = 0;
        double rotate2 = 0;

        if (genome.getRotate() != 0) {
            rotate1 = cos(genome.getRotate() * 2 * PI / 360.0);
            rotate2 = sin(genome.getRotate() * 2 * PI / 360.0);
        }

        double[] xyc2 = new double[3];

        int fuse = 5;
        int consequentErrors = 0;

        genome.createXformDistrib();

        int i = -4 * fuse;

        for (; ; ++i) {
            Xform xform = genome.pickRandomXform();
            if (!state.applyXform(xform)) {
                consequentErrors++;
                System.out.println("consequentErrors = " + consequentErrors);

                if (consequentErrors < 5) {
                    continue;
                }
            }

            consequentErrors = 0;

            if (i >= 0) {
                double[] xyc = state.xyc;
                double opacity = xform.getOpacity();

                if (opacity != 1.0) {
                    opacity = flam3.adjust_percentage(xform.getOpacity());
                }

                if (genome.finalxform != null) {
                    genome.finalxform.applyTo(xyc, xyc2);
                } else {
                    xyc2[0] = xyc[0];
                    xyc2[1] = xyc[1];
                    xyc2[2] = xyc[2];
                }
                xyc = xyc2;

                if (genome.getRotate() != 0) {
                    //todo: optimize
                    double x1 = xyc[0] - genome.center[0];
                    double y1 = xyc[1] - genome.center[1];
                    double x = rotate1 * x1 - rotate2 * y1 + genome.center[0];
                    double y = rotate2 * x1 + rotate1 * y1 + genome.center[1];
                    xyc[0] = x;
                    xyc[1] = y;
                }

                buffer.updateHistogram(genome, state.view, xyc[0], xyc[1], xyc[2], opacity);
            }

            if (i % 1000 == 0 && i > minimumSamples) {
                if ((System.currentTimeMillis() - start) > allottedTimeMs) {
                    return;
                }
            }
        }

    }

    @Override
    public void render(RenderState state, RenderBuffer buffer) {
        ((RIABuffer) buffer).renderHistogram(state.getGenome());
    }

    public void setMimimumSamples(int samples) {
        this.minimumSamples = samples;
    }
}
