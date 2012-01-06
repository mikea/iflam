package flam;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

/**
 * @author mike
 */
public class AnimationProvider implements GenomeProvider {
    private FlamGenome genome;
    private int xFormIndex;
    private int coef;
    private CoefAnimator animator;

    public AnimationProvider() {
    }

    public void setGenome(FlamGenome genome) {
        this.genome = genome;
        reset();
    }

    @Override
    public FlamGenome getGenome() {
        double t = System.nanoTime() / 1e9;

        FlamGenome result = (FlamGenome) DeepCopy.copy(genome);
        FlamGenome.Xform xform = result.xforms.get(xFormIndex);
        animator.animate(xform.coefs, System.currentTimeMillis() / 1e3);
        final double a = xform.coefs[0];
        final double b = xform.coefs[2];
        final double c = xform.coefs[4];
        final double d = xform.coefs[1];
        final double e = xform.coefs[3];
        final double f = xform.coefs[5];

//        x1 = x * a + y * b + c;
//        y1 = x * d + y * e + f;




        return result;
    }

    @Override
    public void reset() {
        xFormIndex = FlamComponent.random.nextInt(genome.xforms.size());

        switch (FlamComponent.random.nextInt(3)) {
            default:
                throw new UnsupportedOperationException();
            case 0:
                animator = new CoordinateAnimator();
                break;
            case 1:
                animator = new PointRotator();
                break;
            case 2:
                animator = new CoefRotator();
                break;
        }

        System.out.println("animator = " + animator);
    }


    interface CoefAnimator {
        void animate(double[] coefs, double time);
    }

    static class CoordinateAnimator implements CoefAnimator {
        private int coef;

        CoordinateAnimator() {
            coef = FlamComponent.random.nextInt(6);
        }

        @Override
        public void animate(double[] coefs, double time) {
            coefs[coef] += Math.sin(time) * coefs[coef] / 2;
        }
    }

    static class PointRotator implements CoefAnimator {
        private int idx;

        PointRotator() {
            idx = FlamComponent.random.nextInt(3);
        }

        @Override
        public void animate(double[] coefs, double time) {
            double x, y;
            double a = coefs[0];
            double b = coefs[2];
            double c = coefs[4];
            double d = coefs[1];
            double e = coefs[3];
            double f = coefs[5];

    //        x1 = x * a + y * b + c;
    //        y1 = x * d + y * e + f;
    //
            switch (idx) {
                default:
                    throw new UnsupportedOperationException();
                case 0:
                    x = a; y = d; break;
                case 1:
                    x = b; y = e; break;
                case 2:
                    x = c; y = f; break;
            }

            x += Math.sin(time);
            y += Math.cos(time);

            switch (idx) {
                default:
                    throw new UnsupportedOperationException();
                case 0:
                    a = x; d = y; break;
                case 1:
                    b = x; e = y; break;
                case 2:
                    c = x; f = y; break;
            }
            
            coefs[0] = a;
            coefs[2] = b;
            coefs[4] = c;
            coefs[1] = d;
            coefs[3] = e;
            coefs[5] = f;
        }
    }
    
    static class CoefRotator implements CoefAnimator {

        @Override
        public void animate(double[] coefs, double time) {
            double a = coefs[0];
            double b = coefs[2];
            double c = coefs[4];
            double d = coefs[1];
            double e = coefs[3];
            double f = coefs[5];

    //        x1 = x * a + y * b + c;
    //        y1 = x * d + y * e + f;
            
            a -= c;
            b -= c;
            e -= f;
            d -= f;

            double si = sin(time / 20);
            double co = cos(time / 20);

            double a1 = a * co - d * si;
            double d1 = a * si + d * co;

            double b1 = b * co - e * si;
            double e1 = b * si + e * co;

            a = a1;
            d = d1;
            b = b1;
            e = e1;
            
            a += c;
            b += c;
            e += f;
            d += f;
        }
    }
}
