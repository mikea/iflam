package flam.renderers.escape;

import flam.Genome;
import flam.GenomeView;
import flam.RenderState;

/**
 */
public class EscapeState implements RenderState {
    public GenomeView view;
    private final Genome genome;

    public EscapeState(Genome genome, int width, int height) {
        this.genome = genome;
        genome.createXformDistrib();
        view = new GenomeView(genome, width, height);
    }

    @Override
    public Genome getGenome() {
        return genome;
    }
}
