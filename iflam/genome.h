#ifndef __GENOME_H__
#define __GENOME_H__

#include <string>
#include <vector>
#include <boost/scoped_ptr.hpp>
#include <boost/utility.hpp>
#include <boost/array.hpp>

class Genome : boost::noncopyable {
public:
    Genome();
    ~Genome();

    void Randomize();

    bool Read(std::string file_name, std::string* error_message);
private:

    std::string name_;
    double time_;
    boost::array<int, 2> size_;
    boost::array<double, 2> center_;
    boost::array<double, 3> background_;
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
};


#endif
