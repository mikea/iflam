#include <boost/algorithm/string.hpp>
#include <boost/random/uniform_int_distribution.hpp>
#include <boost/lexical_cast.hpp>
#include <iostream>
#include "tinyxml/tinyxml.h"

#include "genome.h"

using std::string;
using std::vector;
using boost::scoped_ptr;

namespace {
  bool BadValue(double x) {
    return (x != x) || (x > 1e10) || (x < -1e10);;
  }

  const char * const kVariationNames[Xform::kVariationsCount] = {
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

}

struct apply_error : virtual error { };

Xform::Xform()
 : color_speed_(0.5),
   opacity_(1.0),
   weight_(1.0) { }

Xform::~Xform() { }

bool Xform::Apply(double* in, double* out) const {
  double x = in[0];
  double y = in[1];
  double cc = in[2];

  const double a = coefs_[0];
  const double b = coefs_[2];
  const double c = coefs_[4];
  const double d = coefs_[1];
  const double e = coefs_[3];
  const double f = coefs_[5];

  { // Affine transform.
    double x1, y1;
    x1 = x * a + y * b + c;
    y1 = x * d + y * e + f;
    x = x1;
    y = y1;
  }

  {  // Nonlinear transform
    double x1 = 0;
    double y1 = 0;
    const double r2 = x * x + y * y;
    const double r = sqrt(r2);

    for (size_t i = 0; i < non_zero_variations_.size(); ++i) {
      size_t var = non_zero_variations_[i];
      const double w = variations_[var];

      if (w == 0) {
        continue;
      }

      double dx;
      double dy;

      switch (var) {
        default:
          BOOST_THROW_EXCEPTION(apply_error()
              << error_message("Variation " + boost::lexical_cast<string>(var)
                + " (" + kVariationNames[var] + ") not implemented"));
          break;
        case 0: // linear
        {
          dx = x;
          dy = y;
          break;
        }
        case 8: // disc
        {
          double theta = atan2(x, y);
          dx = theta * sin(kPI * r) / kPI;
          dy = theta * cos(kPI * r) / kPI;
          break;
        }
        case 28: // bubble
        {
          dx = 4 * x / (r2 + 4);
          dy = 4 * y / (r2 + 4);
          break;
        }
        case 30: // perspective
        {
          double p1 = perspective_angle_;
          double p2 = perspective_dist_;
          dx = p2 * x / (p2 - y * sin(p1));
          dy = p2 * y * cos(p1) / (p2 - y * sin(p1));
          break;
        }
        case 32: // julian
        {
          double phi = atan2(y, x);
          double p1 = julian_power_;
          double p2 = julian_dist_;
          double p3 = floor(abs(p1) * Random::rnd());
          double t = (phi + 2 * kPI * p3) / p1;
          double z = pow(r, p2 / p1);
          dx = z * cos(t);
          dy = z * sin(t);
          break;
        }
      }

      x1 += w * dx;
      y1 += w * dy;
    }

    x = x1;
    y = y1;
  }

  if (post_.get() != NULL) {
    double x1, y1;
    const array<double, 6>& post = *post_;
    x1 = x * post[0] + y * post[2] + post[4];
    y1 = x * post[1] + y * post[3] + post[5];
    x = x1;
    y = y1;
  }

  cc = cc * (1 - color_speed_) + color_ * color_speed_;

  bool good = true;
  if (BadValue(x) || BadValue(y)) {
    abort();
  }

  out[0] = x;
  out[1] = y;
  out[2] = cc;
  return good;
}

Genome::Genome()
 : brightness_(1),
   contrast_(1),
   gamma_ (4),
   gamma_threshold_(0.01),
   highlight_power_(1),
   passes_ (1),
   pixels_per_unit_(50),
   quality_(1),
   vibrancy_(1),
   zoom_(0) {
}

Genome::~Genome() {
}

// ----------------------------------------------------------------------------
// - Parsing
// ----------------------------------------------------------------------------


struct parse_error : virtual error { };
struct vector_parse_error : virtual parse_error { };
struct xml_parse_error : virtual parse_error { };
struct unsupported_attribute_error : virtual parse_error { };
struct unsupported_element_error : virtual parse_error { };

namespace {
  template<typename T, size_t size>
  void ParseArray(const string& str, array<T, size>* array) {
    vector<string> strs;
    boost::split(strs, str, boost::is_space());
    if (strs.size() != size) {
      BOOST_THROW_EXCEPTION(vector_parse_error()
          << error_message("Wrong vector length in '" + str + "'. Is " +
            boost::lexical_cast<string>(strs.size()) + " but should be " +
            boost::lexical_cast<string>(size)));
    }
    for (size_t i = 0; i < strs.size(); ++i) {
      (*array)[i] = boost::lexical_cast<T>(strs[i]);
    }
  }

