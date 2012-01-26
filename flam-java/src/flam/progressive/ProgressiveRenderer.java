package flam.progressive;

import flam.Genome;
import flam.GenomeView;
import flam.RenderBuffer;
import flam.RenderState;
import flam.Renderer;
import flam.Xform;

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
        ProgressiveBuffer buffer = (ProgressiveBuffer) aBuffer;
        ProgressiveState state = (ProgressiveState) aState;

        Genome genome = state.getGenome();
        List<Xform> xforms = genome.xforms;
        int size = xforms.size();
        
        GenomeView view = state.getView();

        int[] v1 = new int[2];
        double[] c1 = new double[3];
        double[] c2 = new double[3];
        int[] v2 = new int[2];
        
        double[] d1 = new double[3];
        double[] d2 = new double[3];

        buffer.startCopy();
        for (int y = 0; y < buffer.getHeight(); ++y) {
            for (int x = 0; x < buffer.getWidth(); ++x) {
                buffer.get(x, y, d1);

                v1[0] = x;
                v1[1] = y;

                view.viewToCoords(v1, c1);

                for (int i = 0; i < size; ++i) {
                    Xform xform = xforms.get(i);
                    xform.applyTo(c1, c2);
                    if (view.coordsToView(c2, v2)) {
                        double[] color = genome.getColor((int) (xform.color * 255));
                        double f = 1.0 * 20;

                        d2[0] = f * color[0] * d1[0] * xform.weight * xform.getOpacity();
                        d2[1] = f * color[1] * d1[1] * xform.weight * xform.getOpacity();
                        d2[2] = f * color[2] * d1[2] * xform.weight * xform.getOpacity();
                        buffer.add(v2[0], v2[1], d2);
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
