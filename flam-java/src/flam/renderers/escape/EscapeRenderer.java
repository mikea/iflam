package flam.renderers.escape;

import flam.Genome;
import flam.GenomeView;
import flam.RenderBuffer;
import flam.RenderState;
import flam.Renderer;

/**
 */
public class EscapeRenderer implements Renderer {
    @Override
    public RenderState newState(Genome genome, RenderBuffer buffer) {
        return new EscapeState(genome, buffer.getWidth(), buffer.getHeight());
    }

    @Override
    public RenderBuffer newBuffer(int width, int height) {
        return new EscapeBuffer(width, height);
    }

    @Override
    public void iterate(RenderState aState, RenderBuffer aBuffer, double allottedTimeMs) {
        EscapeState state = (EscapeState) aState;
        EscapeBuffer buffer = (EscapeBuffer) aBuffer;
        
        int v[] = new int[2];
        double c[] = new double[3];

        GenomeView view = state.view;
        
        for (int y = 0; y < buffer.getHeight(); ++y) {
            for (int x = 0; x < buffer.getWidth(); ++x) {
                v[0] = x;
                v[1] = y;
                view.viewToCoords(v, c);

                int i;

                for (i = 0; i < 255; ++i) {
                    if ((c[0] * c[0] + c[1] * c[1]) > 4) {
                        break;
                    }

                    state.getGenome().pickRandomXform().applyTo(c, c);
                }

                buffer.set(x, y, i);
            }
        }

        System.out.println("done");
    }

    @Override
    public void render(RenderState state, RenderBuffer buffer) {
    }
}
