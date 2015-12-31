package flam;

/**
 * @author mike
 */
public class TransitionGenomeProvider implements GenomeProvider {
    private Genome g1;
    private Genome g2;
    private long startTime;
    private double lastT;
    private Genome currentGenome;

    public TransitionGenomeProvider(Genome g1, Genome g2) {
        this.g1 = g1;
        this.g2 = g2;
    }

    @Override
    public Genome getGenome() {
        if (currentGenome == null) {
            currentGenome = g1;
            startTime = System.currentTimeMillis();
            lastT = 0;
            return currentGenome;
        } else {
            double t = getT();
            if (t != lastT) {
                lastT = t;
                System.out.println("******" + t);
                currentGenome = new Genome(g1, g2, t);
            }
            return currentGenome;
        }
    }

    @Override
    public void reset() {
        currentGenome = null;
    }

    public double getT() {
        double step = 100.0;
        int duration = 100;

        double t = (System.currentTimeMillis() - startTime) / (step * duration);
        if (t > 1.0) {
            return 1.0;
        }

        t = ((int) (t * step)) / step;

        return t;
    }
}
