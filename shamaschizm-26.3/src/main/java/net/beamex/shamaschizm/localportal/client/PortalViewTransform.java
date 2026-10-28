package net.beamex.shamaschizm.localportal.client;

import net.beamex.shamaschizm.localportal.Doorway;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class PortalViewTransform {
    private PortalViewTransform(){}
    public static Matrix4f surface(Matrix4fc projectionView,Vec3 camera,Doorway source){
        return new Matrix4f(projectionView).translate((float)(source.origin().x-camera.x),
                (float)(source.origin().y-camera.y),(float)(source.origin().z-camera.z));
    }
    public static Matrix4f scene(Matrix4fc surfaceMatrix,Doorway source,Doorway destination){
        Vec3 x=destination.transformVector(source,new Vec3(1,0,0));
        Vec3 y=destination.transformVector(source,new Vec3(0,1,0));
        Vec3 z=destination.transformVector(source,new Vec3(0,0,1));
        Matrix4f rotation=new Matrix4f().m00((float)x.x).m01((float)x.y).m02((float)x.z)
                .m10((float)y.x).m11((float)y.y).m12((float)y.z).m20((float)z.x).m21((float)z.y).m22((float)z.z);
        return new Matrix4f(surfaceMatrix).mul(rotation);
    }
}
