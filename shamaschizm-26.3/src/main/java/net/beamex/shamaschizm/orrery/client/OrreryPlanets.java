package net.beamex.shamaschizm.orrery.client;
import java.util.*;
import net.beamex.shamaschizm.orrery.OrreryMotion;

/** Small textured cubes and stepped pixel-grid mechanical tracks, matching the original model's construction. */
final class OrreryPlanets {
    static final List<float[]> TRACKS=new ArrayList<>();
    static final List<List<float[]>> BODIES=new ArrayList<>();
    static {
        for(double r:OrreryMotion.RADII)ring(TRACKS,r,.03125,9.0/16,0x3E4453);
        // Rasterize in the ellipse's own plane before tilting it. Its outline remains stepped.
        pixelRing(TRACKS,9.5/16,30.0/16,30.0/16,.03125,12.0/16,-1,
                Math.toRadians(-22.5),0x3E4453);
        double[] sizes={.10,.15,.1625,.12,.25,.22,.18,.18,.09};
        int[] colors={0x99938B,0xD5B37B,0xFFFFFF,0xB96749,0xD6B496,0xC9B17A,0x83BDBD,0x5269A5,0xB8A89E};
        for(int i=0;i<9;i++){
            List<float[]> mesh=new ArrayList<>();double h=sizes[i]/2;
            if(i==4||i==5){
                for(int band=0;band<5;band++)cube(mesh,-h,-h+sizes[i]*band/5,-h,h,-h+sizes[i]*(band+1)/5,h,
                        band%2==0?colors[i]:(i==4?0x9D765B:0xA38D61));
            }else cube(mesh,-h,-h,-h,h,h,h,colors[i]);
            if(i==5)ring(mesh,.20,.046875,0,0xBAA475);
            BODIES.add(List.copyOf(mesh));
        }
    }
    private static void ring(List<float[]> dst,double r,double width,double y,int color){
        pixelRing(dst,r,r,0,width,y,0,0,color);
    }
    /** Stepped outlines: half-model-pixel grid for slimmer tracks, full pixels for Saturn. */
    private static void pixelRing(List<float[]> dst,double rx,double rz,double centerZ,double width,
                                  double y,double zOffset,double tilt,int color){
        double pixel=width<=.03125?1.0/32:1.0/16;
        int minX=(int)Math.floor((-rx-width)/pixel),maxX=(int)Math.ceil((rx+width)/pixel);
        int minZ=(int)Math.floor((centerZ-rz-width)/pixel),maxZ=(int)Math.ceil((centerZ+rz+width)/pixel);
        for(int iz=minZ;iz<maxZ;iz++){
            int run=Integer.MIN_VALUE;
            for(int ix=minX;ix<=maxX;ix++){
                double x=(ix+.5)*pixel,z=(iz+.5)*pixel-centerZ;
                double rho=Math.sqrt(x*x/(rx*rx)+z*z/(rz*rz));
                double gradient=rho<1e-10?0:Math.sqrt(x*x/(rx*rx*rx*rx)+z*z/(rz*rz*rz*rz))/rho;
                boolean filled=ix<maxX&&gradient>0&&Math.abs(rho-1)/gradient<=width;
                if(filled&&run==Integer.MIN_VALUE)run=ix;
                if(!filled&&run!=Integer.MIN_VALUE){
                    double x0=run*pixel,x1=ix*pixel,z0=iz*pixel,z1=(iz+1)*pixel;
                    double sin=Math.sin(tilt),cos=Math.cos(tilt);
                    quad(dst,new double[][]{{x0,y-sin*z0,zOffset+cos*z0},{x0,y-sin*z1,zOffset+cos*z1},
                            {x1,y-sin*z1,zOffset+cos*z1},{x1,y-sin*z0,zOffset+cos*z0}},color,true);
                    run=Integer.MIN_VALUE;
                }
            }
        }
    }
    private static void cube(List<float[]> d,double x,double y,double z,double X,double Y,double Z,int c){
        quad(d,new double[][]{{X,Y,z},{X,y,z},{x,y,z},{x,Y,z}},c,false);
        quad(d,new double[][]{{x,Y,Z},{x,y,Z},{X,y,Z},{X,Y,Z}},c,false);
        quad(d,new double[][]{{X,Y,Z},{X,y,Z},{X,y,z},{X,Y,z}},c,false);
        quad(d,new double[][]{{x,Y,z},{x,y,z},{x,y,Z},{x,Y,Z}},c,false);
        quad(d,new double[][]{{x,Y,z},{x,Y,Z},{X,Y,Z},{X,Y,z}},c,false);
        quad(d,new double[][]{{x,y,Z},{x,y,z},{X,y,z},{X,y,Z}},c,false);
    }
    private static void quad(List<float[]> d,double[][] p,int c,boolean twoSides){
        double ax=p[1][0]-p[0][0],ay=p[1][1]-p[0][1],az=p[1][2]-p[0][2],bx=p[2][0]-p[0][0],by=p[2][1]-p[0][1],bz=p[2][2]-p[0][2];
        double nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx,len=Math.sqrt(nx*nx+ny*ny+nz*nz);
        if(len<1e-10)return;
        for(int side=0;side<(twoSides?2:1);side++)for(int j=0;j<4;j++){
            int k=side==0?j:3-j;double sign=side==0?1:-1;
            d.add(new float[]{(float)p[k][0],(float)p[k][1],(float)p[k][2],k<2?0:1,(k==1||k==2)?1:0,(float)(nx/len*sign),(float)(ny/len*sign),(float)(nz/len*sign),c});
        }
    }
}
