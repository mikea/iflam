#ifndef __GENOME_H__
#define __GENOME_H__

#include <string>
#include <vector>
#include <boost/scoped_ptr.hpp>
#include <boost/utility.hpp>
#include <boost/ptr_container/ptr_vector.hpp>
#include <boost/mpl/list.hpp>
#include <boost/preprocessor/seq/for_each.hpp>
#include <boost/preprocessor/tuple/elem.hpp>
#include <boost/preprocessor.hpp>

#include "common.h"

class TiXmlElement;

template<typename Type>
struct PropertyInfo {
  typedef Type type;
};

/*
#define DECLARE_PROPERTY(_Container, _Type, _name) \
  public: \
  const _Type& get_##_name() const { return _name##_; } \
  private: \
  _Type _name##_; \
  struct _name : public PropertyInfo<_Type> { \
    static const char* name; \
    static _Type* ptr(_Container* container) { return &container->_name##_;} \
    static const _Type* ptr(const _Container& container) { return &container._name##_;} \
  };

#define DEFINE_PROPERTY(_Container, _Type, _name) \
  const char* _Container::_name::name = #_name;

#define PROPERTY_LIST(properties...) \
    typedef boost::mpl::list<properties> PropertyList;
*/

#define __DECLARE_PROPERTY_MEMBERS(_Container, _Type, _name) \
  public: \
  const _Type& BOOST_PP_CAT(get_, _name)() const { return BOOST_PP_CAT(_name, _); } \
  private: \
  _Type BOOST_PP_CAT(_name, _); \
  struct _name : public PropertyInfo<_Type> { \
    static const char* name; \
    static _Type* ptr(_Container* container) { \
      return &container->BOOST_PP_CAT(_name, _); \
    } \
    static const _Type* ptr(const _Container& container) { \
      return &container.BOOST_PP_CAT(_name, _);\
    } \
  };

#define _PROPERTY_NAME(_tuple) BOOST_PP_TUPLE_ELEM(2, 1, _tuple)

#define _DECLARE_PROPERTY_MEMBERS(r, _Container, _tuple) \
  __DECLARE_PROPERTY_MEMBERS( \
      _Container, \
      BOOST_PP_TUPLE_ELEM(2, 0, _tuple), \
      _PROPERTY_NAME(_tuple))

#define _PROPERTY_LIST(_r, _data, _i, _tuple) \
  BOOST_PP_COMMA_IF(_i) _PROPERTY_NAME(_tuple)

#define _DEFINE_PROPERTY(r, _Container, _tuple) \
  const char* _Container::_PROPERTY_NAME(_tuple)::name = BOOST_PP_STRINGIZE(_PROPERTY_NAME(_tuple));

///////////////////////////////////////////////

#define PROPERTY(_Type, _name) ((_Type, _name))

#define DECLARE_PROPERTIES(_Container, _properties...) \
  BOOST_PP_SEQ_FOR_EACH(_DECLARE_PROPERTY_MEMBERS, _Container, _properties) \
  typedef boost::mpl::list< \
    BOOST_PP_SEQ_FOR_EACH_I(_PROPERTY_LIST, _, _properties) \
    > PropertyList;

#define DEFINE_PROPERTIES(_Container, _properties...) \
  BOOST_PP_SEQ_FOR_EACH(_DEFINE_PROPERTY, _Container, _properties)

class Xform {
  public:
    enum { kVariationsCount = 99 };
    Xform();
    explicit Xform(const Xform& genome);

    ~Xform();

    void Parse(const TiXmlElement* element);

    bool Apply(Float* in, Float* out, Random* rnd) const;

    array<Float, 6>* mutable_coefs() { return &coefs_; }
  private:
    void Init();

    array<Float, 6> coefs_;
    array<Float, kVariationsCount> variations_;
    std::vector<int> non_zero_variations_;
    boost::scoped_ptr<array<Float, 6> > post_;

    DECLARE_PROPERTIES(Xform,
        PROPERTY(Float, animate) // is it bool?
        PROPERTY(Float, color)
        PROPERTY(Float, color_speed)
        PROPERTY(Float, curl_c1)
        PROPERTY(Float, curl_c2)
        PROPERTY(Float, fan2_x)
        PROPERTY(Float, fan2_y)
        PROPERTY(Float, flower_holes)
        PROPERTY(Float, flower_petals)
        PROPERTY(Float, julian_dist)
        PROPERTY(Float, julian_power)
        PROPERTY(Float, juliascope_dist)
        PROPERTY(Float, juliascope_power)
        PROPERTY(Float, opacity)
        PROPERTY(Float, parabola_height)
        PROPERTY(Float, parabola_width)
        PROPERTY(Float, perspective_angle)
        PROPERTY(Float, perspective_dist)
        PROPERTY(Float, radial_blur_angle)
        PROPERTY(Float, rectangles_x)
        PROPERTY(Float, rectangles_y)
        PROPERTY(Float, rings2_val)
        PROPERTY(Float, weight)
        );
};

class Genome {
public:
    Genome();
    explicit Genome(const Genome& genome);

    ~Genome();

    void Randomize();

    // Throws error.
    void Read(std::string file_name);

    const array<Float, 2>& center() const { return center_; }
    Float pixels_per_unit() const { return pixels_per_unit_; }
    Float zoom() const { return zoom_; }
    const boost::ptr_vector<Xform>& xforms() const { return xforms_; }
    bool is_chaos_enabled() const { return false; }  // todo: chaos
    bool has_final_xform() const { return final_xform_.get() != NULL; }
    const Xform& final_xform() const { return *final_xform_; }
    const Color& color(size_t c) const { return colors_[c]; }
    Float vibrancy() const { return vibrancy_; }
    Float gamma_threshold() const { return gamma_threshold_; }
    Float gamma() const { return gamma_; }
    Float highlight_power() const { return highlight_power_; }
    Float contrast() const { return contrast_; }
    Float brightness() const { return brightness_; }
    const Color& background() const { return background_; }
    const array<int, 2>& size() const { return size_; }
    Float rotate() const {return rotate_; }

    void Magnify(Float magnification) { zoom_ += magnification; }
    void Rotate(Float rotation) { rotate_ += rotation; }
    void Move(Float deltaX, Float deltaY) {
      center_[0] += deltaX;
      center_[1] += deltaY;
    }

    boost::ptr_vector<Xform>* mutable_xforms() { return &xforms_;}

private:
    Float brightness_;
    Float contrast_;
    Float gamma_;
    Float gamma_threshold_;
    Float highlight_power_;
    int passes_;
    Float pixels_per_unit_;
    int quality_;
    Float vibrancy_;
    Float zoom_;

    std::string name_;
    Float time_;
    array<int, 2> size_;
    array<Float, 2> center_;
    Color background_;
    array<Color, 256> colors_;
    Float rotate_;
    int supersample_;
    int filter_;
    std::string filter_shape_;
    std::string temporal_filter_type_;
    int temporal_filter_width_;
    int temporal_samples_;
    Float estimator_radius_;
    Float estimator_minimum_;
    Float estimator_curve_;
    std::string palette_mode_;
    std::string interpolation_type_;
    std::string url_;
    std::string nick_;
    std::string notes_;

    boost::ptr_vector<Xform> xforms_;
    boost::scoped_ptr<Xform> final_xform_;
};


#endif
