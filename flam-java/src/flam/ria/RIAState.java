package flam.ria;

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
    private int lastxf = 0;
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

    Xform pickRandomXform() {
        int k;
        if (genome.chaosEnabled) {
            k = genome.xformDistrib[lastxf][Rnd.random.nextInt(Genome.CHOOSE_XFORM_GRAIN)];
            lastxf = k + 1;
        } else {
            k = genome.xformDistrib[0][Rnd.random.nextInt(Genome.CHOOSE_XFORM_GRAIN)];
        }
        return genome.xforms.get(k);
    }


    public boolean applyXform(Xform xform) {
        return xform.applyTo(xyc, xyc);
    }

    @Override
    public Genome getGenome() {
        return genome;
    }
}
