package flam;

import flam.renderers.ria.RIARenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mike
 */
public class FlamComponent extends JComponent {
    private GenomeProvider provider;
    private double fps = 10;
    public long iterTime = 0;
    public long renderTime = 0;
    public int batches = 0;
    private Renderer renderer = new RIARenderer();

    public FlamComponent(final GenomeProvider provider) {
        this.provider = provider;
        setFocusable(true);

        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                reset();
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {
            }
        });
        addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                reset();
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
    }

    private void reset() {
        this.provider.reset();
        batches = 0;
        renderTime = 0;
        iterTime = 0;
    }

    public void setFps(double fps) {
        this.fps = fps;
    }

    private static class State {
        private final RenderState state;
        private final RenderBuffer buffer;

        public State(RenderState state, RenderBuffer buffer) {
            this.state = state;
            this.buffer = buffer;
        }
    }

    private State state;
    private static Map<Genome, State> states = new HashMap<>();

    private void resetState(Genome genome) {
        State state = states.get(genome);
        if (state != null) {
            if (state.buffer.getWidth() == getWidth() && state.buffer.getHeight() == getHeight()) {
                this.state = state;
                return;
            } else {
                states.remove(genome);
            }
        }

        RenderBuffer buffer = renderer.newBuffer(getWidth(), getHeight());
        this.state = new State(renderer.newState(genome, buffer), buffer);
    }


    @Override
    protected void paintComponent(Graphics graphics) {
        updateGenome();

        long start = System.currentTimeMillis();
        double allottedIterTime = 1000.0 / fps;
        if (batches > 0) {
            allottedIterTime -= renderTime / batches;
        }
        renderer.iterate(state.state, state.buffer,  allottedIterTime);
        long batchFinish = System.currentTimeMillis();
        iterTime += (batchFinish - start);
        renderer.render(state.state, state.buffer);
        long bltFinish = System.currentTimeMillis();
        renderTime += (bltFinish - batchFinish);
        batches++;
        graphics.drawImage(state.buffer.getImage(), 0, 0, null);
        repaint();
    }

    private void updateGenome() {
        Genome newGenome = provider.getGenome();
        if (state != null
                && state.buffer.getWidth() == getWidth()
                && state.buffer.getHeight() == getHeight()
                && state.state.getGenome() == newGenome) {
            return;
        }
        resetState(newGenome);
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }
}