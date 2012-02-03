#ifndef __COMPONENT__H_
#define __COMPONENT__H_

#include <string>
#include <boost/smart_ptr.hpp>

class Controller;
class Genome;
class Model;
class RenderState;
class RenderBuffer;

class Component {
  public:
    Component(boost::shared_ptr<Controller> controller);
    void Tick();
    void SetSize(size_t width, size_t height);

    boost::shared_ptr<Genome> genome() const {
      return genome_;
    }

    boost::shared_ptr<RenderBuffer> render_buffer() const {
      return buffer_;
    }

    boost::shared_ptr<Controller> controller() const {
      return controller_;
    }

  private:
    void Reset(boost::shared_ptr<Genome> genome);

    boost::shared_ptr<Controller> controller_;
    boost::shared_ptr<Model> model_;
    boost::shared_ptr<Genome> genome_;
    boost::scoped_ptr<RenderState> state_;
    boost::shared_ptr<RenderBuffer> buffer_;
    size_t width_;
    size_t height_;
};

#endif

