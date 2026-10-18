package net.beamex.shamaschizm.saints.vindication.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
public final class VindicationSphereRenderer {
 public static void submit(PoseStack pose,SubmitNodeCollector collector){
  collector.submitCustomGeometry(pose,RenderTypes.entityTranslucentEmissive(
   Shamaschizm.id("textures/entity/vindication_core.png")),VindicationSphereRenderer::render);
 }
 private static void render(PoseStack.Pose pose,VertexConsumer buffer){
  for(var v:VindicationSphereMesh.VERTICES)
   buffer.addVertex(pose,v.x(),v.y(),v.z()).setColor(v.color()).setUv(v.u(),v.v())
    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(pose,v.nx(),v.ny(),v.nz());
 }
 private VindicationSphereRenderer(){}
}
