package flam.renderers.progressive;

import flam.*;
import flam.util.Rnd;

import java.util.List;

/**
 */
public class ProgressiveRenderer implements Renderer {
    @Override
    public RenderState newState(Genome genome, RenderBuffer buffer) {
        return new ProgressiveState(genome, buffer);
    }

    @Override
    public RenderBuffer newBuffer(int width, int height) {
        return new ProgressiveBuffer(width, height);
    }

    @Override
    public void iterate(RenderState aState, RenderBuffer aBuffer, double allottedTimeMs) {
//        aBuffer.reset();
        iterateImpl((ProgressiveState) aState, (ProgressiveBuffer) aBuffer);
//        iterateImpl((ProgressiveState) aState, (ProgressiveBuffer) aBuffer);
//        iterateImpl((ProgressiveState) aState, (ProgressiveBuffer) aBuffer);
//        iterateImpl((ProgressiveState) aState, (ProgressiveBuffer) aBuffer);
    }

    private void iterateImpl(ProgressiveState state, ProgressiveBuffer buffer) {

        Genome genome = state.getGenome();
        List<Xform> xforms = genome.xforms;
        int size = xforms.size();

        GenomeView view = state.getView();

        double[] v1 = new double[2];
        double[] c1 = new double[3];
        double[] c2 = new double[3];
        int[] v2 = new int[2];

        double[] d1 = new double[3];
        double[] d2 = new double[3];

        buffer.startCopy();

        {   // fill background
/*
            for (int y = 0; y < buffer.getHeight(); ++y) {
                for (int x = 0; x < buffer.getWidth(); ++x) {
                    v1[0] = x;
                    v1[1] = y;
                    view.viewToCoords(v1, c1);

                    double dx = genome.center[0] - c1[0];
                    double dy = genome.center[1] - c1[1];
                    double r = MyMath.sqrt(dx * dx + dy * dy);
                    double c = 1 - r;
                    if (c < 0) c = 0;
                    if (c > 1) c = 1;

                    d1[0] = 0.5 * c;
                    d1[1] = 0.1 * c;
                    d1[2] = 0.5 * c;

                    buffer.add(x, y, d1);
                }
            }
*/
        }

        int supersamples = 10;
        for (int y = 0; y < buffer.getHeight(); ++y) {
            for (int x = 0; x < buffer.getWidth(); ++x) {
                buffer.get(x, y, d1);
                
                if (d1[0] == 0 && d1[1] == 0 && d1[2] == 0) {
                    continue;
                }

                for (int sample = 0; sample < supersamples; ++sample) {
                    v1[0] = x;
                    v1[1] = y;

                v1[0] += Rnd.rnd() - 0.5;
                v1[1] += Rnd.rnd() - 0.5;
                    view.viewToCoords(v1, c1);

                    for (int i = 0; i < size; ++i) {
                        Xform xform = xforms.get(i);
                        xform.applyTo(c1, c2);

                        if (view.coordsToView(c2, v2)) {
                            double[] color = genome.getColor(i);
                            double f = 1.0/size / supersamples;

    /*
                            d2[0] = f * color[0] * d1[0] * xform.weight * xform.getOpacity();
                            d2[1] = f * color[1] * d1[1] * xform.weight * xform.getOpacity();
                            d2[2] = f * color[2] * d1[2] * xform.weight * xform.getOpacity();
    */
                            d2[0] = f * d1[0];
                            d2[1] = f * d1[1];
                            d2[2] = f * d1[2];
                            buffer.add(v2[0], v2[1], d2);
                        }
                    }
                }
            }
        }

        buffer.endCopy();
    }

    @Override
    public void render(RenderState state, RenderBuffer buffer) {
        ((ProgressiveBuffer) buffer).render(state.getGenome());
    }
}
