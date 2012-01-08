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
  template<typename ArrayType, int size>
  bool ParseArray(const string& str,
      boost::array<ArrayType, size>* array,
      string* error) {
    vector<string> strs;
    boost::split(strs, str, boost::is_space());

    if (strs.size() != size) {
      *error = "Wrong vector length in " + str;
      return false;
    }
    for (int i = 0; i < strs.size(); ++i) {
      (*array)[i] = boost::lexical_cast<ArrayType>(strs[i]);
    }

    return true;
  }

  template<typename T>
  bool ParseScalar(
      const string& str,
      T* d,
      string* error) {
    // TODO: error checks
    *d = boost::lexical_cast<T>(str);
    return true;
  }
}

bool Genome::Read(string file_name, string* error) {
  error->clear();

  TiXmlDocument doc(file_name.c_str());
  if (!doc.LoadFile()) {
    return false;
  }

  const TiXmlElement* root = doc.RootElement();

  for (const TiXmlAttribute* attr = root->FirstAttribute();
       attr != NULL;
       attr = attr->Next()) {
    const string attr_name(attr->Name());

    if (attr_name == "name") {
      name_ = attr->Value();
    } else if (attr_name == "time") {
      if (!ParseScalar(attr->ValueStr(), &time_, error)) {
        return false;
      }
    } else if (attr_name == "size") {
      if (!ParseArray<int, 2>(attr->ValueStr(), &size_, error)) {
        return false;
      }
    } else if (attr_name == "center") {
      if (!ParseArray<double, 2>(attr->ValueStr(), &center_, error)) {
        return false;
      }
    } else if (attr_name == "scale") {
      if (!ParseScalar(attr->ValueStr(), &scale_, error)) {
        return false;
      }
    } else if (attr_name == "rotate") {
      if (!ParseScalar(attr->ValueStr(), &rotate_, error)) {
        return false;
      }
    } else if (attr_name == "supersample") {
      if (!ParseScalar(attr->ValueStr(), &supersample_, error)) {
        return false;
      }
    } else if (attr_name == "filter") {
      if (!ParseScalar(attr->ValueStr(), &filter_, error)) {
        return false;
      }
    } else if (attr_name == "filter_shape") {
      filter_shape_ = attr->ValueStr();
    } else if (attr_name == "temporal_filter_type") {
      temporal_filter_type_ = attr->ValueStr();
    } else if (attr_name == "temporal_filter_width") {
      if (!ParseScalar(attr->ValueStr(), &temporal_filter_width_, error)) {
        return false;
      }
    } else if (attr_name == "quality") {
      if (!ParseScalar(attr->ValueStr(), &quality_, error)) {
        return false;
      }
    } else if (attr_name == "passes") {
      if (!ParseScalar(attr->ValueStr(), &passes_, error)) {
        return false;
      }
    } else if (attr_name == "temporal_samples") {
      if (!ParseScalar(attr->ValueStr(), &temporal_samples_, error)) {
        return false;
      }
    } else if (attr_name == "background") {
      if (!ParseArray<double, 3>(attr->ValueStr(), &background_, error)) {
        return false;
      }
    } else if (attr_name == "brightness") {
      if (!ParseScalar(attr->ValueStr(), &brightness_, error)) {
        return false;
      }
    } else if (attr_name == "gamma") {
      if (!ParseScalar(attr->ValueStr(), &gamma_, error)) {
        return false;
      }
    } else if (attr_name == "vibrancy") {
      if (!ParseScalar(attr->ValueStr(), &vibrancy_, error)) {
        return false;
      }
    } else if (attr_name == "estimator_radius") {
      if (!ParseScalar(attr->ValueStr(), &estimator_radius_, error)) {
        return false;
      }
    } else if (attr_name == "estimator_minimum") {
      if (!ParseScalar(attr->ValueStr(), &estimator_minimum_, error)) {
        return false;
      }
    } else if (attr_name == "estimator_curve") {
      if (!ParseScalar(attr->ValueStr(), &estimator_curve_, error)) {
        return false;
      }
    } else if (attr_name == "gamma_threshold") {
      if (!ParseScalar(attr->ValueStr(), &gamma_threshold_, error)) {
        return false;
      }
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
      *error = "Unsupported attribute: " + attr_name + "=\"" +
        attr->Value() + "\"";
      return false;
    }
  }

  return true;
}
