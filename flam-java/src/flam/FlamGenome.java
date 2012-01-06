package flam;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Math.*;
import static java.lang.Math.sin;

/**
 */
public class FlamGenome {
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
    double quality = 1;
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

    public FlamGenome() {
        for (int i = 0; i < colors.length; i++) {
            colors[i] = new double[3];
        }
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

    static class Xform {
        double[] coefs = new double[6];
        double[] variations = new double[variationNames.length];

        double color;
        double weight;
        private double animate;
        double colorSpeed = 0.5;
        private double opacity;


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
                double theta = Math.atan2(x, y);

                for (int j = 0; j < FlamGenome.variationNames.length; ++j) {
                    double dx;
                    double dy;

                    if (variations[j] == 0) continue;

                    switch (j) {
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
                        case 6: // handkerchief
                            dx = r * sin(theta + r);
                            dy = r * cos(theta - r);
                            break;
                        case 11: // diamond
                            dx = sin(theta) * cos(r);
                            dy = cos(theta) * sin(r);
                            break;
                        case 13: // julia
                            double omega = FlamComponent.random.nextBoolean() ? 0 : PI;
                            dx = sqrt(r) * cos(theta / 2 + omega);
                            dy = sqrt(r) * sin(theta / 2 + omega);
                            break;
                        case 15: // waves
                            dx = x + b * sin(y / (c * c));
                            dy = y + e * sin(x / (f * f));
                            break;
                        case 27: // eyefish
                            dx = 2 * x / (r + 1);
                            dy = 2 * y / (r + 1);
                            break;
                        case 29: // cylinder
                            dx = sin(x);
                            dy = y;
                            break;
                    }

                    x2 += variations[j] * dx;
                    y2 += variations[j] * dy;
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
}
