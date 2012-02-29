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
    private double theta = 0;
    private double s = 0;
    private double c = 0;
    private FBObject fbo;

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface GLEventListener ---------------------

    @Override
    public void init(GLAutoDrawable drawable) {
//        drawable.setGL(new DebugGL2(drawable.getGL().getGL2()));
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        fbo.destroy(gl);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        update();
        render(drawable.getGL().getGL2());
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        if (fbo != null) {
            fbo.destroy(gl);
        }
        fbo = new FBObject(width, height);
        fbo.init(gl);
        fbo.attachTexture2D(gl, 0, GL2.GL_NEAREST, GL2.GL_NEAREST, 0, 0);
        if (!fbo.isStatusValid()) {
            throw new IllegalStateException();
        }
        fbo.unbind(gl);
    }

// -------------------------- OTHER METHODS --------------------------

    private void render(GL2 gl) {
        {
            fbo.bind(gl);
            renderScene(gl);
            fbo.unbind(gl);
        }

        renderFBO(gl);
    }

    private void renderFBO(GL2 gl) {
        gl.glEnable(GL.GL_TEXTURE_2D);
        fbo.use(gl, 0);
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
        fbo.unuse(gl);
        gl.glDisable(GL.GL_TEXTURE_2D);
    }

    private void renderScene(GL2 gl) {
        gl.glClearColor(.1f, .1f, .1f, 1);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();
        {
            gl.glBegin(GL.GL_TRIANGLES);
            gl.glColor3f(1, 0, 0);
            gl.glVertex2d(-c, -c);
//            gl.glVertex2d(0, 0);

            gl.glColor3f(0, 0, 1);
            gl.glVertex2d(0, c);
//            gl.glVertex2d(0, 1);

            gl.glColor3f(0, 1, 0);
            gl.glVertex2d(s, -s);
//            gl.glVertex2d(1, 0);

            gl.glEnd();
        }
    }

    private void update() {
        theta += 0.01;
        s = Math.sin(theta);
        c = Math.cos(theta);
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
