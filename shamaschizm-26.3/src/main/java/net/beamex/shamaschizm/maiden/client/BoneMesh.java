package net.beamex.shamaschizm.maiden.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.beamex.shamaschizm.maiden.MaidenBones;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.*;
final class BoneMesh {
 private static final Identifier TEXTURE=Identifier.withDefaultNamespace("textures/entity/skeleton/skeleton.png");
 static void submit(int part,Matrix4f transform,boolean centered,PoseStack pose,SubmitNodeCollector collector,int light){
  float[] b=MaidenBones.PARTS[part];float x=b[0]/16,y=b[1]/16,z=b[2]/16,X=b[3]/16,Y=b[4]/16,Z=b[5]/16;
  if(centered){float cx=(x+X)/2,cy=(y+Y)/2,cz=(z+Z)/2;x-=cx;X-=cx;y-=cy;Y-=cy;z-=cz;Z-=cz;}
  float[][][] faces={{{X,y,z},{x,y,z},{x,Y,z},{X,Y,z}},{{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}},{{X,y,Z},{X,y,z},{X,Y,z},{X,Y,Z}},{{x,y,z},{x,y,Z},{x,Y,Z},{x,Y,z}},{{x,Y,Z},{X,Y,Z},{X,Y,z},{x,Y,z}},{{x,y,z},{X,y,z},{X,y,Z},{x,y,Z}}};
  float[][] norms={{0,0,-1},{0,0,1},{1,0,0},{-1,0,0},{0,1,0},{0,-1,0}};float u=b[6],v=b[7],w=b[3]-b[0],h=b[4]-b[1],d=b[5]-b[2];
  float[][] rects={{u+d,v+d,u+d+w,v+d+h},{u+2*d+w,v+d,u+2*d+2*w,v+d+h},{u,v+d,u+d,v+d+h},{u+d+w,v+d,u+2*d+w,v+d+h},{u+d+w,v+d,u+d,v},{u+d+2*w,v,u+d+w,v+d}};
  collector.submitCustomGeometry(pose,RenderTypes.entityCutout(TEXTURE),(matrix,vertices)->{
   for(int f=0;f<6;f++){var n=transform.transformDirection(new Vector3f(norms[f])).normalize();var uv=rects[f];float[][] coords={{uv[0],uv[3]},{uv[2],uv[3]},{uv[2],uv[1]},{uv[0],uv[1]}};
    for(int i=0;i<4;i++){var at=transform.transformPosition(new Vector3f(faces[f][i]));vertices.addVertex(matrix,at.x,at.y,at.z).setColor(-1).setUv(coords[i][0]/64,coords[i][1]/32).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix,n.x,n.y,n.z);}
   }
  });
 }
}
