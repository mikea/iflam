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

    DECLARE_PROPERTY(Xform, Float, animate); // is it bool?
    DECLARE_PROPERTY(Xform, Float, color);
    DECLARE_PROPERTY(Xform, Float, color_speed);
    DECLARE_PROPERTY(Xform, Float, curl_c1);
    DECLARE_PROPERTY(Xform, Float, curl_c2);
    DECLARE_PROPERTY(Xform, Float, fan2_x);
    DECLARE_PROPERTY(Xform, Float, fan2_y);
    DECLARE_PROPERTY(Xform, Float, flower_holes);
    DECLARE_PROPERTY(Xform, Float, flower_petals);
    DECLARE_PROPERTY(Xform, Float, julian_dist);
    DECLARE_PROPERTY(Xform, Float, julian_power);
    DECLARE_PROPERTY(Xform, Float, juliascope_dist);
    DECLARE_PROPERTY(Xform, Float, juliascope_power);
    DECLARE_PROPERTY(Xform, Float, opacity);
    DECLARE_PROPERTY(Xform, Float, parabola_height);
    DECLARE_PROPERTY(Xform, Float, parabola_width);
    DECLARE_PROPERTY(Xform, Float, perspective_angle);
    DECLARE_PROPERTY(Xform, Float, perspective_dist);
    DECLARE_PROPERTY(Xform, Float, radial_blur_angle);
    DECLARE_PROPERTY(Xform, Float, rectangles_x);
    DECLARE_PROPERTY(Xform, Float, rectangles_y);
    DECLARE_PROPERTY(Xform, Float, rings2_val);
    DECLARE_PROPERTY(Xform, Float, weight);

    PROPERTY_LIST(
        animate,
        color,
        color_speed,
        curl_c1,
        curl_c2,
        fan2_x,
        fan2_y,
        flower_holes,
        flower_petals,
        julian_dist,
        julian_power,
        juliascope_power,
        juliascope_dist,
        opacity,
        parabola_height,
        parabola_width,
        perspective_angle,
        perspective_dist,
        radial_blur_angle,
        rectangles_x,
        rectangles_y,
        rings2_val,
        weight
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
