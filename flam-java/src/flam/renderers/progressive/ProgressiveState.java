package flam.renderers.progressive;

import flam.Genome;
import flam.GenomeView;
import flam.RenderBuffer;
import flam.RenderState;

class ProgressiveState implements RenderState {
    private final Genome genome;
    private final GenomeView view;

    public ProgressiveState(Genome genome, RenderBuffer buffer) {
        this.genome = genome;
        this.view = new GenomeView(genome, buffer.getWidth(), buffer.getHeight());
    }

    @Override
    public Genome getGenome() {
        return genome;
    }

    public GenomeView getView() {
        return view;
    }
}
