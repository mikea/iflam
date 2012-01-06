package flam;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Math.*;

/**
 */
public class FlamGenome implements Serializable {
    static final String[] variationNames = {
            "linear",
            "sinusoidal",
            "spherical",
            "swirl",
            "horseshoe",
            "polar",
            "handkerchief",
            "heart",
            "disc",
            "spiral",
            "hyperbolic",
            "diamond",
            "ex",
            "julia",
            "bent",
            "waves",
            "fisheye",
            "popcorn",
            "exponential",
            "power",
            "cosine",
            "rings",
            "fan",
            "blob",
            "pdj",
            "fan2",
            "rings2",
            "eyefish",
            "bubble",
            "cylinder",
            "perspective",
            "noise",
            "julian",
            "juliascope",
            "blur",
            "gaussian_blur",
            "radial_blur",
            "pie",
            "ngon",
            "curl",
            "rectangles",
            "arch",
            "tangent",
            "square",
            "rays",
            "blade",
            "secant2",
            "twintrian",
            "cross",
            "disc2",
            "super_shape",
            "flower",
            "conic",
            "parabola",
            "bent2",
            "bipolar",
            "boarders",
            "butterfly",
            "cell",
            "cpow",
            "curve",
            "edisc",
            "elliptic",
            "escher",
            "foci",
            "lazysusan",
            "loonie",
            "pre_blur",
            "modulus",
            "oscilloscope",
            "polar2",
            "popcorn2",
            "scry",
            "separation",
            "split",
            "splits",
            "stripes",
            "wedge",
            "wedge_julia",
            "wedge_sph",
            "whorl",
            "waves2",
            "exp",
            "log",
            "sin",
            "cos",
            "tan",
            "sec",
            "csc",
            "cot",
            "sinh",
            "cosh",
            "tanh",
            "sech",
            "csch",
            "coth",
            "auger",
            "flux",
            "mobius",
    };

    static final Set<String> variationNameSet = new HashSet<String>();

    static {
        Collections.addAll(variationNameSet, variationNames);
    }

    double[] background = new double[3];
    double brightness = 1.0;
    double[] center = new double[2];
    private double estimatorCurve = 0;
    private double estimatorMinimum = 0;
    private double estimatorRadius = 0;
    private double filter = 0;
    private String filterShape;
    double gamma = 4.0;
    private double gammaThreshold;
    private String interpolationType;
    private String name;
    private String nick;
    private String notes;
    private String paletteMode;
    int passes = 1;
    double quality = 1;  // aka sample_density
    double rotate;
    private double scale;
    private int[] size = new int[]{1024, 1024};
    int oversample = 1;
    private String temporalFilterType;
    private double temporalFilterWidth;
    private double temporalSamples;
    private double time;
    private String url;
    double vibrancy = 1.0;
    List<Xform> xforms = new ArrayList<Xform>();
    Xform finalxform;
    double[][] colors = new double[256][];
    double pixelsPerUnit = 50;
    private String version;
    double highlightPower = 1;
    double zoom = 1.0;
    double contrast = 1.0;
    double gammaLinearThreshold = 0.01;
    public int nbatches = 1;
    private String brood;
    private String genebank;

    public FlamGenome() {
        for (int i = 0; i < colors.length; i++) {
            colors[i] = new double[3];
        }
    }

