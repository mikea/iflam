#ifndef __GENOME_H__
#define __GENOME_H__

#include <string>
#include <vector>
#include <boost/scoped_ptr.hpp>
#include <boost/utility.hpp>
#include <boost/array.hpp>
#include <boost/ptr_container/ptr_vector.hpp>
#include <boost/exception/all.hpp>

class TiXmlElement;

struct error : virtual boost::exception, virtual std::exception { };

typedef boost::array<double, 3> Color;

class Xform : boost::noncopyable {
  public:
    enum { kVariationsCount = 99 };
    Xform();
    ~Xform();

    void Parse(const TiXmlElement* element);

    double weight() const { return weight_; }
    double opacity() const { return opacity_; }

    bool Apply(double* in, double* out) const;
  private:

    boost::array<double, 6> coefs_;
    boost::array<double, kVariationsCount> variations_;
    double color_;
    double color_speed_;
    double opacity_;
    double weight_;
    double animate_;  // is it bool?
    double julian_dist_;
    double julian_power_;
    double perspective_angle_;
    double perspective_dist_;
    double radial_blur_angle_;
    double rings2_val_;
    boost::scoped_ptr<boost::array<double, 6> > post_;
};

class Genome : boost::noncopyable {
public:
    Genome();
    ~Genome();

    void Randomize();

    // Throws error.
    void Read(std::string file_name);

    const boost::array<double, 2> center() const { return center_; }
    double pixels_per_unit() const { return pixels_per_unit_; }
    double zoom() const { return zoom_; }
    const boost::ptr_vector<Xform>& xforms() const { return xforms_; }
    bool is_chaos_enabled() const { return true; }  // todo: chaos
    bool has_final_xform() const { return final_xform_.get() != NULL; }
    const Xform& final_xform() const { return *final_xform_; }
    const Color& color(size_t c) const { return colors_[c]; }

private:
    double brightness_;
    double contrast_;
    double gamma_;
    double gamma_threshold_;
    int passes_;
    double pixels_per_unit_;
    int quality_;
    double vibrancy_;
    double zoom_;

    std::string name_;
    double time_;
    boost::array<int, 2> size_;
    boost::array<double, 2> center_;
    boost::array<double, 3> background_;
    boost::array<Color, 256> colors_;
    double scale_;
    double rotate_;
    int supersample_;
    int filter_;
    std::string filter_shape_;
    std::string temporal_filter_type_;
    int temporal_filter_width_;
    int temporal_samples_;
    double estimator_radius_;
    double estimator_minimum_;
    double estimator_curve_;
    std::string palette_mode_;
    std::string interpolation_type_;
    std::string url_;
    std::string nick_;
    std::string notes_;

    boost::ptr_vector<Xform> xforms_;
    boost::scoped_ptr<Xform> final_xform_;
};


#endif
