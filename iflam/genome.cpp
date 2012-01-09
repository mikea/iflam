#include <boost/algorithm/string.hpp>
#include <boost/random/uniform_int_distribution.hpp>
#include <boost/lexical_cast.hpp>
#include <iostream>
#include "tinyxml/tinyxml.h"

#include "genome.h"

using std::string;
using std::vector;

Genome::Genome() {
}

Genome::~Genome() {
}

namespace {
  template<typename ArrayType, size_t size>
  void ParseArray(const string& str, boost::array<ArrayType, size>* array) {
    vector<string> strs;
    boost::split(strs, str, boost::is_space());
    if (strs.size() != size) {
      throw ReadError("Wrong vector length in " + str);
    }
    for (size_t i = 0; i < strs.size(); ++i) {
      (*array)[i] = boost::lexical_cast<ArrayType>(strs[i]);
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
    throw ReadError("Can't load file: " + file_name);
  }

  const TiXmlElement* root = doc.RootElement();

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
      ParseScalar(attr->ValueStr(), &scale_);
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
      throw ReadError("Unsupported attribute: " + attr_name + "=\"" +
        attr->Value() + "\"");
    }
  }

  throw ReadError("Element parsing not yet implemented.");
}
