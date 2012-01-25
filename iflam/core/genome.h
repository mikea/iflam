#ifndef __GENOME_H__
#define __GENOME_H__

#include <string>
#include <vector>
#include <boost/scoped_ptr.hpp>
#include <boost/utility.hpp>
#include <boost/ptr_container/ptr_vector.hpp>
#include <boost/mpl/list.hpp>

#include "common.h"

class TiXmlElement;

template<typename Type>
struct PropertyInfo {
  typedef Type type;
};

#define DECLARE_PROPERTY(_Container, _Type, _name) \
  _Type _name##_; \
  struct _name##_property_info : public PropertyInfo<_Type> { \
    static const char* name; \
    static _Type* ptr(_Container* container) { return &container->_name##_;} \
    static const _Type* ptr(const _Container& container) { return &container._name##_;} \
  };

#define DEFINE_PROPERTY(_Container, _Type, _name) \
  const char* _Container::_name##_property_info::name = #_name;

class Xform {
  public:
    enum { kVariationsCount = 99 };
    Xform();
    explicit Xform(const Xform& genome);

    ~Xform();

    void Parse(const TiXmlElement* element);

    Float weight() const { return weight_; }
    Float opacity() const { return opacity_; }

    bool Apply(Float* in, Float* out, Random* rnd) const;

    array<Float, 6>* mutable_coefs() { return &coefs_; }
  private:
    void Init();

    array<Float, 6> coefs_;
    array<Float, kVariationsCount> variations_;
    std::vector<int> non_zero_variations_;
    Float color_;
    Float color_speed_;
    Float opacity_;
    Float weight_;
    Float animate_;  // is it bool?
    Float julian_dist_;
    Float julian_power_;
    Float perspective_angle_;
    Float perspective_dist_;
    Float radial_blur_angle_;
    Float rings2_val_;
    Float rectangles_x_;
    Float rectangles_y_;
    Float juliascope_power_;
    Float juliascope_dist_;
    Float fan2_x_;
    Float fan2_y_;
    Float curl_c1_;
    Float curl_c2_;
    Float parabola_height_;
    Float parabola_width_;
    boost::scoped_ptr<array<Float, 6> > post_;

    DECLARE_PROPERTY(Xform, Float, flower_petals);
    DECLARE_PROPERTY(Xform, Float, flower_holes);

    typedef boost::mpl::list<
      flower_petals_property_info,
      flower_holes_property_info
      > PropertyInfos;
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
