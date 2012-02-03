#ifndef __CONTROLLER_H__
#define __CONTROLLER_H__

#include <string>
#include <boost/filesystem.hpp>
#include <boost/smart_ptr.hpp>
#include "common.h"

class Genome;

class Model {
  public:
    Model() { }
    boost::shared_ptr<Genome> genome() const { return genome_; }
    void set_genome(boost::shared_ptr<Genome> genome) { genome_ = genome; }
  private:
    boost::shared_ptr<Genome> genome_;
};

class Controller {
  public:
    Controller() : model_(new Model()) { }
    Controller(boost::shared_ptr<Model> model) : model_(model) { }
    virtual ~Controller() { }
    virtual void Tick() = 0;
    virtual void Next() = 0;
    virtual std::string GetWindowTitle() = 0;

    boost::shared_ptr<Model> model() const { return model_; }

  protected:
    boost::shared_ptr<Model> model_;
};

class SlideshowController : public Controller {
  public:
    SlideshowController(const std::string& dir) : dir_(dir) {
      LoadRandomSheep();
    }
    ~SlideshowController() { }

    virtual void Tick();

    virtual void Next() {
      LoadRandomSheep();
    }

    virtual std::string GetWindowTitle() { return current_path_; }

  private:
    void LoadRandomSheep();
    Random rnd_;
    const boost::filesystem::path dir_;
    double last_change_;
    std::string current_path_;
};


#endif

