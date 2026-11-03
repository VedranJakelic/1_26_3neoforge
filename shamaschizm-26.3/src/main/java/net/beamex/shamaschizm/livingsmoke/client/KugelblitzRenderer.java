package net.beamex.shamaschizm.livingsmoke.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.livingsmoke.Kugelblitz;
import net.beamex.shamaschizm.livingsmoke.LivingSmokeRegistration;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Vector3f;

/** Opaque black core with a subtle purple edge. Sparks and dark trails are particles. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class KugelblitzRenderer extends EntityRenderer<Kugelblitz, EntityRenderState> {
    public KugelblitzRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius=0; }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(LivingSmokeRegistration.KUGELBLITZ, KugelblitzRenderer::new);
    }
    @Override public EntityRenderState createRenderState() { return new EntityRenderState(); }
    @Override public void submit(EntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state,pose,collector,camera);
        collector.submitCustomGeometry(pose,
                RenderTypes.entityCutout(Shamaschizm.id("textures/entity/kugelblitz.png")), (matrix, out) -> {
            final int slices=16, stacks=10;
            float radius=.31F + .018F * (float)Math.sin(state.ageInTicks * .4F);
            for (int j=0;j<stacks;j++) for(int i=0;i<slices;i++) {
                float y0=(float)(-Math.PI/2+Math.PI*j/stacks),y1=(float)(-Math.PI/2+Math.PI*(j+1)/stacks);
                float a0=(float)(Math.PI*2*i/slices),a1=(float)(Math.PI*2*(i+1)/slices);
                for (float[] v : new float[][] {{y0,a0},{y0,a1},{y1,a1},{y1,a0}}) {
                    float x=(float)(Math.cos(v[0])*Math.sin(v[1]));
                    float y=(float)Math.sin(v[0]);
                    float z=(float)(Math.cos(v[0])*Math.cos(v[1]));
                    out.addVertex(matrix,x*radius,y*radius,z*radius)
                            .setColor(-1).setUv(0,0).setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(0xF000F0).setNormal(matrix,x,y,z);
                }
            }
        });
    }
}
