package flam;

/**
 */
public interface View2D {
    int getHeight();
    int getWidth();
    void set(int x, int y, double d);
    double get(int x, int y);
}
