package flam.renderers.ria;

import flam.Genome;
import flam.GenomeView;
import flam.RenderBuffer;
import flam.RenderState;
import flam.util.Rnd;
import flam.Xform;

/**
 */
class RIAState implements RenderState {
    double[] xyc = new double[3];
    final Genome genome;
/*
    public long samples = 0;
    double viewHeight;
    double viewWidth;
    double viewLeft;
    double viewBottom;
*/
    GenomeView view;

    public RIAState(Genome genome, RenderBuffer buffer) {
        this.genome = genome;
        this.view = new GenomeView(genome, buffer.getWidth(), buffer.getHeight());
        reseed();
    }


    public void reseed() {
        xyc[0] = Rnd.crnd();
        xyc[1] = Rnd.crnd();
        xyc[2] = Rnd.rnd();
    }


    public boolean applyXform(Xform xform) {
        return xform.applyTo(xyc, xyc);
    }

    @Override
    public Genome getGenome() {
        return genome;
    }
}
