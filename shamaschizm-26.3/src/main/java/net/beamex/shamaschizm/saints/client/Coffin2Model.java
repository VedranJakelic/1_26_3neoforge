package net.beamex.shamaschizm.saints.client;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
public final class Coffin2Model extends EntityModel<Coffin2Renderer.State> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(Shamaschizm.id("coffin2"),"main");
    private final ModelPart lid,dirt;
    private final KeyframeAnimation open;
    public Coffin2Model(ModelPart root){super(root);lid=root.getChild("lid");dirt=root.getChild("dirt");open=Coffin2Animations.OPEN.bake(root);}
    public static LayerDefinition layer(){
        MeshDefinition mesh=new MeshDefinition();PartDefinition root=mesh.getRoot();
        PartDefinition bone=root.addOrReplaceChild("bone",CubeListBuilder.create(),PartPose.offset(0,24,0));
        bone.addOrReplaceChild("cube_0",CubeListBuilder.create().texOffs(0,0)
                .addBox(0.000000F,-9.000000F,-8.000000F,1.000000F,9.000000F,32.000000F,new CubeDeformation(0)),
                PartPose.offsetAndRotation(7.000000F,0.000000F,0.000000F,0.000000F,0.000000F,0.000000F));
        bone.addOrReplaceChild("cube_1",CubeListBuilder.create().texOffs(0,41)
                .addBox(0.000000F,-9.000000F,-8.000000F,1.000000F,9.000000F,32.000000F,new CubeDeformation(0)),
                PartPose.offsetAndRotation(-8.000000F,0.000000F,0.000000F,0.000000F,0.000000F,0.000000F));
        bone.addOrReplaceChild("cube_2",CubeListBuilder.create().texOffs(66,0)
                .addBox(0.000000F,-14.000000F,-7.000000F,1.000000F,14.000000F,30.000000F,new CubeDeformation(0)),
                PartPose.offsetAndRotation(-7.000000F,-1.000000F,0.000000F,0.000000F,0.000000F,1.570796F));
        bone.addOrReplaceChild("cube_3",CubeListBuilder.create().texOffs(64,86)
                .addBox(-7.000000F,-9.000000F,-1.000000F,14.000000F,9.000000F,1.000000F,new CubeDeformation(0)),
                PartPose.offsetAndRotation(0.000000F,0.000000F,-7.000000F,0.000000F,0.000000F,0.000000F));
        bone.addOrReplaceChild("cube_4",CubeListBuilder.create().texOffs(94,86)
                .addBox(-7.000000F,-9.000000F,-1.000000F,14.000000F,9.000000F,1.000000F,new CubeDeformation(0)),
                PartPose.offsetAndRotation(0.000000F,0.000000F,24.000000F,0.000000F,0.000000F,0.000000F));
        PartDefinition lid=root.addOrReplaceChild("lid",CubeListBuilder.create(),PartPose.offset(0,24,0));
        lid.addOrReplaceChild("cube_0",CubeListBuilder.create().texOffs(66,68)
                .addBox(-8.212270F,-2.000000F,-8.000000F,16.000000F,2.000000F,16.000000F,new CubeDeformation(0)),
                PartPose.offsetAndRotation(0.212270F,-9.000000F,0.011310F,0.000000F,0.000000F,0.000000F));
        lid.addOrReplaceChild("cube_1",CubeListBuilder.create().texOffs(0,82)
                .addBox(-7.212270F,-2.000000F,-8.000000F,16.000000F,2.000000F,16.000000F,new CubeDeformation(0)),
                PartPose.offsetAndRotation(-0.787730F,-9.000000F,16.011310F,0.000000F,0.000000F,0.000000F));
        PartDefinition dirt=root.addOrReplaceChild("dirt",CubeListBuilder.create(),PartPose.offset(0,24,0));
        dirt.addOrReplaceChild("cube_0",CubeListBuilder.create().texOffs(0,100)
                .addBox(-7.000000F,-7.000000F,-7.000000F,14.000000F,7.000000F,30.000000F,new CubeDeformation(0)),
                PartPose.offsetAndRotation(0.000000F,0.000000F,0.000000F,0.000000F,0.000000F,0.000000F));
        return LayerDefinition.create(mesh,256,256);
    }
    @Override public void setupAnim(Coffin2Renderer.State state){
        super.setupAnim(state);dirt.visible=false;lid.visible=!state.lidGone;
        if(state.open)open.apply(state.animation,state.ageInTicks);
    }
}
