package flam;

/**
 * @author mike
 */
public class AnimationProvider implements GenomeProvider {
    private FlamGenome genome;
    private int xFormIndex;
    private int coef;

    public AnimationProvider(FlamGenome genome) {
        this.genome = genome;
        reset();
    }


    @Override
    public FlamGenome getGenome() {
        FlamGenome result = (FlamGenome) DeepCopy.copy(genome);
        FlamGenome.Xform xform = result.xforms.get(xFormIndex);
        xform.coefs[coef] += Math.sin(System.nanoTime() / 1e9);

        return result;
    }

    @Override
    public void reset() {
        xFormIndex = FlamComponent.random.nextInt(genome.xforms.size());
        coef = FlamComponent.random.nextInt(6);
    }

}
