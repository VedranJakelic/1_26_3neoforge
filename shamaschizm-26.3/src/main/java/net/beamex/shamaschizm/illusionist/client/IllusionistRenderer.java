package net.beamex.shamaschizm.illusionist.client;
import java.io.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.illusionist.Illusionist;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Smooth normals and vertex colors; geometry is loaded once per renderer/resource reload. */
public final class IllusionistRenderer extends EntityRenderer<Illusionist,IllusionistRenderer.State> {
 private static final Identifier TEXTURE=Identifier.withDefaultNamespace("textures/block/white_concrete.png");
 private final float[][] vertices;
 private final int[] colors;
 private final Part[] parts;
 private record Part(int start,int count,int kind,int side,float x,float y,float z){}
 public IllusionistRenderer(EntityRendererProvider.Context context){
  super(context);shadowRadius=.45F;
  try(var in=new DataInputStream(Minecraft.getInstance().getResourceManager().open(Shamaschizm.id("illusionist/model.bin")))){
   int count=in.readInt();if(count<=0||count>200000||count%4!=0)throw new IOException("Invalid illusionist mesh size");
   vertices=new float[count][6];colors=new int[count];
   for(int i=0;i<count;i++){
    for(int j=0;j<6;j++)vertices[i][j]=in.readFloat();
    int r=color(in.readFloat()),g=color(in.readFloat()),b=color(in.readFloat());colors[i]=0xFF000000|(r<<16)|(g<<8)|b;
   }
  }catch(IOException ex){throw new IllegalStateException("Cannot load Illusionist model",ex);}
  try(var in=new DataInputStream(Minecraft.getInstance().getResourceManager().open(Shamaschizm.id("illusionist/rig.bin")))){
   int count=in.readInt();if(count<1||count>64)throw new IOException("Invalid rig size");parts=new Part[count];int covered=0;
   for(int i=0;i<count;i++){
    Part p=new Part(in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readFloat(),in.readFloat(),in.readFloat());
    if(p.start!=covered||p.count<0||p.count%4!=0||p.start+p.count>vertices.length)throw new IOException("Invalid rig range");
    parts[i]=p;covered+=p.count;
   }
   if(covered!=vertices.length)throw new IOException("Incomplete rig");
  }catch(IOException ex){throw new IllegalStateException("Cannot load Illusionist rig",ex);}
 }
 private static int color(float value){return Math.clamp(Math.round(value*255),0,255);}
 public static final class State extends EntityRenderState {float yaw,time,headYaw,headPitch,firstSpin,talk,gestureTime;}
 @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(Illusionist e,float partial){return e.getBoundingBox().inflate(.9);}
 @Override public State createRenderState(){return new State();}
 @Override public void extractRenderState(Illusionist e,State s,float partial){super.extractRenderState(e,s,partial);s.yaw=e.getYRot();s.headYaw=e.headYaw(partial);s.firstSpin=e.firstSpin(partial)+e.choiceRoll(partial);s.headPitch=e.headPitch(partial);s.talk=e.talkBlend(partial);s.gestureTime=e.talkClock(partial);s.time=(e.tickCount+partial)/20F+(e.getId()%997)*.618F;}
 @Override public void submit(State s,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
  super.submit(s,pose,collector,camera);pose.pushPose();
  // Mesh faces -Z; Minecraft yaw zero faces +Z.
  pose.rotate(Axis.YP.rotationDegrees(180-s.yaw));
  // Fractional tick time gives smooth, frame-rate-independent idle movement.
  final float t=s.time;
  pose.translate(0,.012*Math.sin(t*.9),0);
  int fingerIndex=0;
  for(Part part:parts){
   if(part.kind==2)fingerIndex=0;
   final int finger=part.kind==3?fingerIndex++:0;
   final float curl=s.talk*IllusionistGesture.curl(s.gestureTime,part.side,finger);
   pose.pushPose();
   if(part.kind==1){
    pose.translate(part.x,part.y,part.z);
    pose.rotate(Axis.YP.rotationDegrees((float)(s.headYaw+1.2*Math.sin(t*.65))));
    pose.rotate(Axis.XP.rotationDegrees(s.headPitch));
    pose.rotate(Axis.ZP.rotationDegrees((float)(s.firstSpin+1.3*Math.sin(t*.8))));
    pose.translate(-part.x,-part.y,-part.z);
   }else if(part.kind>=2){
    Part palm=null;for(Part p:parts)if(p.kind==2&&p.side==part.side){palm=p;break;}
    if(palm!=null){
     float phase=part.side*1.4F;
     float gt=s.gestureTime;
     pose.translate(part.side*s.talk*IllusionistGesture.sample(gt,part.side,0),s.talk*IllusionistGesture.sample(gt,part.side,1),s.talk*IllusionistGesture.sample(gt,part.side,2));
     pose.translate(.012*Math.sin(t*.8+phase),.025*Math.sin(t*1.05+phase),.012*Math.cos(t*.7+phase));
     pose.translate(palm.x,palm.y,palm.z);
     pose.rotate(Axis.ZP.rotationDegrees((float)(3*Math.sin(t*.85+phase)+part.side*s.talk*IllusionistGesture.sample(gt,part.side,5))));
     pose.rotate(Axis.YP.rotationDegrees((float)(3*Math.sin(t*.6+phase)+part.side*s.talk*IllusionistGesture.sample(gt,part.side,4))));
     pose.rotate(Axis.XP.rotationDegrees(s.talk*IllusionistGesture.sample(gt,part.side,3)));
     pose.translate(-palm.x,-palm.y,-palm.z);
     if(part.kind==3){
      pose.translate(part.x,part.y,part.z);
      pose.rotate(Axis.XP.rotationDegrees((float)(4*Math.sin(t*1.15+phase+part.x*8)-55*curl)));
      pose.rotate(Axis.ZP.rotationDegrees(IllusionistGesture.fingerSpread(part.side,finger)*s.talk));
      pose.translate(-part.x,-part.y,-part.z);
     }
    }
   }
   collector.submitCustomGeometry(pose,RenderTypes.entityCutout(TEXTURE),(matrix,buffer)->{
    for(int i=part.start;i<part.start+part.count;i++){
     float[] v=vertices[i];float x=v[0],y=v[1],z=v[2],nx=v[3],ny=v[4],nz=v[5];
     if(part.kind==3&&s.talk>0){
      // Softly weighted middle/distal joints add articulation beyond knuckle rotation.
      // Curl axis follows each finger's sideways spread (including the inward thumb).
      float dx=finger==4?-part.side*.10F:part.side*(finger-1.2F)*.035F;
      float dy=finger==4?.10F:.22F;
      float len=(float)Math.sqrt(dx*dx+dy*dy);dx/=len;dy/=len;
      float along=(x-part.x)*dx+(y-part.y)*dy;
      float w=Math.clamp((along-.045F)/.19F,0,1);
      w=w*w*(3-2*w);
      float angle=(float)Math.toRadians(-72*curl*w);
      float c=(float)Math.cos(angle),sn=(float)Math.sin(angle);
      // Axis in XY perpendicular to finger, Rodrigues rotation around middle joint.
      float ax=dy,ay=-dx,px=x-part.x-dx*.055F,py=y-part.y-dy*.055F,pz=z-part.z;
      float dot=ax*px+ay*py;
      x=part.x+dx*.055F+px*c+ay*pz*sn+ax*dot*(1-c);
      y=part.y+dy*.055F+py*c-ax*pz*sn+ay*dot*(1-c);
      z=part.z+pz*c+(ax*py-ay*px)*sn;
      dot=ax*nx+ay*ny;float ox=nx,oy=ny;
      nx=nx*c+ay*nz*sn+ax*dot*(1-c);ny=ny*c-ax*nz*sn+ay*dot*(1-c);nz=nz*c+(ax*oy-ay*ox)*sn;
     }
     if(part.kind==0){
      float weight=Math.clamp(1-v[1]/1.7F,0,1),angle=t*.8F+v[1]*1.7F;
      x+=.018F*weight*weight*(float)Math.sin(angle);
      // Inverse-transpose of the cloak shear keeps lighting attached to the cloth.
      float slope=v[1]<1.7F?.018F*(-2*weight/1.7F*(float)Math.sin(angle)+weight*weight*1.7F*(float)Math.cos(angle)):0;
      ny-=slope*nx;float len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);nx/=len;ny/=len;nz/=len;
     }
     buffer.addVertex(matrix,x,y,z).setColor(colors[i]).setUv(.5F,.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(s.lightCoords).setNormal(matrix,nx,ny,nz);
    }
   });pose.popPose();
  }
  pose.popPose();
 }
}
