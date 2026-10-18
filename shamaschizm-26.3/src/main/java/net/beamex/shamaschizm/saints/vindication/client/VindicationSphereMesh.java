package net.beamex.shamaschizm.saints.vindication.client;
import java.util.*;
/** Cached visual geometry only: same 5/16-block outer radius as the old core. */
public final class VindicationSphereMesh {
 public static final int SLICES=24,STACKS=12;
 public static final float RADIUS=5F/16F;
 public record Vertex(float x,float y,float z,float nx,float ny,float nz,float u,float v,int color){}
 public static final List<Vertex> VERTICES=build();
 private static List<Vertex> build(){
  List<Vertex> vertices=new ArrayList<>(SLICES*STACKS*4);
  for(int row=0;row<STACKS;row++)for(int col=0;col<SLICES;col++){
   vertices.add(vertex(row,col));vertices.add(vertex(row,col+1));
   vertices.add(vertex(row+1,col+1));vertices.add(vertex(row+1,col));
  }
  return List.copyOf(vertices);
 }
 private static Vertex vertex(int row,int col){
  double theta=Math.PI*row/STACKS,phi=2*Math.PI*(col%SLICES)/SLICES;
  float radial=(row==0||row==STACKS)?0:(float)Math.sin(theta);
  float nx=radial*(float)Math.cos(phi),ny=(float)Math.cos(theta),nz=radial*(float)Math.sin(phi);
  // Gentle baked colour variation gives the emissive surface a rounded appearance.
  // It remains independent of world lighting and does not illuminate blocks.
  float brightness=.78F+.22F*Math.max(0,nx*.3F+ny*.65F-nz*.7F);
  int color=0xFF000000|(Math.round(85*brightness)<<16)|(Math.round(221*brightness)<<8)|Math.round(255*brightness);
  return new Vertex(nx*RADIUS,ny*RADIUS,nz*RADIUS,nx,ny,nz,(float)col/SLICES,(float)row/STACKS,color);
 }
 private VindicationSphereMesh(){}
}
