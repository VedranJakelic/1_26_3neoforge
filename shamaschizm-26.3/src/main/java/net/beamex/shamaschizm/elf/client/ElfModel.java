package net.beamex.shamaschizm.elf.client;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
public final class ElfModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(Shamaschizm.id("garden_elf"),"main");
    private final ModelPart head,leftArm,rightArm,leftLeg,rightLeg;
    public ElfModel(ModelPart root){super(root);head=root.getChild("head");leftArm=root.getChild("left_arm");rightArm=root.getChild("right_arm");leftLeg=root.getChild("left_leg");rightLeg=root.getChild("right_leg");}
    public static LayerDefinition layer(){
        MeshDefinition mesh=new MeshDefinition();PartDefinition root=mesh.getRoot();
        PartDefinition part_head=root.addOrReplaceChild("head",CubeListBuilder.create(),PartPose.offsetAndRotation(0.000000F,15.250000F,0.000000F,-0.000000F,-0.000000F,0.000000F));
        part_head.addOrReplaceChild("cube1",CubeListBuilder.create().texOffs(0, 22).addBox(-3.000000F,-6.250000F,-3.000000F,6.000000F,6.000000F,6.000000F),PartPose.ZERO);
        part_head.addOrReplaceChild("cube2",CubeListBuilder.create().texOffs(36, 40).addBox(-1.000000F,-3.250000F,-4.000000F,2.000000F,3.000000F,1.000000F),PartPose.ZERO);
        PartDefinition part_hat=part_head.addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.offsetAndRotation(0.000000F,-6.250000F,0.000000F,-0.130569F,0.007596F,0.086936F));
        part_hat.addOrReplaceChild("cube3",CubeListBuilder.create().texOffs(0, 0).addBox(-6.000000F,0.000000F,-6.000000F,12.000000F,0.000000F,12.000000F),PartPose.ZERO);
        part_hat.addOrReplaceChild("cube4",CubeListBuilder.create().texOffs(0, 41).addBox(-4.000000F,-2.000000F,-4.000000F,8.000000F,2.000000F,8.000000F),PartPose.ZERO);
        part_hat.addOrReplaceChild("cube5",CubeListBuilder.create().texOffs(40, 21).addBox(-3.000000F,-3.000000F,-3.000000F,6.000000F,3.000000F,6.000000F),PartPose.offsetAndRotation(0.000000F,-1.000000F,0.000000F,-0.087266F,-0.000000F,0.000000F));
        part_hat.addOrReplaceChild("cube6",CubeListBuilder.create().texOffs(28, 40).addBox(-1.000000F,-3.000000F,-1.000000F,2.000000F,4.000000F,2.000000F),PartPose.offsetAndRotation(0.000000F,-4.000000F,0.000000F,-0.349066F,-0.000000F,0.000000F));
        PartDefinition part_body=root.addOrReplaceChild("body",CubeListBuilder.create(),PartPose.offsetAndRotation(0.000000F,17.500000F,0.000000F,-0.000000F,-0.000000F,0.000000F));
        part_body.addOrReplaceChild("cube7",CubeListBuilder.create().texOffs(0, 34).addBox(-2.000000F,-2.500000F,-1.000000F,4.000000F,5.000000F,2.000000F),PartPose.ZERO);
        PartDefinition part_left_arm=root.addOrReplaceChild("left_arm",CubeListBuilder.create(),PartPose.offsetAndRotation(-3.000000F,15.500000F,0.000000F,-0.000000F,-0.000000F,0.000000F));
        part_left_arm.addOrReplaceChild("cube8",CubeListBuilder.create().texOffs(12, 34).addBox(-1.000000F,-0.500000F,-1.000000F,2.000000F,5.000000F,2.000000F),PartPose.ZERO);
        PartDefinition part_right_arm=root.addOrReplaceChild("right_arm",CubeListBuilder.create(),PartPose.offsetAndRotation(3.000000F,15.500000F,0.000000F,-0.000000F,-0.000000F,0.000000F));
        part_right_arm.addOrReplaceChild("cube9",CubeListBuilder.create().texOffs(20, 34).addBox(-1.000000F,-0.500000F,-1.000000F,2.000000F,5.000000F,2.000000F),PartPose.ZERO);
        PartDefinition part_left_leg=root.addOrReplaceChild("left_leg",CubeListBuilder.create(),PartPose.offsetAndRotation(-1.000000F,20.000000F,0.000000F,-0.000000F,-0.000000F,0.000000F));
        part_left_leg.addOrReplaceChild("cube10",CubeListBuilder.create().texOffs(28, 34).addBox(-1.000000F,0.000000F,-1.000000F,2.000000F,4.000000F,2.000000F),PartPose.ZERO);
        PartDefinition part_right_leg=root.addOrReplaceChild("right_leg",CubeListBuilder.create(),PartPose.offsetAndRotation(1.000000F,20.000000F,0.000000F,-0.000000F,-0.000000F,0.000000F));
        part_right_leg.addOrReplaceChild("cube11",CubeListBuilder.create().texOffs(36, 34).addBox(-1.000000F,0.000000F,-1.000000F,2.000000F,4.000000F,2.000000F),PartPose.ZERO);
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public void setupAnim(LivingEntityRenderState s){
        super.setupAnim(s);head.yRot=s.yRot*Mth.PI/180F;head.xRot=s.xRot*Mth.PI/180F;
        float swing=Mth.cos(s.walkAnimationPos*1.2F)*Math.min(s.walkAnimationSpeed*2,0.7F);
        leftLeg.xRot=swing;rightLeg.xRot=-swing;leftArm.xRot=-swing;rightArm.xRot=swing;
    }
}
