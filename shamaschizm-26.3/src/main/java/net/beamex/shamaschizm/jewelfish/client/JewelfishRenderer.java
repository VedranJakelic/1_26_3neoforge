package net.beamex.shamaschizm.jewelfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.jewelfish.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;

@EventBusSubscriber(modid=Shamaschizm.MOD_ID,value=Dist.CLIENT)
public final class JewelfishRenderer extends EntityRenderer<Jewelfish,JewelfishRenderer.State> {
    public static final class State extends EntityRenderState {float yaw,pitch;boolean water;int overlay;}
    public JewelfishRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.25F;}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(JewelfishRegistration.TYPE,JewelfishRenderer::new);}
    @Override public State createRenderState(){return new State();}
    @Override protected AABB getBoundingBoxForCulling(Jewelfish e,float partial){return e.getBoundingBox().inflate(1.6);}
    @Override public void extractRenderState(Jewelfish e,State s,float p){
        super.extractRenderState(e,s,p);s.yaw=Mth.rotLerp(p,e.yRotO,e.getYRot());s.water=e.isInWater();
        var velocity=e.getDeltaMovement();s.pitch=(float)Math.clamp(-Math.atan2(velocity.y,Math.max(.04,velocity.horizontalDistance())),-.5,.5);
        s.overlay=OverlayTexture.pack(0F,e.hurtTime>0||e.deathTime>0);
    }
    @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        super.submit(s,pose,collector,camera);
        // Negative-Z model head becomes the entity's positive-Z forward at yaw zero.
        // Match vanilla salmon cadence: rigid body sway plus a hinged rear half.
        float amplitude=s.water?1F:1.3F, speed=s.water?1F:1.7F;
        float swing=(float)Math.sin(s.ageInTicks*.6F*speed);
        Matrix4f front=new Matrix4f().rotateY((float)Math.toRadians(180-s.yaw+4.3F*amplitude*swing)).rotateX(s.pitch);
        if(!s.water)front.translate(.2F,.1F,0).rotateZ(1.5707963F);
        // The supplied body2/body3 joint is at model Z=1, or -1/16 after centering.
        Matrix4f rear=new Matrix4f(front).translate(0,0,-.0625F)
                .rotateY(-.25F*amplitude*swing).translate(0,0,.0625F);
        Matrix3f frontNormals=front.normal(new Matrix3f()),rearNormals=rear.normal(new Matrix3f());
        collector.submitCustomGeometry(pose,RenderTypes.entityCutout(Shamaschizm.id("textures/entity/jewelfish.png")),(matrix,vertices)->{
            for(int part=0;part<JewelfishMesh.PARTS.length;part++){
                // Tail (7) and body3/body4 with their fins (8 onward) move as one rigid piece.
                Matrix4f transform=part>=7?rear:front;
                Matrix3f normals=part>=7?rearNormals:frontNormals;
                for(var v:JewelfishMesh.PARTS[part]){
                var at=transform.transformPosition(new Vector3f(v[0],v[1],v[2]));
                var normal=normals.transform(new Vector3f(v[5],v[6],v[7])).normalize();
                vertices.addVertex(matrix,at.x,at.y,at.z).setColor(-1).setUv(v[3],v[4]).setOverlay(s.overlay).setLight(s.lightCoords).setNormal(matrix,normal.x,normal.y,normal.z);
                }
            }
        });
    }
}
