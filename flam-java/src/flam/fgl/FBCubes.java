/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 */

package flam.fgl;

import com.jogamp.opengl.util.FBObject;
import com.jogamp.opengl.util.FPSAnimator;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


class FBCubes implements GLEventListener, MouseListener, MouseMotionListener {
    private static final int FBO_SIZE = 1024;

    public FBCubes() {
        cubeInner = new CubeObject(false);
        cubeMiddle = new CubeObject(true);
        cubeOuter = new CubeObject(true);
        fbo1 = new FBObject(FBO_SIZE, FBO_SIZE);
        fbo2 = new FBObject(FBO_SIZE, FBO_SIZE);
    }

    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        // drawable.setGL(new DebugGL2(gl));
        // gl = drawable.getGL().getGL2();
        fbo1.init(gl);
        fbo1.attachTexture2D(gl, 0, gl.GL_NEAREST, gl.GL_NEAREST, 0, 0);
        fbo1.unbind(gl);
        fbo2.init(gl);
        fbo2.attachTexture2D(gl, 0, gl.GL_NEAREST, gl.GL_NEAREST, 0, 0);
        fbo2.unbind(gl);
    }

    int x, y, width, height;
    float motionIncr;
    float xRot, yRot;

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        cubeOuter.reshape(drawable.getGL().getGL2(), x, y, width, height);
        motionIncr = 180.f / Math.max(width, height);
    }

    public void dispose(GLAutoDrawable drawable) {
        System.out.println("FBCubes.dispose: " + drawable);
        GL2 gl = drawable.getGL().getGL2();
        fbo1.destroy(gl);
        fbo2.destroy(gl);
        fbo1 = null;
        fbo2 = null;
        cubeInner.dispose(gl);
        cubeInner = null;
        cubeMiddle.dispose(gl);
        cubeMiddle = null;
        cubeOuter.dispose(gl);
        cubeOuter = null;
    }

    public void display(GLAutoDrawable drawable) {
        //        System.out.println("display");
        GL2 gl = drawable.getGL().getGL2();

        fbo1.bind(gl);
        cubeInner.reshape(gl, 0, 0, FBO_SIZE, FBO_SIZE);
        cubeInner.display(gl, xRot, yRot);
        fbo1.unbind(gl);

        FBObject tex = fbo1;
        FBObject rend = fbo2;

        int MAX_ITER = 3;

        for (int i = 0; i < MAX_ITER; i++) {
            rend.bind(gl);
            gl.glEnable(GL.GL_TEXTURE_2D);
            tex.use(gl, 0);
            cubeMiddle.reshape(gl, 0, 0, FBO_SIZE, FBO_SIZE);
            cubeMiddle.display(gl, xRot, yRot);
            tex.unuse(gl);
            gl.glDisable(GL.GL_TEXTURE_2D);
            rend.unbind(gl);
            FBObject tmp = tex;
            tex = rend;
            rend = tmp;
        }

        //        System.out.println("display .. p6");
        cubeOuter.reshape(gl, x, y, width, height);
        //        System.out.println("display .. p7");

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glClearColor(0, 0, 0, 1);

        gl.glEnable(GL.GL_TEXTURE_2D);
        tex.use(gl, 0);
        cubeOuter.display(gl, xRot, yRot);
        //        System.out.println("display .. p8");
        tex.unuse(gl);
        gl.glDisable(GL.GL_TEXTURE_2D);
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }

    private boolean dragging;
    private int lastDragX;
    private int lastDragY;

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
        dragging = false;
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        if (!dragging) {
            dragging = true;
            lastDragX = e.getX();
            lastDragY = e.getY();
        } else {
            yRot += (e.getX() - lastDragX) * motionIncr;
            xRot += (e.getY() - lastDragY) * motionIncr;
            lastDragX = e.getX();
            lastDragY = e.getY();
        }
    }

    public void mouseMoved(MouseEvent e) {
    }

    CubeObject cubeInner;
    CubeObject cubeMiddle;
    CubeObject cubeOuter;
    FBObject fbo1;
    FBObject fbo2;

    public static void main(String[] args) {
        // set argument 'NotFirstUIActionOnProcess' in the JNLP's application-desc tag for example
        // <application-desc main-class="demos.j2d.TextCube"/>
        //   <argument>NotFirstUIActionOnProcess</argument>
        // </application-desc>
        boolean firstUIActionOnProcess = 0 == args.length || !args[0].equals("NotFirstUIActionOnProcess");
        GLProfile.initSingleton(firstUIActionOnProcess);

        GLCapabilities caps = new GLCapabilities(null);
        GLCanvas canvas = new GLCanvas(caps);

        FBCubes cubes = new FBCubes();
        canvas.addMouseListener(cubes);
        canvas.addMouseMotionListener(cubes);
        canvas.addGLEventListener(cubes);

        Frame frame = new Frame("FBCubes Demo ES 1.1");
        frame.add(canvas);
        frame.setSize(800, 480);

        final GLAnimatorControl animator = new FPSAnimator(canvas, 60);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                // Run this on another thread than the AWT event queue to
                // make sure the call to Animator.stop() completes before
                // exiting
                new Thread(new Runnable() {
                    public void run() {
                        animator.stop();
                        System.exit(0);
                    }
                }).start();
            }
        });

        frame.setVisible(true);
        animator.start();
    }
}

