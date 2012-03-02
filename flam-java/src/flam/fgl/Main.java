package flam.fgl;

import com.jogamp.opengl.util.FBObject;
import com.jogamp.opengl.util.FPSAnimator;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.gl2.GLUgl2;
import javax.swing.*;

/**
 */
public class Main implements GLEventListener {
// ------------------------------ FIELDS ------------------------------

    private FBObject fbo1;
    private FBObject fbo2;

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface GLEventListener ---------------------

    @Override
    public void init(GLAutoDrawable drawable) {
//        drawable.setGL(new DebugGL2(drawable.getGL().getGL2()));
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        fbo1.destroy(gl);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        render(drawable.getGL().getGL2());
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        if (fbo1 != null) {
            fbo1.destroy(gl);
        }
        fbo1 = createFBO(width, height, gl);
        if (fbo2 != null) {
            fbo2.destroy(gl);
        }
        fbo2 = createFBO(width, height, gl);
    }

// -------------------------- OTHER METHODS --------------------------

    private FBObject createFBO(int width, int height, GL2 gl) {
        FBObject fbo = new FBObject(width, height);
        fbo.init(gl);
        fbo.attachTexture2D(gl, 0, GL2.GL_LINEAR, GL2.GL_LINEAR, 0, 0);
        if (!fbo.isStatusValid()) {
            throw new IllegalStateException();
        }
        fbo.unbind(gl);
        return fbo;
    }

    private void iter(final GL2 gl, final FBObject src, FBObject dst) {
        renderToFBO(gl, dst, new Runnable() {
            @Override
            public void run() {
                useFbo(gl, src, 0, new Runnable() {
                    @Override
                    public void run() {
                        gl.glClearColor(0, 0, 0, 0);
                        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
                        gl.glColor4d(1, 1, 1, 1);
                        gl.glLoadIdentity();
                        GLUgl2.createGLU(gl).gluOrtho2D(0, 1, 0, 1);

                        gl.glEnable(GL.GL_BLEND);
                        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

                        // a b c d e f (r g b)?
/*
                        double[][] coefs = new double[][]{
                                new double[]{-0.2960, 0.0469, -0.0469, -0.2960, 0.3791, 0.5687, 1.0602, 1.0602, 1.0602},
                                new double[]{0.8302, 0.4091, -0.4091, 0.8302, -0.0674, 0.3319, 1.0602, 1.0602, 1.0602},
                        };
*/
                        double[][] coefs = new double[][]{
                                new double[]{ 0.6416,  0.3591, -0.3591,  0.6416, 0.1480,  0.3403, 1.1901, 1.2867, 1.3398},
                                new double[]{ 0.1906, -0.2554,  0.2554,  0.1906, 0.4162,  0.6122, 1.1901, 1.2867, 0.000},
                                new double[]{ 0.1681, -0.2279,  0.2279,  0.1681, 0.4531, -0.0205, 1.1901, 0.4087, 0.425},
                                new double[]{-0.2848, -0.0141,  0.0141, -0.2848, 0.3362,  0.8164, 0.3780, 0.4087, 1.3398},
                                new double[]{ 0.3672,  0.0051, -0.0051,  0.3672, 0.0776,  0.1726, 1.1901, 1.2867, 1.339},
                        };
                        for (int i = 0; i < coefs.length; ++i) {
                            double[] row = coefs[i];
                            renderRow(row);
                        }

                        gl.glDisable(GL.GL_BLEND);
                    }

                    private void renderRow(double[] row) {
                        double[] xy1 = new double[] {0, 0};
                        double[] xy2 = new double[] {0, 0};

                        gl.glBegin(GL2.GL_QUADS);

                        xy1[0] = 0;
                        xy1[1] = 0;
                        transform(row, xy1, xy2);
                        gl.glTexCoord2d(xy1[0], xy1[1]);
                        gl.glVertex2d(xy2[0], xy2[1]);

                        xy1[0] = 0;
                        xy1[1] = 1;
                        transform(row, xy1, xy2);
                        gl.glTexCoord2d(xy1[0], xy1[1]);
                        gl.glVertex2d(xy2[0], xy2[1]);

                        xy1[0] = 1;
                        xy1[1] = 1;
                        transform(row, xy1, xy2);
                        gl.glTexCoord2d(xy1[0], xy1[1]);
                        gl.glVertex2d(xy2[0], xy2[1]);


                        xy1[0] = 1;
                        xy1[1] = 0;
                        transform(row, xy1, xy2);
                        gl.glTexCoord2d(xy1[0], xy1[1]);
                        gl.glVertex2d(xy2[0], xy2[1]);

                        gl.glEnd();
                    }
                });
            }
        });
    }

    private void transform(double[] row, double[] in, double[] out) {
        double x1 = in[0];
        double y1 = in[1];

        double a = row[0];
        double b = row[1];
        double c = row[2];
        double d = row[3];
        double e = row[4];
        double f = row[5];

        double x2 = a * x1 + b * y1 + e;
        double y2 = c * x1 + d * y1 + f;

        out[0] = x2;
        out[1] = y2;
    }

    private void render(final GL2 gl) {
        // start by filling in fbo1
        renderToFBO(gl, fbo1, new Runnable() {
            @Override
            public void run() {
                gl.glClearColor(1, 1, 1, 1);
                gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
                gl.glLoadIdentity();
            }
        });

        for (int i = 0; i < 10; ++i) {
            iter(gl, fbo1, fbo2);
            FBObject tmp = fbo1;
            fbo1 = fbo2;
            fbo2 = tmp;
        }

        showFBO(gl, fbo1, 0);
    }

    private static void renderToFBO(GL2 gl, FBObject fbo, Runnable runnable) {
        fbo.bind(gl);
        try {
            runnable.run();
        } finally {
            fbo.unbind(gl);
        }
    }

    private static void showFBO(final GL2 gl, FBObject fbo, int texture) {
        useFbo(gl, fbo, texture, new Runnable() {
            @Override
            public void run() {
                gl.glClearColor(0, 0, 1, 1);
                gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
                gl.glLoadIdentity();
                {
                    gl.glColor3f(1, 1, 1);
                    gl.glBegin(GL2.GL_QUADS);
                    gl.glTexCoord2d(0.0, 0.0);
                    gl.glVertex2d(-1.0, -1.0);

                    gl.glTexCoord2d(0.0, 1.0);
                    gl.glVertex2d(-1.0, 1.0);

                    gl.glTexCoord2d(1.0, 1.0);
                    gl.glVertex2d(1.0, 1.0);

                    gl.glTexCoord2d(1.0, 0.0);
                    gl.glVertex2d(1.0, -1.0);
                    gl.glEnd();
                }
            }
        });
    }

    private static void useFbo(GL2 gl, FBObject fbo, int texture, Runnable runnable) {
        gl.glEnable(GL.GL_TEXTURE_2D);
        fbo.use(gl, texture);
        try {
            runnable.run();
        } finally {
            fbo.unuse(gl);
            gl.glDisable(GL.GL_TEXTURE_2D);
        }
    }

// --------------------------- main() method ---------------------------

    public static void main(String[] args) {
        GLProfile glp = GLProfile.getDefault();
        GLCapabilities caps = new GLCapabilities(glp);
        GLCanvas canvas = new GLCanvas(caps);
        canvas.addGLEventListener(new Main());

        FPSAnimator animator = new FPSAnimator(canvas, 60);
        animator.add(canvas);
        animator.start();

        JFrame frame = new JFrame("A window");
        frame.add(canvas);
        frame.setSize(800, 600);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
