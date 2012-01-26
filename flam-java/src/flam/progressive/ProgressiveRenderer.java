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

        buffer.startCopy();
        for (int y = 0; y < buffer.getHeight(); ++y) {
            for (int x = 0; x < buffer.getWidth(); ++x) {
                double d = buffer.get(x, y);

                v1[0] = x;
                v1[1] = y;

                view.viewToCoords(v1, c1);

                for (int i = 0; i < size; ++i) {
                    Xform xform = xforms.get(i);
                    xform.applyTo(c1, c2);
                    if (view.coordsToView(c2, v2)) {
                        buffer.add(v2[0], v2[1], d * xform.weight * xform.getOpacity() * 100);
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
