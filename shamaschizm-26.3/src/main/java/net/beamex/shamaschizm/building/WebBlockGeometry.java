package net.beamex.shamaschizm.building;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Pure geometry shared by placement, collision, repair and whole-web removal. */
public final class WebBlockGeometry {
    public static List<BlockPos> cells(WebRectangle r){
        var box=r.bounds();List<BlockPos> result=new ArrayList<>();
        for(BlockPos p:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))
            if(r.intersects(new AABB(p)) && Math.min(p.getY()+1,r.bottom().y+r.height())-Math.max(p.getY(),r.bottom().y)>1E-6)result.add(p.immutable());
        return result;
    }
    /** Vanilla voxel shapes approximate the diagonal plane with subpixel-sized strips. Cached per cell. */
    public static VoxelShape shape(WebRectangle r,BlockPos pos){
        double y0=Math.max(0,r.bottom().y-pos.getY()),y1=Math.min(1,r.bottom().y+r.height()-pos.getY());
        if(y1<=y0)return Shapes.empty();
        VoxelShape shape=Shapes.empty();int steps=Math.max(1,(int)Math.ceil(r.width()*32));
        for(int i=0;i<steps;i++){
            double a=-r.width()/2+r.width()*i/steps,b=-r.width()/2+r.width()*(i+1)/steps;
            double x0=r.bottom().x+r.ux()*a-pos.getX(),x1=r.bottom().x+r.ux()*b-pos.getX();
            double z0=r.bottom().z+r.uz()*a-pos.getZ(),z1=r.bottom().z+r.uz()*b-pos.getZ();
            double loX=Math.max(0,Math.min(x0,x1)-.015625),hiX=Math.min(1,Math.max(x0,x1)+.015625);
            double loZ=Math.max(0,Math.min(z0,z1)-.015625),hiZ=Math.min(1,Math.max(z0,z1)+.015625);
            if(hiX>loX && hiZ>loZ)shape=Shapes.or(shape,Shapes.box(loX,y0,loZ,hiX,y1,hiZ));
        }
        return shape.optimize();
    }
    private WebBlockGeometry(){}
}
