package flam;

import java.awt.*;

/**
 */
public interface RenderBuffer {
    int getWidth();
    int getHeight();
    void reset();
    Image getImage();
}
