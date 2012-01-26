package flam;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static flam.MyMath.abs;
import static flam.MyMath.min;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

/**
 */
public class Genome implements Serializable {
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
    public static final double EPS = 1e-10;

    static {
        Collections.addAll(variationNameSet, variationNames);
    }

    public double[] background = new double[3];
    public double brightness = 1.0;
    public double[] center = new double[2];
    public double estimatorCurve = 0.4;
    public double estimatorMinimum = 0;
    public double estimatorRadius = 9;
    private double filter = 0;
    private String filterShape;
    public double gamma = 4.0;
    private double gammaThreshold;
    private String interpolationType;
    private String name;
    private String nick;
    private String notes;
    private String paletteMode;
    int passes = 1;
    double quality = 1;  // aka sample_density
    double rotate;
    public int[] size = new int[]{1024, 1024};
    int oversample = 1;
    private String temporalFilterType;
    private double temporalFilterWidth;
    private double temporalSamples;
    private double time;
    private String url;
    public double vibrancy = 1.0;
    public List<Xform> xforms = new ArrayList<Xform>();
    public Xform finalxform;
    double[][] colors = new double[256][];
    public double pixelsPerUnit = 50;
    private String version;
    public double highlightPower = 1;
    public double zoom = 0;
    public double contrast = 1.0;
    double gammaLinearThreshold = 0.01;
    public int nbatches = 1;
    private String brood;
    private String genebank;
    public static final int CHOOSE_XFORM_GRAIN = 16384;
    private double[][] chaos;
    public boolean chaosEnabled;
    public int[][] xformDistrib;
    private String parents;

    public Genome() {
        for (int i = 0; i < colors.length; i++) {
            colors[i] = new double[3];
        }
    }

    public Genome(Genome g1, Genome g2, double t) {
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
            Colors.rgb2hsv(g1.colors[i], hsv1);
            Colors.rgb2hsv(g2.colors[i], hsv2);

            for (int j = 0; j < 3; ++j) {
                hsv[j] = interpolate(t, hsv1[j], hsv2[j]);
            }

            colors[i] = new double[3];
            Colors.hsv2rgb(hsv, colors[i]);
        }

        finalxform = new Xform(g1.finalxform == null ? Xform.IDENTITY : g1.finalxform, g2.finalxform == null ? Xform.IDENTITY : g2.finalxform, t);

        for (int i = 0; i < min(g1.xforms.size(), g2.xforms.size()); ++i) {
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

        result.append("Genome[\n");
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

    static double interpolate(double t, double v1, double v2) {
        return (1 - t) * v1 + t * v2;
    }


    public static Genome parse(String filename) throws IOException, SAXException {
        DOMParser parser = new DOMParser();
        parser.parse(filename);

        Document document = parser.getDocument();
        Element documentElement = document.getDocumentElement();
        Genome flamGenome = new Genome();
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
                pixelsPerUnit = parseDouble(node.getNodeValue());
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
            } else if (attrName.equals("parents")) {
                parents = node.getNodeValue();
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

    static void parseIntoDoubleVector(Node node, double[] vector) {
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

    public void createXformDistrib() {
        chaos = new double[xforms.size()][];

        for (int i = 0; i < xforms.size(); ++i) {
            chaos[i] = new double[xforms.size()];

            for (int j = 0; j < xforms.size(); ++j) {
                chaos[i][j] = 1;
            }
        }

        xformDistrib = new int[xforms.size()][];
        for (int i = 0; i < xformDistrib.length; ++i) {
            xformDistrib[i] = new int[CHOOSE_XFORM_GRAIN];
        }

        createChaosDist(-1, xformDistrib[0]);
        chaosEnabled = isChaosEnabled();
        if (chaosEnabled) {
            for (int i = 0; i < xforms.size(); i++) {
                createChaosDist(i, xformDistrib[i]);
            }
        }
    }

    private boolean isChaosEnabled() {
        for (int i = 0; i < xforms.size(); i++) {
            for (int j = 0; j < xforms.size(); j++) {
                if (abs(chaos[i][j] - 1.0) > EPS)
                    return true;
            }
        }

        return false;
    }

    private void createChaosDist(int xi, int[] xform_distrib) {
        int xformsSize = xforms.size();

        double weightSum = 0.0;
        {
            for (int i = 0; i < xformsSize; i++) {
                double d = xforms.get(i).weight;
                if (xi >= 0)
                    d *= chaos[xi][i];
                if (d < 0.0) {
                    throw new IllegalStateException("xform weight must be non-negative, not " + d);
                }
                weightSum += d;
            }
        }

        if (weightSum == 0.0) {
            throw new IllegalStateException("cannot iterate empty flame");
        }

        double step = weightSum / CHOOSE_XFORM_GRAIN;

        int j = 0;
        double t = xforms.get(0).weight;
        if (xi >= 0)
            t *= chaos[xi][0];
        double r = 0.0;
        for (int i = 0; i < CHOOSE_XFORM_GRAIN; i++) {
            while (r >= t) {
                j++;

                if (xi >= 0)
                    t += xforms.get(j).weight * chaos[xi][j];
                else
                    t += xforms.get(j).weight;

            }
            xform_distrib[i] = j;
            r += step;
        }
    }


    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    public double[] getColor(int idx) {
        return colors[idx];
    }

    public double getRotate() {
        return rotate;
    }
}
