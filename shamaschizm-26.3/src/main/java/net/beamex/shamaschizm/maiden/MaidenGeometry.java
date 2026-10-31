package net.beamex.shamaschizm.maiden;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import com.google.gson.JsonParser;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import net.minecraft.world.phys.Vec3;
/** Exact animated model surfaces; transparent shell pixels do not block an attack. */
public final class MaidenGeometry {
 public record Quad(int part,float[][] vertices,Vector3f normal){}
 public static final Quad[] QUADS;
 private static final String[] OPAQUE=new String[128];
 static {
  try(var in=MaidenGeometry.class.getResourceAsStream("/assets/shamaschizm/iron_maiden_geometry.json")){
   if(in==null)throw new IllegalStateException("Missing Iron Maiden geometry");
   var json=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();var list=new ArrayList<Quad>();
   for(var value:json.getAsJsonArray("quads")){var q=value.getAsJsonObject();float[][] v=new float[4][5];for(int i=0;i<4;i++)for(int j=0;j<5;j++)v[i][j]=q.getAsJsonArray("v").get(i).getAsJsonArray().get(j).getAsFloat();var n=q.getAsJsonArray("n");list.add(new Quad(q.get("part").getAsInt(),v,new Vector3f(n.get(0).getAsFloat(),n.get(1).getAsFloat(),n.get(2).getAsFloat())));}
   QUADS=list.toArray(Quad[]::new);for(int i=0;i<128;i++)OPAQUE[i]=json.getAsJsonArray("opaque").get(i).getAsString();
  }catch(Exception e){throw new ExceptionInInitializerError(e);}
 }
 public static int hit(Vec3 from,Vec3 to,Matrix4f[] pose){
  Vector3f start=new Vector3f((float)from.x,(float)from.y,(float)from.z),direction=new Vector3f((float)(to.x-from.x),(float)(to.y-from.y),(float)(to.z-from.z));float nearest=Float.POSITIVE_INFINITY;int part=-1;
  for(var q:QUADS){Vector3f[] v=new Vector3f[4];for(int i=0;i<4;i++)v[i]=pose[q.part].transformPosition(new Vector3f(q.vertices[i][0],q.vertices[i][1],q.vertices[i][2]));
   for(int tri=0;tri<2;tri++){int a=0,b=tri+1,c=tri+2;Vector3f e1=new Vector3f(v[b]).sub(v[a]),e2=new Vector3f(v[c]).sub(v[a]),h=new Vector3f(direction).cross(e2);float det=e1.dot(h);if(Math.abs(det)<1e-8)continue;Vector3f s=new Vector3f(start).sub(v[a]);float u=s.dot(h)/det;if(u<0||u>1)continue;Vector3f cross=new Vector3f(s).cross(e1);float w=direction.dot(cross)/det;if(w<0||u+w>1)continue;float t=e2.dot(cross)/det;if(t<0||t>1||t>=nearest)continue;
    float texU=q.vertices[a][3]*(1-u-w)+q.vertices[b][3]*u+q.vertices[c][3]*w,texV=q.vertices[a][4]*(1-u-w)+q.vertices[b][4]*u+q.vertices[c][4]*w;
    if(OPAQUE[Math.clamp((int)(texV*128),0,127)].charAt(Math.clamp((int)(texU*128),0,127))!='1')continue;nearest=t;part=q.part;
   }
  }return part;
 }
}
