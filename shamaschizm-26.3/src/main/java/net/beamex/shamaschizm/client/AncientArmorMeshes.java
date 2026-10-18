package net.beamex.shamaschizm.client;

import java.util.EnumSet;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;

/** Texture-preserving chainmail slices and a separate, standard-UV trim mesh. */
final class AncientArmorMeshes {
    static void remapChain(ModelPart part,float dx,float dy) {
        var original=new ModelPart.Cube(0,44,-9,-1,-3,18,14,6,-0.25F,-0.25F,-0.25F,false,64,64,EnumSet.allOf(Direction.class));
        var cube=part.getRandomCube(RandomSource.create(0));
        for(int i=0;i<cube.polygons.length;i++) {
            var face=cube.polygons[i];
            for(var old:original.polygons) {
                if(face.normal().dot(old.normal())<0.99F)continue;
                var a=old.vertices()[0]; var b=old.vertices()[1]; var d=old.vertices()[3];
                float ex=b.x()-a.x(),ey=b.y()-a.y(),ez=b.z()-a.z();
                float fx=d.x()-a.x(),fy=d.y()-a.y(),fz=d.z()-a.z();
                var vertices=face.vertices().clone();
                for(int v=0;v<vertices.length;v++) {
                    var p=vertices[v]; float px=p.x()+dx-a.x(),py=p.y()+dy-a.y(),pz=p.z()-a.z();
                    float t=(px*ex+py*ey+pz*ez)/(ex*ex+ey*ey+ez*ez);
                    float u=(px*fx+py*fy+pz*fz)/(fx*fx+fy*fy+fz*fz);
                    vertices[v]=p.remap(a.u()+t*(b.u()-a.u())+u*(d.u()-a.u()),a.v()+t*(b.v()-a.v())+u*(d.v()-a.v()));
                }
                cube.polygons[i]=new ModelPart.Polygon(vertices,face.normal()); break;
            }
        }
    }
    static HumanoidModel<HumanoidRenderState> trim(EquipmentSlot slot,boolean baby) {
        var mesh=new MeshDefinition(); var root=mesh.getRoot();
        root.addOrReplaceChild("head",CubeListBuilder.create(),PartPose.ZERO)
                .addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.ZERO);
        root.addOrReplaceChild("body",CubeListBuilder.create(),PartPose.ZERO);
        root.addOrReplaceChild("left_arm",CubeListBuilder.create(),PartPose.offset(5,2,0));
        root.addOrReplaceChild("right_arm",CubeListBuilder.create(),PartPose.offset(-5,2,0));
        root.addOrReplaceChild("left_leg",CubeListBuilder.create(),PartPose.offset(1.9F,12,0));
        root.addOrReplaceChild("right_leg",CubeListBuilder.create(),PartPose.offset(-1.9F,12,0));
        switch(slot) {
            case HEAD -> root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0)
                    .addBox(-4,-8,-4,8,8,8,new CubeDeformation(1.005F)),PartPose.ZERO)
                    .addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.ZERO);
            case CHEST -> {
                root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(16,16)
                        .addBox(-4,0,-2,8,12,4,new CubeDeformation(0.995F)),PartPose.ZERO);
                root.addOrReplaceChild("left_arm",CubeListBuilder.create().texOffs(40,16).mirror()
                        .addBox(-1,-2,-2,4,12,4,new CubeDeformation(1.005F)),PartPose.offset(5,2,0));
                root.addOrReplaceChild("right_arm",CubeListBuilder.create().texOffs(40,16)
                        .addBox(-3,-2,-2,4,12,4,new CubeDeformation(1.005F)),PartPose.offset(-5,2,0));
            }
            case LEGS -> {
                root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(16,16)
                        .addBox(-4,0,-2,8,12,4,new CubeDeformation(0.505F)),PartPose.ZERO);
                legs(root,0.505F);
            }
            case FEET -> legs(root,0.755F);
            default -> { }
        }
        var layer=LayerDefinition.create(mesh,64,32);
        return new HumanoidModel<>((baby?layer.apply(HumanoidModel.BABY_TRANSFORMER):layer).bakeRoot());
    }
    private static void legs(PartDefinition root,float inflation) {
        root.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(0,16).mirror()
                .addBox(-2,0,-2,4,12,4,new CubeDeformation(inflation)),PartPose.offset(1.9F,12,0));
        root.addOrReplaceChild("right_leg",CubeListBuilder.create().texOffs(0,16)
                .addBox(-2,0,-2,4,12,4,new CubeDeformation(inflation)),PartPose.offset(-1.9F,12,0));
    }
}