    public FlamGenome(FlamGenome g1, FlamGenome g2, double t) {
        for (int i = 0; i < 3; ++i) {
            background[i] = interpolate(t, g1.background[i], g2.background[i]);
        }
        brightness = interpolate(t, g1.brightness, g2.brightness);
        for (int i = 0; i < 2; ++i) {
            center[i] = interpolate(t, g1.center[i], g2.center[i]);
        }
        gamma = interpolate(t, g1.gamma, g2.gamma);
        rotate = interpolate(t, g1.rotate, g2.rotate);
        vibrancy = interpolate(t, g1.vibrancy, g2.vibrancy);
        highlightPower = interpolate(t, g1.highlightPower, g2.highlightPower);
        zoom = interpolate(t, g1.zoom, g2.zoom);
        contrast = interpolate(t, g1.contrast, g2.contrast);
        gammaLinearThreshold = interpolate(t, g1.gammaLinearThreshold, g2.gammaLinearThreshold);
        for (int i = 0; i < 256; ++i) {
            double[] hsv1 = new double[3];
            double[] hsv2 = new double[3];
            double[] hsv = new double[3];
            flam3.rgb2hsv(g1.colors[i], hsv1);
            flam3.rgb2hsv(g2.colors[i], hsv2);

            for (int j = 0; j < 3; ++j) {
                hsv[j] = interpolate(t, hsv1[j], hsv2[j]);
            }

            colors[i] = new double[3];
            flam3.hsv2rgb(hsv, colors[i]);
        }

        finalxform = new Xform(g1.finalxform == null ? Xform.IDENTITY : g1.finalxform, g2.finalxform == null ? Xform.IDENTITY : g2.finalxform, t);

        for (int i = 0; i < Math.min(g1.xforms.size(), g2.xforms.size()); ++i) {
            xforms.add(new Xform(g1.xforms.get(i), g2.xforms.get(i), t));
        }
        if (g1.xforms.size() > g2.xforms.size()) {
            for (int i = g2.xforms.size(); i < g1.xforms.size(); ++i) {
                xforms.add(new Xform(g1.xforms.get(i), Xform.IDENTITY, t));
            }
        } else {
            for (int i = g1.xforms.size(); i < g2.xforms.size(); ++i) {
                xforms.add(new Xform(Xform.IDENTITY, g2.xforms.get(i), t));
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        
        result.append("FlamGenome[\n");
        result.append("  xforms=[\n");
        for (Xform xform : xforms) {
            result.append("     ");
            result.append(xform);
            result.append("\n");
        }
        result.append("  ],\n");
        result.append("  finalxform = " + finalxform + "\n");
        result.append("  brightness = " + brightness + "\n");
        result.append("  gamma = " + gamma + "\n");
        result.append("  rotate = " + rotate + "\n");
        result.append("  vibrancy = " + vibrancy + "\n");
        result.append("  highlightPower = " + highlightPower + "\n");
        result.append("  zoom = " + zoom + "\n");
        result.append("  contrast = " + contrast + "\n");
        result.append("  gammaLinearThreshold = " + gammaLinearThreshold + "\n");

        // center
        // colors
        // background
        result.append("]\n");
        
        return result.toString();
    }

    private static double interpolate(double t, double v1, double v2) {
        return (1 - t) * v1 + t * v2;
    }


    public static FlamGenome parse(String filename) throws IOException, SAXException {
        DOMParser parser = new DOMParser();
        parser.parse(filename);

        Document document = parser.getDocument();
        Element documentElement = document.getDocumentElement();
        FlamGenome flamGenome = new FlamGenome();
        flamGenome.parseFlame(documentElement);
        return flamGenome;
    }

    private void parseFlame(Element rootElement) {
        if (!rootElement.getNodeName().equals("flame")) {
            throw new IllegalArgumentException("Can't find flame element");
        }

        NamedNodeMap attributes = rootElement.getAttributes();
        for (int i = 0; i < attributes.getLength(); ++i) {
            Node node = attributes.item(i);
            String attrName = node.getNodeName();

            if (attrName.equals("background")) {
                parseIntoDoubleVector(node, background);
            } else if (attrName.equals("brightness")) {
                brightness = parseDouble(node.getNodeValue());
            } else if (attrName.equals("center")) {
                parseIntoDoubleVector(node, center);
            } else if (attrName.equals("estimator_curve")) {
                estimatorCurve = parseDouble(node.getNodeValue());
            } else if (attrName.equals("estimator_minimum")) {
                estimatorMinimum = parseDouble(node.getNodeValue());
            } else if (attrName.equals("estimator_radius")) {
                estimatorRadius = parseDouble(node.getNodeValue());
            } else if (attrName.equals("filter")) {
                filter = parseDouble(node.getNodeValue());
            } else if (attrName.equals("filter_shape")) {
                filterShape = node.getNodeValue();
            } else if (attrName.equals("gamma")) {
                gamma = parseDouble(node.getNodeValue());
            } else if (attrName.equals("gamma_threshold")) {
                gammaThreshold = parseDouble(node.getNodeValue());
            } else if (attrName.equals("interpolation_type")) {
                interpolationType = node.getNodeValue();
            } else if (attrName.equals("name")) {
                name = node.getNodeValue();
            } else if (attrName.equals("nick")) {
                nick = node.getNodeValue();
            } else if (attrName.equals("notes")) {
                notes = node.getNodeValue();
            } else if (attrName.equals("palette_mode")) {
                paletteMode = node.getNodeValue();
            } else if (attrName.equals("passes")) {
                passes = parseInt(node.getNodeValue());
            } else if (attrName.equals("quality")) {
                quality = parseDouble(node.getNodeValue());
            } else if (attrName.equals("rotate")) {
                rotate = parseDouble(node.getNodeValue());

                while (rotate > 180) {
                    rotate -= 360;
                }
                while (rotate <= -180) {
                    rotate += 360;
                }
            } else if (attrName.equals("scale")) {
                scale = parseDouble(node.getNodeValue());
            } else if (attrName.equals("size")) {
                parseIntoIntVector(node, size);
            } else if (attrName.equals("supersample")) {
                oversample = parseInt(node.getNodeValue());
            } else if (attrName.equals("temporal_filter_type")) {
                temporalFilterType = node.getNodeValue();
            } else if (attrName.equals("temporal_filter_width")) {
                temporalFilterWidth = parseDouble(node.getNodeValue());
            } else if (attrName.equals("temporal_samples")) {
                temporalSamples = parseDouble(node.getNodeValue());
            } else if (attrName.equals("time")) {
                time = parseDouble(node.getNodeValue());
            } else if (attrName.equals("url")) {
                url = node.getNodeValue();
            } else if (attrName.equals("vibrancy")) {
                vibrancy = parseDouble(node.getNodeValue());
            } else if (attrName.equals("highlight_power")) {
                highlightPower = parseDouble(node.getNodeValue());
            } else if (attrName.equals("version")) {
                version = node.getNodeValue();
            } else if (attrName.equals("brood")) {
                brood = node.getNodeValue();
            } else if (attrName.equals("genebank")) {
                genebank = node.getNodeValue();
            } else {
                throw new IllegalArgumentException("Unsupported attribute: " + node);
            }
        }

        NodeList childNodes = rootElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); ++i) {
            Node node = childNodes.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = node.getNodeName();
                if (nodeName.equals("xform")) {
                    xforms.add(parseXform(node));
                } else if (nodeName.equals("finalxform")) {
                    finalxform = parseXform(node);
                } else if (nodeName.equals("color")) {
                    parseColor(node);
                } else if (nodeName.equals("edit")) {
                    // ignore
                } else {
                    throw new IllegalArgumentException("Unsupported node: " + node);
                }
            }
        }
    }

    private void parseColor(Node node) {
        NamedNodeMap attributes = node.getAttributes();

        int index = -1;
        int[] rgb = new int[3];

        for (int j = 0; j < attributes.getLength(); ++j) {
            Node attr = attributes.item(j);
            String attrName = attr.getNodeName();

            if (attrName.equals("index")) {
                index = parseInt(attr.getNodeValue());
            } else if (attrName.equals("rgb")) {
                parseIntoIntVector(attr, rgb);
            } else {
                throw new IllegalArgumentException("Unsupported attr: " + attr);
            }
        }

        colors[index][0] = rgb[0] / 255.0;
        colors[index][1] = rgb[1] / 255.0;
        colors[index][2] = rgb[2] / 255.0;
    }

    private Xform parseXform(Node xformNode) {
        Xform xform = new Xform();

        xform.parse(xformNode);

        return xform;
    }

    private static void parseIntoDoubleVector(Node node, double[] vector) {
        String[] splits = node.getNodeValue().split(" ");
        for (int j = 0; j < splits.length; ++j) {
            vector[j] = parseDouble(splits[j]);
        }
    }

    private void parseIntoIntVector(Node node, int[] vector) {
        String[] splits = node.getNodeValue().split(" ");
        for (int j = 0; j < splits.length; ++j) {
            vector[j] = parseInt(splits[j]);
        }
    }

    static class Xform implements Serializable {
        double[] coefs = new double[6];
        double[] variations = new double[variationNames.length];
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
            for (int j = 0; j < FlamGenome.variationNames.length; ++j) {
                if (variations[j] != 0.0) {
                    variationCount++;
                }
            }
            nonZeroVariations = new int[variationCount];
            variationCount = 0;
            for (int j = 0; j < FlamGenome.variationNames.length; ++j) {
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
            if (f2 == null) {
                t = 1 - t;
                for (int i = 0; i < 6; ++i) {
                    coefs[i] = f1.coefs[i];
                }
                for (int i = 0; i < variationNames.length; ++i) {
                    variations[i] = f1.variations[i] * t;
                }

                color = f1.color;
                weight = f1.weight * t;
                opacity = f1.opacity * t;
                colorSpeed = f1.colorSpeed;
                julian_dist = f1.julian_dist;
                julian_power = f1.julian_power;
                perspective_angle = f1.perspective_angle;
                perspective_dist = f1.perspective_dist;
            } else {
                for (int i = 0; i < 6; ++i) {
                    coefs[i] = interpolate(t, f1.coefs[i], f2.coefs[i]);
                }
                for (int i = 0; i < variationNames.length; ++i) {
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
                    parseIntoDoubleVector(node, coefs);
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
                } else if (variationNameSet.contains(attrName)) {
                    for (int j = 0; j < variationNames.length; j++) {
                        if (variationNames[j].equals(attrName)) {
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


        public void applyTo(double[] in, double[] out) {
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
                double r = Math.sqrt(r2);

                for (int j = 0; j < nonZeroVariations.length; ++j) {
                    double dx;
                    double dy;

                    int var = nonZeroVariations[j];
                    switch (var) {
                        default:
                            throw new IllegalArgumentException("Unimplemented variation: " + j + " : " + FlamGenome.variationNames[j]);
                        case 0: // linear
                            dx = x;
                            dy = y;
                            break;
                        case 1: // sinusoidal
                            dx = sin(x);
                            dy = sin(y);
                            break;
                        case 2: // spherical
                            dx = x / r2;
                            dy = y / r2;
                            break;
                        case 3: // swirl
                            dx = x * sin(r2) - y * cos(r2);
                            dy = x * cos(r2) + y * sin(r2);
                            break;
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
                        case 8: // disc
                        {
                            double theta = atan2(x, y);
                            dx = theta * sin(PI * r) / PI;
                            dy = theta * cos(PI * r) / PI;
                            break;
                        }
                        case 11: // diamond
                        {
                            double theta = atan2(x, y);
                            dx = sin(theta) * cos(r);
                            dy = cos(theta) * sin(r);
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
                        case 15: // waves
                            dx = x + b * sin(y / (c * c));
                            dy = y + e * sin(x / (f * f));
                            break;
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
                    }

                    x2 += variations[var] * dx;
                    y2 += variations[var] * dy;
                }

                x = x2;
                y = y2;
            }

            cc = cc * (1 - colorSpeed) + color * colorSpeed;

            out[0] = x;
            out[1] = y;
            out[2] = cc;
        }
    }

    private static double atan2(double x, double y) {
//        return Math.atan2(x, y);
        return fastAtan2(x, y);
    }


    //http://dspguru.com/dsp/tricks/fixed-point-atan2-with-self-normalization
    static double fastAtan2(double x, double y) {
        double coeff_1 = PI / 4;
        double coeff_2 = 3 * coeff_1;
        double abs_y = abs(x) + 1e-10;      // kludge to prevent 0/0 condition

        double r;
        double angle;

        if (y >= 0) {
            r = (y - abs_y) / (y + abs_y);
            angle = coeff_1 - coeff_1 * r;
        } else {
            r = (y + abs_y) / (abs_y - y);
            angle = coeff_2 - coeff_1 * r;
        }
        if (x < 0)
            return (-angle);     // negate if in quad III or IV
        else
            return (angle);
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }
}
