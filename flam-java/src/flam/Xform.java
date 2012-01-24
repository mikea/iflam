package flam;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.Serializable;

import static flam.Genome.interpolate;
import static flam.MyMath.*;
import static java.lang.Double.parseDouble;

/**
*/
public class Xform implements Serializable {
    public double[] coefs = new double[6];
    double[] variations = new double[Genome.variationNames.length];
    int[] nonZeroVariations;

    double color;
    double weight;
    private double animate;
    double colorSpeed = 0.5;
    double opacity = 1.0;
    private double julian_dist;
    private double julian_power;
    private double perspective_angle;
    private double perspective_dist;
    public static final Xform IDENTITY = new Xform();
    private double chaos = 1;
    private double[] post;
    private double radial_blur_angle;
    private double rings2_val;
    private double juliascopeDist;
    private double juliascopePower;

    static {
        IDENTITY.coefs[0] = 1;
        IDENTITY.coefs[3] = 1;
        IDENTITY.variations[0] = 1;
        IDENTITY.init();
    }

    Xform() {
    }

    private void init() {
        int variationCount = 0;
        for (int j = 0; j < Genome.variationNames.length; ++j) {
            if (variations[j] != 0.0) {
                variationCount++;
            }
        }
        nonZeroVariations = new int[variationCount];
        variationCount = 0;
        for (int j = 0; j < Genome.variationNames.length; ++j) {
            if (variations[j] != 0.0) {
                nonZeroVariations[variationCount] = j;
                variationCount++;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("Xform[");
        builder.append("coefs=");
        for (double coef : coefs) {
            builder.append(coef);
            builder.append(", ");
        }
        builder.append(", variations=");
        for (double coef : variations) {
            builder.append(coef);
            builder.append(", ");
        }
        builder.append(", nonZeroVariations=");
        for (int coef : nonZeroVariations) {
            builder.append(coef);
            builder.append(", ");
        }
        builder.append("]");

        return builder.toString();
    }

    public Xform(Xform f1, Xform f2, double t) {
        for (int i = 0; i < 6; ++i) {
            coefs[i] = interpolate(t, f1.coefs[i], f2.coefs[i]);
        }
        for (int i = 0; i < Genome.variationNames.length; ++i) {
            variations[i] = interpolate(t, f1.variations[i], f2.variations[i]);
        }
        color = interpolate(t, f1.color, f2.color);
        weight = interpolate(t, f1.weight, f2.weight);
        colorSpeed = interpolate(t, f1.colorSpeed, f2.colorSpeed);
        julian_dist = interpolate(t, f1.julian_dist, f2.julian_dist);
        julian_power = interpolate(t, f1.julian_power, f2.julian_power);
        perspective_angle = interpolate(t, f1.perspective_angle, f2.perspective_angle);
        perspective_dist = interpolate(t, f1.perspective_dist, f2.perspective_dist);
        opacity = interpolate(t, f1.opacity, f2.opacity);
        radial_blur_angle = interpolate(t, f1.radial_blur_angle, f2.radial_blur_angle);
        rings2_val = interpolate(t, f1.rings2_val, f2.rings2_val);
        juliascopeDist = interpolate(t, f1.juliascopeDist, f2.juliascopeDist);
        juliascopePower = interpolate(t, f1.juliascopePower, f2.juliascopePower);

        if (f1.post != null || f2.post != null) {
            post = new double[6];
        }
        if (f1.post != null && f2.post != null) {
            for (int i = 0; i < 6; ++i) {
                post[i] = interpolate(t, f1.post[i], f2.post[i]);
            }
        } if (f1.post != null) {
            post[0] = interpolate(t, f1.post[0], 1);
            post[1] = interpolate(t, f1.post[1], 0);
            post[2] = interpolate(t, f1.post[2], 0);
            post[3] = interpolate(t, f1.post[3], 1);
            post[4] = interpolate(t, f1.post[4], 0);
            post[5] = interpolate(t, f1.post[5], 0);
        } else if (f2.post != null) {
            post[0] = interpolate(t, 1, f2.post[0]);
            post[1] = interpolate(t, 0, f2.post[1]);
            post[2] = interpolate(t, 0, f2.post[2]);
            post[3] = interpolate(t, 1, f2.post[3]);
            post[4] = interpolate(t, 0, f2.post[4]);
            post[5] = interpolate(t, 0, f2.post[5]);
        }

        init();
    }

    public void parse(Node xformNode) {
        if (!xformNode.getNodeName().equals("xform") && !xformNode.getNodeName().equals("finalxform")) {
            throw new IllegalArgumentException("Can't find xform element");
        }


        NamedNodeMap attributes = xformNode.getAttributes();
        nextAttr:
        for (int i = 0; i < attributes.getLength(); ++i) {
            Node node = attributes.item(i);
            String attrName = node.getNodeName();

            if (attrName.equals("coefs")) {
                Genome.parseIntoDoubleVector(node, coefs);
            } else if (attrName.equals("color")) {
                color = parseDouble(node.getNodeValue());
            } else if (attrName.equals("symmetry")) {
                colorSpeed = (1 - parseDouble(node.getNodeValue())) / 2;
                animate = parseDouble(node.getNodeValue()) > 0 ? 0 : 1;
            } else if (attrName.equals("weight")) {
                weight = parseDouble(node.getNodeValue());
            } else if (attrName.equals("animate")) {
                animate = parseDouble(node.getNodeValue());
            } else if (attrName.equals("color_speed")) {
                colorSpeed = parseDouble(node.getNodeValue());
            } else if (attrName.equals("opacity")) {
                opacity = parseDouble(node.getNodeValue());
            } else if (attrName.equals("julian_dist")) {
                julian_dist = parseDouble(node.getNodeValue());
            } else if (attrName.equals("julian_power")) {
                julian_power = parseDouble(node.getNodeValue());
            } else if (attrName.equals("perspective_angle")) {
                perspective_angle = parseDouble(node.getNodeValue());
            } else if (attrName.equals("perspective_dist")) {
                perspective_dist = parseDouble(node.getNodeValue());
            } else if (attrName.equals("radial_blur_angle")) {
                radial_blur_angle = parseDouble(node.getNodeValue());
            } else if (attrName.equals("juliascope_dist")) {
                juliascopeDist = parseDouble(node.getNodeValue());
            } else if (attrName.equals("juliascope_power")) {
                juliascopePower = parseDouble(node.getNodeValue());
            } else if (attrName.equals("rings2_val")) {
                rings2_val = parseDouble(node.getNodeValue());
            } else if (attrName.equals("post")) {
                post = new double[6];
                Genome.parseIntoDoubleVector(node, post);
//                } else if (attrName.equals("chaos")) {
//                    chaos = parseDouble(node.getNodeValue());
            } else if (Genome.variationNameSet.contains(attrName)) {
                for (int j = 0; j < Genome.variationNames.length; j++) {
                    if (Genome.variationNames[j].equals(attrName)) {
                        variations[j] = parseDouble(node.getNodeValue());
                        continue nextAttr;
                    }
                }
                throw new IllegalArgumentException("Shouldn't happen");
            } else {
                throw new IllegalArgumentException("Unsupported attribute: " + node);
            }
        }

        init();
    }

    public boolean applyTo(double[] in, double[] out) {
        double x = in[0];
        double y = in[1];
        double cc = in[2];

        final double a = coefs[0];
        final double b = coefs[2];
        final double c = coefs[4];
        final double d = coefs[1];
        final double e = coefs[3];
        final double f = coefs[5];

        {   // Affine transform
            double x1, y1;
            x1 = x * a + y * b + c;
            y1 = x * d + y * e + f;

            x = x1;
            y = y1;
        }

        {   // Nonlinear transform
            double x2 = 0, y2 = 0;

            double r2 = x * x + y * y;
            double r = sqrt(r2);

            for (int j = 0; j < nonZeroVariations.length; ++j) {
                double dx;
                double dy;

                int var = nonZeroVariations[j];
                final double w = variations[var];
                switch (var) {
                    default:
                        throw new IllegalArgumentException("Unimplemented variation: " + var + " : " + Genome.variationNames[var]);
                    case 0: // linear
                        dx = x;
                        dy = y;
                        break;
                    case 1: // sinusoidal
                    {
                        dx = sin(x);
                        dy = sin(y);
                        break;
                    }
                    case 2: // spherical
                    {
                        dx = x / r2;
                        dy = y / r2;
                        break;
                    }
                    case 3: // swirl
                    {
                        dx = x * sin(r2) - y * cos(r2);
                        dy = x * cos(r2) + y * sin(r2);
                        break;
                    }
                    case 4: // horseshoe
                    {
                        dx = (x - y) * (x + y) / r;
                        dy = 2 * x * y / r;
                        break;
                    }
                    case 5: // polar
                    {
                        double theta = atan2(x, y);
                        dx = theta / PI;
                        dy = r - 1;
                        break;
                    }
                    case 6: // handkerchief
                    {
                        double theta = atan2(x, y);
                        dx = r * sin(theta + r);
                        dy = r * cos(theta - r);
                        break;
                    }
                    case 7: // heart
                    {
                        double theta = atan2(x, y);
                        dx = r * sin(theta * r);
                        dy = - r * cos(theta * r);
                        break;
                    }
                    case 8: // disc
                    {
                        double theta = atan2(x, y);
                        dx = theta * sin(PI * r) / PI;
                        dy = theta * cos(PI * r) / PI;
                        break;
                    }
                    case 10: // hyperbolic
                    {
                        double theta = atan2(x, y);
                        dx = sin(theta)/ r;
                        dy = r * cos(theta);
                        break;
                    }
                    case 11: // diamond
                    {
                        double theta = atan2(x, y);
                        dx = sin(theta) * cos(r);
                        dy = cos(theta) * sin(r);
                        break;
                    }
                    case 12: // ex
                    {
                        double theta = atan2(x, y);
                        double p0 = sin(theta + r);
                        double p1 = cos(theta - r);
                        dx = r * (p0*p0*p0 + p1 * p1 * p1);
                        dy = r * (p0*p0*p0 - p1 * p1 * p1);
                        break;
                    }
                    case 13: // julia
                    {
                        double theta = atan2(x, y);
                        double omega = FlamComponent.random.nextBoolean() ? 0 : PI;
                        dx = sqrt(r) * cos(theta / 2 + omega);
                        dy = sqrt(r) * sin(theta / 2 + omega);
                        break;
                    }
                    case 14: // bent
                    {
                        if (x >= 0 && y >= 0) {
                            dx = x; dy = y;
                        } else if (x < 0 && y >= 0) {
                            dx = 2 * x; dy = y;
                        } else if (x >= 0 && y < 0) {
                            dx = x; dy = y / 2;
                        } else /* if (x < 0 && y < 0) */ {
                            dx = 2 * x; dy = y / 2;
                        }
                        break;
                    }
                    case 15: // waves
                        dx = x + b * sin(y / (c * c + Genome.EPS));
                        dy = y + e * sin(x / (f * f + Genome.EPS));
                        break;
                    case 16: // fisheye
                        dx = y * 2 / (r + 1);
                        dy = x * 2 / (r + 1);
                        break;
                    case 21: // rings
                    {
                        double theta = atan2(x, y);
                        double c2 = c * c;
                        double k = fmod(r + c2, 2 * c2) - c2 + r * (1 - c2);
                        dx = k * cos(theta);
                        dy = k * sin(theta);
                        break;
                    }
                    case 22: // fan
                    {
                        double theta = atan2(x, y);
                        double t = PI * c * c;
                        if (fmod(theta + f, t) > t / 2) {
                            dx = r * cos (theta - t/2);
                            dy = r * sin (theta - t/2);
                        } else {
                            dx = r * cos (theta + t/2);
                            dy = r * sin (theta + t/2);
                        }
                        break;
                    }
                    case 26: // rings2
                    {
                        double theta = atan2(x, y);
                        double p = rings2_val* rings2_val;
                        double t = r - 2*p*floor((r + p) / (2 * p)) + r* (1-p);
                        dx = t * sin(theta);
                        dy = t * cos(theta);
                        break;
                    }
                    case 27: // eyefish
                        dx = 2 * x / (r + 1);
                        dy = 2 * y / (r + 1);
                        break;
                    case 28: // bubble
                        dx = 4 * x / (r2 + 4);
                        dy = 4 * y / (r2 + 4);
                        break;
                    case 29: // cylinder
                        dx = sin(x);
                        dy = y;
                        break;
                    case 30: // perspective
                    {
                        double p1 = perspective_angle;
                        double p2 = perspective_dist;
                        dx = p2 * x / (p2 - y * sin(p1));
                        dy = p2 * y * cos(p1) / (p2 - y * sin(p1));
                        break;
                    }
                    case 32: // julian
                    {
                        double phi = atan2(y, x);
                        double p1 = julian_power;
                        double p2 = julian_dist;
                        double p3 = floor(abs(p1) * FlamComponent.random.nextDouble());
                        double t = (phi + 2 * PI * p3) / p1;
                        double z = pow(r, p2 / p1);
                        dx = z * cos(t);
                        dy = z * sin(t);
                        break;
                    }
                    case 33: // julia_scope
                    {
                      double phi = atan2(y, x);
                        double p1 = juliascopePower;
                        double p2 = juliascopeDist;
                        double p3 = floor(abs(p1) * FlamComponent.random.nextDouble());
                        double t = (FlamComponent.crnd() * phi + 2 * PI * p3) / p1;
                        double z = pow(r, p2 / p1);
                      dx = z * cos(t);
                      dy = z * sin(t);
                      break;
                    }
                    case 34: // blur
                    {
                        double xi1 = FlamComponent.random.nextDouble();
                        double xi2 = FlamComponent.random.nextDouble();
                        dx = xi1 * cos(2 * PI * xi2);
                        dy = xi1 * sin(2 * PI * xi2);
                        break;
                    }
                    case 36:  // radial_blur
                    {
                        double phi = atan2(y, x);
                        double p1 = radial_blur_angle * PI / 2;
                        double t1 = w * (FlamComponent.random.nextDouble() + FlamComponent.random.nextDouble() + FlamComponent.random.nextDouble() + FlamComponent.random.nextDouble() - 2);
                        double t2 = phi + t1 * sin(p1);
                        double t3 = t1 * cos(p1) - 1;
                        dx = (r * cos(t2) + t3 * x) / w;
                        dy = (r * sin(t2) + t3 * y) / w;
                    }
                    case 45:  // blade
                    {
                        double xi = FlamComponent.random.nextDouble();
                        dx = x * (cos(xi * r * w) + sin(xi * r * w));
                        dy = x * (cos(xi * r * w) - sin(xi * r * w));
                    }
                    case 46:  // secant2
                    {
                        dx = x;
                        dy = 1 / (w * cos(w * r));
                    }
                }

                x2 += w * dx;
                y2 += w * dy;
            }

            x = x2;
            y = y2;
        }

        {
            if (post != null) {
                double x1, y1;
                x1 = x * post[0] + y * post[2] + post[4];
                y1 = x * post[1] + y * post[3] + post[5];

                x = x1;
                y = y1;
            }
        }

        cc = cc * (1 - colorSpeed) + color * colorSpeed;

        boolean good = true;

        if (badvalue(x) || badvalue(y)) {
            x = FlamComponent.crnd();
            y = FlamComponent.crnd();
            good = false;
        }

        out[0] = x;
        out[1] = y;
        out[2] = cc;
        return good;
    }

    private static boolean badvalue(double x) {
        return ((x) != (x)) || ((x) > 1e10) || ((x) < -1e10);
    }
}
