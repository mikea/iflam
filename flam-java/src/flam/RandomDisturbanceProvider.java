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

        Xform xform = result.xforms.get(FlamComponent.random.nextInt(result.xforms.size()));
        int i = FlamComponent.random.nextInt(6);
        xform.coefs[i] += FlamComponent.random.nextGaussian() * xform.coefs[i] / 5;

        return result;
    }

    @Override
    public void reset() {
    }
}
