package net.beamex.shamaschizm.ascent.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Filters sleeve faces before atlas UV conversion. ModelPart emits complete
 * quads through the bulk addVertex overload in 26.3. Testing the whole quad
 * preserves torso faces that merely share the u=40/64 edge with an arm.
 * The lower v threshold also supports AncientArmorModel's 64x64 layout.
 */
public final class AscentSleeveFilter implements VertexConsumer {
    private final VertexConsumer delegate;
    private final Vertex[] quad = {new Vertex(), new Vertex(), new Vertex(), new Vertex()};
    private int count;

    public AscentSleeveFilter(VertexConsumer delegate) { this.delegate = delegate; }

    @Override
    public void addVertex(float x, float y, float z, int color, float u, float v,
            int overlay, int light, float nx, float ny, float nz) {
        quad[count++].set(x, y, z, color, u, v, overlay, light, nx, ny, nz);
        if (count != 4) return;
        boolean sleeve = true;
        for (Vertex vertex : quad) {
            sleeve &= vertex.u >= 40.0F / 64.0F && vertex.v >= 16.0F / 64.0F;
        }
        if (!sleeve) {
            for (Vertex vertex : quad) {
                delegate.addVertex(vertex.x, vertex.y, vertex.z, vertex.color,
                        vertex.u, vertex.v, vertex.overlay, vertex.light,
                        vertex.nx, vertex.ny, vertex.nz);
            }
        }
        count = 0;
    }

    private static final class Vertex {
        float x, y, z, u, v, nx, ny, nz;
        int color, overlay, light;
        void set(float x, float y, float z, int color, float u, float v,
                int overlay, int light, float nx, float ny, float nz) {
            this.x=x; this.y=y; this.z=z; this.color=color; this.u=u; this.v=v;
            this.overlay=overlay; this.light=light; this.nx=nx; this.ny=ny; this.nz=nz;
        }
    }

    // Preserve the interface for consumers that do not emit ModelPart quads.
    @Override public VertexConsumer addVertex(float x, float y, float z) { delegate.addVertex(x,y,z); return this; }
    @Override public VertexConsumer setColor(int r,int g,int b,int a) { delegate.setColor(r,g,b,a); return this; }
    @Override public VertexConsumer setColor(int color) { delegate.setColor(color); return this; }
    @Override public VertexConsumer setUv(float u,float v) { delegate.setUv(u,v); return this; }
    @Override public VertexConsumer setUv1(int u,int v) { delegate.setUv1(u,v); return this; }
    @Override public VertexConsumer setUv2(int u,int v) { delegate.setUv2(u,v); return this; }
    @Override public VertexConsumer setUv3(float u,float v) { delegate.setUv3(u,v); return this; }
    @Override public VertexConsumer setNormal(float x,float y,float z) { delegate.setNormal(x,y,z); return this; }
    @Override public VertexConsumer setLineWidth(float width) { delegate.setLineWidth(width); return this; }
}
