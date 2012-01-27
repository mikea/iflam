package flam;

import flam.util.DeepCopy;
import flam.util.Rnd;

/**
 * @author mike
 */
public class RandomDisturbanceProvider implements GenomeProvider {
    private Genome genome;

    public RandomDisturbanceProvider(Genome genome) {
        this.genome = genome;
    }

    @Override
    public Genome getGenome() {
        Genome result = (Genome) DeepCopy.copy(genome);

        Xform xform = result.xforms.get(Rnd.irnd(result.xforms.size()));
        int i = Rnd.irnd(6);
        xform.coefs[i] += Rnd.grnd() * xform.coefs[i] / 5;

        return result;
    }

    @Override
    public void reset() {
    }
}
