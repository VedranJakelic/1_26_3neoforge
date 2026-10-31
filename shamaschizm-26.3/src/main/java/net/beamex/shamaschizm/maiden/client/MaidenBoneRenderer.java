package net.beamex.shamaschizm.maiden.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.beamex.shamaschizm.maiden.MaidenBone;
import net.beamex.shamaschizm.maiden.MaidenBones;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
public final class MaidenBoneRenderer extends EntityRenderer<MaidenBone,MaidenBoneRenderer.State>{
 public static final class State extends EntityRenderState{int part;Matrix4f transform;}
 public MaidenBoneRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=.12F;}
 @Override public State createRenderState(){return new State();}
 @Override public void extractRenderState(MaidenBone e,State s,float p){super.extractRenderState(e,s,p);s.part=e.part();var a=e.angles();s.transform=new Matrix4f().translation(0,(MaidenBones.PARTS[e.part()][5]-MaidenBones.PARTS[e.part()][2])/32,0).rotateXYZ(a.x(),a.y(),a.z());}
 @Override public void submit(State s,PoseStack p,SubmitNodeCollector c,CameraRenderState camera){super.submit(s,p,c,camera);BoneMesh.submit(s.part,s.transform,true,p,c,s.lightCoords);}
}