  template<typename T>
  void ParseScalar(const string& str, T* d) {
    // TODO: error checks
    *d = boost::lexical_cast<T>(str);
  }

}

void Genome::Read(string file_name) {
  TiXmlDocument doc(file_name.c_str());
  if (!doc.LoadFile()) {
    // throw ReadError();
    BOOST_THROW_EXCEPTION(xml_parse_error()
        << error_message("Can't parse")
        << boost::errinfo_file_name(file_name));
  }

  const TiXmlElement* root = doc.RootElement();

  // parse attributes
  for (const TiXmlAttribute* attr = root->FirstAttribute();
       attr != NULL;
       attr = attr->Next()) {
    const string attr_name(attr->Name());

    if (attr_name == "name") {
      name_ = attr->Value();
    } else if (attr_name == "time") {
      ParseScalar(attr->ValueStr(), &time_);
    } else if (attr_name == "size") {
      ParseArray<int, 2>(attr->ValueStr(), &size_);
    } else if (attr_name == "center") {
      ParseArray<double, 2>(attr->ValueStr(), &center_);
    } else if (attr_name == "scale") {
      ParseScalar(attr->ValueStr(), &pixels_per_unit_);
    } else if (attr_name == "rotate") {
      ParseScalar(attr->ValueStr(), &rotate_);
    } else if (attr_name == "supersample") {
      ParseScalar(attr->ValueStr(), &supersample_);
    } else if (attr_name == "filter") {
      ParseScalar(attr->ValueStr(), &filter_);
    } else if (attr_name == "filter_shape") {
      filter_shape_ = attr->ValueStr();
    } else if (attr_name == "temporal_filter_type") {
      temporal_filter_type_ = attr->ValueStr();
    } else if (attr_name == "temporal_filter_width") {
      ParseScalar(attr->ValueStr(), &temporal_filter_width_);
    } else if (attr_name == "quality") {
      ParseScalar(attr->ValueStr(), &quality_);
    } else if (attr_name == "passes") {
      ParseScalar(attr->ValueStr(), &passes_);
    } else if (attr_name == "temporal_samples") {
      ParseScalar(attr->ValueStr(), &temporal_samples_);
    } else if (attr_name == "background") {
      ParseArray<double, 3>(attr->ValueStr(), &background_);
    } else if (attr_name == "brightness") {
      ParseScalar(attr->ValueStr(), &brightness_);
    } else if (attr_name == "gamma") {
      ParseScalar(attr->ValueStr(), &gamma_);
    } else if (attr_name == "vibrancy") {
      ParseScalar(attr->ValueStr(), &vibrancy_);
    } else if (attr_name == "estimator_radius") {
      ParseScalar(attr->ValueStr(), &estimator_radius_);
    } else if (attr_name == "estimator_minimum") {
      ParseScalar(attr->ValueStr(), &estimator_minimum_);
    } else if (attr_name == "estimator_curve") {
      ParseScalar(attr->ValueStr(), &estimator_curve_);
    } else if (attr_name == "gamma_threshold") {
      ParseScalar(attr->ValueStr(), &gamma_threshold_);
    } else if (attr_name == "palette_mode") {
      palette_mode_ = attr->ValueStr();
    } else if (attr_name == "interpolation_type") {
      interpolation_type_ = attr->ValueStr();
    } else if (attr_name == "url") {
      url_ = attr->ValueStr();
    } else if (attr_name == "nick") {
      nick_ = attr->ValueStr();
    } else if (attr_name == "notes") {
      notes_ = attr->ValueStr();
    } else {
      BOOST_THROW_EXCEPTION(unsupported_attribute_error()
          << error_message("Unsupported attribute: " + attr_name + "=\"" +
            attr->Value() + "\""));
    }
  }

  // elements
  for (const TiXmlElement* e = root->FirstChildElement();
       e != NULL;
       e = e->NextSiblingElement()) {
    const string element_name(e->ValueStr());

    if (element_name == "edit") {
      // ignore this
    } else if (element_name == "xform") {
      std::auto_ptr<Xform> xform(new Xform());
      xform->Parse(e);
      xforms_.push_back(xform.release());
    } else if (element_name == "finalxform") {
      scoped_ptr<Xform> xform(new Xform());
      xform->Parse(e);
      swap(xform, final_xform_);
    } else if (element_name == "color") {
      size_t index = 0;
      array<double, 3> color;

      for (const TiXmlAttribute* attr = e->FirstAttribute();
          attr != NULL;
          attr = attr->Next()) {
        const string attr_name(attr->Name());

        if (attr_name == "index") {
          ParseScalar(attr->ValueStr(), &index);
        } else if (attr_name == "rgb") {
          ParseArray<double, 3>(attr->ValueStr(), &color);
        } else {
          BOOST_THROW_EXCEPTION(unsupported_attribute_error()
              << error_message("Unsupported attribute: " + attr_name + "=\"" +
                attr->Value() + "\""));
        }
      }

      if (index > colors_.size()) {
        BOOST_THROW_EXCEPTION(parse_error()
            << error_message("Color index too big: " +
              boost::lexical_cast<string>(index)));
      }
      color[0] /= 255.0;
      color[1] /= 255.0;
      color[2] /= 255.0;
      colors_[index] = color;
    } else {
      BOOST_THROW_EXCEPTION(unsupported_element_error()
          << error_message("Unsupported element: " + element_name));
    }
  }
}

void Xform::Parse(const TiXmlElement* element) {
  for (const TiXmlAttribute* attr = element->FirstAttribute();
      attr != NULL;
      attr = attr->Next()) {
    const string attr_name(attr->Name());

    if (attr_name == "coefs") {
      ParseArray<double, 6>(attr->ValueStr(), &coefs_);
    } else if (attr_name == "color") {
      ParseScalar(attr->ValueStr(), &color_);
    } else if (attr_name == "symmetry") {
      double symmetry;
      ParseScalar(attr->ValueStr(), &symmetry);
      color_speed_ = (1 - symmetry) / 2;
      animate_ = symmetry > 0 ? 0 : 1;
    } else if (attr_name == "weight") {
      ParseScalar(attr->ValueStr(), &weight_);
    } else if (attr_name == "animate") {
      ParseScalar(attr->ValueStr(), &animate_);
    } else if (attr_name == "color_speed") {
      ParseScalar(attr->ValueStr(), &color_speed_);
    } else if (attr_name == "opacity") {
      ParseScalar(attr->ValueStr(), &opacity_);
    } else if (attr_name == "julian_dist") {
      ParseScalar(attr->ValueStr(), &julian_dist_);
    } else if (attr_name == "julian_power") {
      ParseScalar(attr->ValueStr(), &julian_power_);
    } else if (attr_name == "perspective_dist") {
      ParseScalar(attr->ValueStr(), &perspective_dist_);
    } else if (attr_name == "perspective_angle") {
      ParseScalar(attr->ValueStr(), &perspective_angle_);
    } else if (attr_name == "radial_blur_angle") {
      ParseScalar(attr->ValueStr(), &radial_blur_angle_);
    } else if (attr_name == "rings2_val") {
      ParseScalar(attr->ValueStr(), &rings2_val_);
    } else if (attr_name == "post") {
      post_.reset(new array<double, 6>);
      ParseArray<double, 6>(attr->ValueStr(), post_.get());
    } else {
      bool found = false;
      for (size_t i = 0; i < variations_.size(); ++i) {
        if (kVariationNames[i] == attr_name) {
          ParseScalar(attr->ValueStr(), &variations_[i]);
          found = true;
          break;
        }
      }

      if (!found) {
        BOOST_THROW_EXCEPTION(unsupported_attribute_error()
            << error_message("Unsupported attribute: " + attr_name + "=\"" +
              attr->Value() + "\""));
      }
    }
  }

  Init();
}

void Xform::Init() {
  for (size_t i = 0; i < variations_.size(); ++i) {
    if (variations_[i] != 0) non_zero_variations_.push_back(i);
  }
}
