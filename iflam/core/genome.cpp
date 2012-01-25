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
  bool BadValue(Float x) {
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

Xform::Xform(const Xform& xform) {
  coefs_ = xform.coefs_;
  variations_ = xform.variations_;
  color_ = xform.color_;
  color_speed_ = xform.color_speed_;
  opacity_ = xform.opacity_;
  weight_ = xform.weight_;
  animate_ = xform.animate_;
  julian_dist_ = xform.julian_dist_;
  julian_power_ = xform.julian_power_;
  perspective_angle_ = xform.perspective_angle_;
  perspective_dist_ = xform.perspective_dist_;
  radial_blur_angle_ = xform.radial_blur_angle_;
  rings2_val_ = xform.rings2_val_;
  if (xform.post_.get() != NULL) {
    post_.reset(new array<Float, 6>(*xform.post_));
  }

  rectangles_x_ = xform.rectangles_x_;
  rectangles_y_ = xform.rectangles_y_;
  juliascope_power_ = xform.juliascope_power_;
  juliascope_dist_ = xform.juliascope_dist_;
  fan2_x_ = xform.fan2_x_;
  fan2_y_ = xform.fan2_y_;
  curl_c1_ = xform.curl_c1_;
  curl_c2_ = xform.curl_c2_;
  parabola_height_ = xform.parabola_height_;
  parabola_width_ = xform.parabola_width_;

  Init();
}

Xform::~Xform() { }

bool Xform::Apply(Float* in, Float* out, Random* rnd) const {
  Float x = in[0];
  Float y = in[1];
  Float cc = in[2];

  const Float a = coefs_[0];
  const Float b = coefs_[2];
  const Float c = coefs_[4];
  const Float d = coefs_[1];
  const Float e = coefs_[3];
  const Float f = coefs_[5];

  { // Affine transform.
    Float x1, y1;
    x1 = x * a + y * b + c;
    y1 = x * d + y * e + f;
    x = x1;
    y = y1;
  }

  {  // Nonlinear transform
    Float x1 = 0;
    Float y1 = 0;
    const Float r2 = x * x + y * y;
    const Float r = sqrt(r2);

    for (size_t i = 0; i < non_zero_variations_.size(); ++i) {
      size_t var = non_zero_variations_[i];
      const Float w = variations_[var];

      if (w == 0) {
        continue;
      }

      Float dx;
      Float dy;

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
          Float theta = atan2(x, y);
          dx = theta / kPI;
          dy = r - 1;
          break;
        }
        case 6: // handkerchief
        {
          Float theta = atan2(x, y);
          dx = r * sin(theta + r);
          dy = r * cos(theta - r);
          break;
        }
        case 7: // heart
        {
          Float theta = atan2(x, y);
          dx = r * sin(theta * r);
          dy = - r * cos(theta * r);
          break;
        }
        case 8: // disc
        {
          Float theta = atan2(x, y);
          dx = theta * sin(kPI * r) / kPI;
          dy = theta * cos(kPI * r) / kPI;
          break;
        }
        case 10: // hyperbolic
        {
          Float theta = atan2(x, y);
          dx = sin(theta)/ r;
          dy = r * cos(theta);
          break;
        }
        case 11: // diamond
        {
          Float theta = atan2(x, y);
          dx = sin(theta) * cos(r);
          dy = cos(theta) * sin(r);
          break;
        }
        case 12: // ex
        {
          Float theta = atan2(x, y);
          Float p0 = sin(theta + r);
          Float p1 = cos(theta - r);
          dx = r * (p0*p0*p0 + p1 * p1 * p1);
          dy = r * (p0*p0*p0 - p1 * p1 * p1);
          break;
        }
        case 13: // julia
        {
          Float theta = atan2(x, y);
          Float omega = rnd->brnd() ? 0 : kPI;
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
        {
          dx = x + b * sin(y / (c * c + kEpsilon));
          dy = y + e * sin(x / (f * f + kEpsilon));
          break;
        }
        case 16: // fisheye
        {
          dx = y * 2 / (r + 1);
          dy = x * 2 / (r + 1);
          break;
        }
        case 21: // rings
        {
          Float theta = atan2(x, y);
          Float c2 = c * c;
          Float k = fmod(r + c2, 2 * c2) - c2 + r * (1 - c2);
          dx = k * cos(theta);
          dy = k * sin(theta);
          break;
        }
        case 22: // fan
        {
          Float theta = atan2(x, y);
          Float t = kPI * c * c;
          if (fmod(theta + f, t) > t / 2) {
            dx = r * cos (theta - t/2);
            dy = r * sin (theta - t/2);
          } else {
            dx = r * cos (theta + t/2);
            dy = r * sin (theta + t/2);
          }
          break;
        }
        case 25: // fan2
        {
          Float p1 = kPI * fan2_x_ * fan2_x_;
          Float p2 = fan2_y_;
          Float theta = atan2(x, y);
          Float t = theta + p2 - p1 *floor(2 * theta * p2 / p1);
          if (t > p1/2) {
            dx = r * sin(theta - p1 / 2);
            dy = r * cos(theta - p1 / 2);
          } else {
            dx = r * sin(theta + p1/2);
            dy = r * cos(theta + p1/2);
          }
          break;
        }
        case 26: // rings2
        {
          Float theta = atan2(x, y);
          Float p = rings2_val_ * rings2_val_;
          Float t = r - 2*p*floor((r + p) / (2 * p)) + r* (1-p);
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
          Float p1 = perspective_angle_;
          Float p2 = perspective_dist_;
          dx = p2 * x / (p2 - y * sin(p1));
          dy = p2 * y * cos(p1) / (p2 - y * sin(p1));
          break;
        }
        case 32: // julian
        {
          Float phi = atan2(y, x);
          Float p1 = julian_power_;
          Float p2 = julian_dist_;
          Float p3 = floor(fabs(p1) * rnd->rnd());
          Float t = (phi + 2 * kPI * p3) / p1;
          Float z = pow(r, p2 / p1);
          dx = z * cos(t);
          dy = z * sin(t);
          break;
        }
        case 33: // julia_scope
        {
          Float phi = atan2(y, x);
          Float p1 = juliascope_power_;
          Float p2 = juliascope_dist_;
          Float p3 = floor(fabs(p1) * rnd->rnd());
          Float t = (rnd->crnd() * phi + 2 * kPI * p3) / p1;
          Float z = pow(r, p2 / p1);
          dx = z * cos(t);
          dy = z * sin(t);
          break;
        }
        case 34: // blur
        {
          Float xi1 = rnd->rnd();
          Float xi2 = rnd->rnd();
          dx = xi1 * cos(2 * kPI * xi2);
          dy = xi1 * sin(2 * kPI * xi2);
          break;
        }
        case 35:  // gaussian blur
        {
          Float t1 = w * (rnd->rnd() + rnd->rnd() + rnd->rnd() + rnd->rnd() - 2);
          Float t2 = rnd->rnd();
          dx = t1 * cos(2 * kPI * t2);
          dy = t1 * sin(2 * kPI * t2);
          break;
        }
        case 36:  // radial_blur
        {
          Float phi = atan2(y, x);
          Float p1 = radial_blur_angle_ * kPI / 2;
          Float t1 = w * (rnd->rnd() + rnd->rnd() + rnd->rnd() + rnd->rnd() - 2);
          Float t2 = phi + t1 * sin(p1);
          Float t3 = t1 * cos(p1) - 1;
          dx = (r * cos(t2) + t3 * x) / w;
          dy = (r * sin(t2) + t3 * y) / w;
          break;
        }
        case 39:  // curl
        {
          Float p1 = curl_c1_;
          Float p2 = curl_c2_;
          Float t1 = 1 + p1 * x + p2 * (x * x - y * y);
          Float t2 = p1 * y + 2 * p2 * x * y;
          Float t3 = 1 / (t1 * t1 +t2 * t2);
          dx = t3 * (x * t1 + y * t2);
          dy = t3 * (y * t1 - x * t2);
          break;
        }
        case 40:  // rectangles
        {
          Float p1 = rectangles_x_;
          Float p2 = rectangles_y_;
          dx = (2 * floor(x / p1) + 1) * p1 - x;
          dy = (2 * floor(y / p2) + 1) * p2 - y;
          break;
        }
        case 45:  // blade
        {
          Float xi = rnd->rnd();
          dx = x * (cos(xi * r * w) + sin(xi * r * w));
          dy = x * (cos(xi * r * w) - sin(xi * r * w));
          break;
        }
        case 46:  // secant2
        {
          dx = x;
          dy = 1 / (w * cos(w * r));
          break;
        }
        case 48:  // cross
        {
          Float t = x*x - y*y;
          Float t1 = sqrt(1/(t*t));
          dx = t1 * x;
          dy = t1 * y;
          break;
        }
        case 53:  // parabola
        {
          Float sr = sin(r);
          Float cr = cos(r);
          dx = parabola_height_ * sr * sr * rnd->rnd();
          dy = parabola_width_ * cr * rnd->rnd();
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
    Float x1, y1;
    const array<Float, 6>& post = *post_;
    x1 = x * post[0] + y * post[2] + post[4];
    y1 = x * post[1] + y * post[3] + post[5];
    x = x1;
    y = y1;
  }

  cc = cc * (1 - color_speed_) + color_ * color_speed_;

  bool good = true;
  if (BadValue(x) || BadValue(y)) {
    good = false;
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

Genome::Genome(const Genome& genome) {
  brightness_ = genome.brightness_;
  contrast_ = genome.contrast_;
  gamma_ = genome.gamma_;
  gamma_threshold_ = genome.gamma_threshold_;
  highlight_power_ = genome.highlight_power_;
  passes_ = genome.passes_;
  pixels_per_unit_ = genome.pixels_per_unit_;
  quality_ = genome.quality_;
  vibrancy_ = genome.vibrancy_;
  zoom_ = genome.zoom_;
  name_ = genome.name_;
  time_ = genome.time_;
  size_ = genome.size_;
  center_ = genome.center_;
  background_ = genome.background_;
  colors_ = genome.colors_;
  rotate_ = genome.rotate_;
  supersample_ = genome.supersample_;
  filter_ = genome.filter_;
  filter_shape_ = genome.filter_shape_;
  temporal_filter_type_ = genome.temporal_filter_type_;
  temporal_filter_width_ = genome.temporal_filter_width_;
  temporal_samples_ = genome.temporal_samples_;
  estimator_radius_ = genome.estimator_radius_;
  estimator_minimum_ = genome.estimator_minimum_;
  estimator_curve_ = genome.estimator_curve_;
  palette_mode_ = genome.palette_mode_;
  interpolation_type_ = genome.interpolation_type_;
  url_ = genome.url_;
  nick_ = genome.nick_;
  notes_ = genome.notes_;

  for (size_t i = 0; i < genome.xforms_.size(); ++i) {
    xforms_.push_back(new Xform(genome.xforms_[i]));
  }
  if (genome.final_xform_.get() != NULL) {
    final_xform_.reset(new Xform(*genome.final_xform_));
  }
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
      ParseArray<Float, 2>(attr->ValueStr(), &center_);
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
      ParseArray<Float, 3>(attr->ValueStr(), &background_);
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
    } else if (attr_name == "genebank" || attr_name == "brood") {
      // ignore
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
      array<Float, 3> color;

      for (const TiXmlAttribute* attr = e->FirstAttribute();
          attr != NULL;
          attr = attr->Next()) {
        const string attr_name(attr->Name());

        if (attr_name == "index") {
          ParseScalar(attr->ValueStr(), &index);
        } else if (attr_name == "rgb") {
          ParseArray<Float, 3>(attr->ValueStr(), &color);
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
      ParseArray<Float, 6>(attr->ValueStr(), &coefs_);
    } else if (attr_name == "color") {
      ParseScalar(attr->ValueStr(), &color_);
    } else if (attr_name == "symmetry") {
      Float symmetry;
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
    } else if (attr_name == "rectangles_x") {
      ParseScalar(attr->ValueStr(), &rectangles_x_);
    } else if (attr_name == "rectangles_y") {
      ParseScalar(attr->ValueStr(), &rectangles_y_);
    } else if (attr_name == "juliascope_power") {
      ParseScalar(attr->ValueStr(), &juliascope_power_);
    } else if (attr_name == "juliascope_dist") {
      ParseScalar(attr->ValueStr(), &juliascope_dist_);
    } else if (attr_name == "fan2_x") {
      ParseScalar(attr->ValueStr(), &fan2_x_);
    } else if (attr_name == "fan2_y") {
      ParseScalar(attr->ValueStr(), &fan2_y_);
    } else if (attr_name == "curl_c1") {
      ParseScalar(attr->ValueStr(), &curl_c1_);
    } else if (attr_name == "curl_c2") {
      ParseScalar(attr->ValueStr(), &curl_c2_);
    } else if (attr_name == "parabola_height") {
      ParseScalar(attr->ValueStr(), &parabola_height_);
    } else if (attr_name == "parabola_width") {
      ParseScalar(attr->ValueStr(), &parabola_width_);
    } else if (attr_name == "post") {
      post_.reset(new array<Float, 6>);
      ParseArray<Float, 6>(attr->ValueStr(), post_.get());
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
