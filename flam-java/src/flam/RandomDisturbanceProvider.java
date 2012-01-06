package flam;

/**
 * @author mike
 */
public class RandomDisturbanceProvider implements GenomeProvider {
    private FlamGenome genome;

    public RandomDisturbanceProvider(FlamGenome genome) {
        this.genome = genome;
    }

    @Override
    public FlamGenome getGenome() {
        FlamGenome result = (FlamGenome) DeepCopy.copy(genome);

        FlamGenome.Xform xform = result.xforms.get(FlamComponent.random.nextInt(result.xforms.size()));
        int i = FlamComponent.random.nextInt(6);
        xform.coefs[i] += FlamComponent.random.nextGaussian() * xform.coefs[i] / 5;

        return result;
    }

    @Override
    public void reset() {
    }
}
