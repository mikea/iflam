package flam;

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

        Xform xform = result.xforms.get(Rnd.random.nextInt(result.xforms.size()));
        int i = Rnd.random.nextInt(6);
        xform.coefs[i] += Rnd.random.nextGaussian() * xform.coefs[i] / 5;

        return result;
    }

    @Override
    public void reset() {
    }
}
