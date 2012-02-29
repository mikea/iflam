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
        fbo.attachTexture2D(gl, 0, GL2.GL_NEAREST, GL2.GL_NEAREST, 0, 0);
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
                        gl.glClearColor(0, 0, 0, 1);
                        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
                        gl.glColor4d(1, 1, 1, 1);
                        gl.glLoadIdentity();
                        {
                            gl.glBegin(GL2.GL_QUADS);
                            gl.glTexCoord2d(0.0, 0.0);
                            gl.glVertex2d(-1.0, -0.7);

                            gl.glTexCoord2d(0.0, 1.0);
                            gl.glVertex2d(-0.9, 0);

                            gl.glTexCoord2d(1.0, 1.0);
                            gl.glVertex2d(0.4, 0.3);

                            gl.glTexCoord2d(1.0, 0.0);
                            gl.glVertex2d(0, -0.9);
                            gl.glEnd();
                        }
                        {
                            gl.glBegin(GL2.GL_QUADS);
                            gl.glTexCoord2d(0.0, 0.0);
                            gl.glVertex2d(-0.5, 0.0);

                            gl.glTexCoord2d(0.0, 1.0);
                            gl.glVertex2d(0.1, 0.7);

                            gl.glTexCoord2d(1.0, 1.0);
                            gl.glVertex2d(0.6, 0.1);

                            gl.glTexCoord2d(1.0, 0.0);
                            gl.glVertex2d(0.2, -0.7);
                            gl.glEnd();
                        }
                    }
                });
            }
        });
    }

    private void render(final GL2 gl) {
        // start by filling in fbo1
        renderToFBO(gl, fbo1, new Runnable() {
            @Override
            public void run() {
                gl.glClearColor(1f, 0, 0, 1);
                gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
                gl.glLoadIdentity();
            }
        });

        for (int i = 0; i < 5; ++i) {
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
                gl.glClearColor(0, 0, 0, 1);
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
