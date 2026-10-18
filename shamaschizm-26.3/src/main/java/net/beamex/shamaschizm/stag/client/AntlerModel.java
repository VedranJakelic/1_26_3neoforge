package net.beamex.shamaschizm.stag.client;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;

public final class AntlerModel {
 public static final ModelLayerLocation LAYER=new ModelLayerLocation(Shamaschizm.id("helmet_antlers"),"main");
 public static LayerDefinition layer(){MeshDefinition mesh=new MeshDefinition();PartDefinition root=mesh.getRoot();
PartDefinition root_1=root.addOrReplaceChild("root_1",CubeListBuilder.create(),PartPose.offsetAndRotation(0.000000F,20.500000F,5.000000F,-0.000000F,-0.000000F,0.000000F));
PartDefinition root_2=root_1.addOrReplaceChild("root_2",CubeListBuilder.create(),PartPose.offsetAndRotation(0.000000F,0.000000F,0.000000F,-0.000000F,-0.000000F,0.000000F));
root_2.addOrReplaceChild("root_3",CubeListBuilder.create().texOffs(52,0).addBox(0.500000F,-37.000000F,-9.000000F,9.000000F,14.000000F,8.000000F),PartPose.ZERO);
PartDefinition root_4=root_1.addOrReplaceChild("root_4",CubeListBuilder.create(),PartPose.offsetAndRotation(0.000000F,0.000000F,0.000000F,-0.000000F,-0.000000F,0.000000F));
root_4.addOrReplaceChild("root_5",CubeListBuilder.create().texOffs(46,50).addBox(-9.500000F,-37.000000F,-9.000000F,9.000000F,14.000000F,8.000000F),PartPose.ZERO);
 return LayerDefinition.create(mesh,128,128);}
}
