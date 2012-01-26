package flam;

/**
 */
public interface Renderer {
    RenderState newState(Genome genome, RenderBuffer buffer);
    RenderBuffer newBuffer(int width, int height);

    void iterate(RenderState state, RenderBuffer buffer, double allottedTimeMs);
    void render(RenderState state, RenderBuffer buffer);
}
