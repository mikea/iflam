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

struct parse_error : virtual boost::exception, virtual std::exception { };

class Xform : boost::noncopyable {
  public:
    enum { kVariationsCount = 99 };
    Xform();
    ~Xform();

    void Parse(const TiXmlElement* element);

  private:

    boost::array<double, 6> coefs_;
    boost::array<double, kVariationsCount> variations_;
    double color_;
    double color_speed_;
    double animate_;  // is it bool?
    double weight_;
    double opacity_;
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

    // Throws ReadError.
    void Read(std::string file_name);
private:
    typedef boost::array<double, 3> Color;

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
    int quality_;
    int passes_;
    int temporal_samples_;
    double brightness_;
    double gamma_;
    double vibrancy_;
    double estimator_radius_;
    double estimator_minimum_;
    double estimator_curve_;
    double gamma_threshold_;
    std::string palette_mode_;
    std::string interpolation_type_;
    std::string url_;
    std::string nick_;
    std::string notes_;

    boost::ptr_vector<Xform> xforms_;
    boost::scoped_ptr<Xform> final_xform_;
};


#endif
