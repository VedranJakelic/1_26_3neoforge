package net.beamex.shamaschizm.homunculus.client;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

/** Supplied Blockbench geometry, UVs, rotations and walk keyframes. */
public final class HomunculusModel extends EntityModel<AvatarRenderState> {
    public static final ModelLayerLocation NORMAL=new ModelLayerLocation(Shamaschizm.id("homunculus"),"main");
    public static final ModelLayerLocation SLIM=new ModelLayerLocation(Shamaschizm.id("homunculus"),"slim");
    private final ModelPart tiny;
    private final ModelPart part0,part1,part2,part3;
    public HomunculusModel(ModelPart root,boolean slim){super(root);tiny=root.getChild("tiny");
        part0=tiny.getChild("part0");
        part3=tiny.getChild("part0").getChild("part3");
        part1=tiny.getChild("part1");
        part2=tiny.getChild("part2");
    }
    public static LayerDefinition layer(boolean slim){
        MeshDefinition mesh=new MeshDefinition();
        PartDefinition tiny=mesh.getRoot().addOrReplaceChild("tiny",CubeListBuilder.create(),PartPose.ZERO);
        PartDefinition part0=tiny.addOrReplaceChild("part0",CubeListBuilder.create(),PartPose.offsetAndRotation(0.000000F,24.000000F,0.000000F,0.000000F,0.000000F,0.000000F));
        part0.addOrReplaceChild("cube1",CubeListBuilder.create().texOffs(0,0).addBox(-4.000000F,-7.000000F,-6.000000F,8.000000F,7.000000F,7.000000F,new CubeDeformation(0.000000F)),PartPose.offsetAndRotation(0.000000F,0.000000F,0.000000F,0.000000F,0.000000F,0.000000F));
        PartDefinition part3=part0.addOrReplaceChild("part3",CubeListBuilder.create(),PartPose.offsetAndRotation(0.000000F,0.000000F,4.000000F,0.000000F,0.000000F,0.000000F));
        part3.addOrReplaceChild("cube2",CubeListBuilder.create().texOffs(0,14).addBox(-3.000000F,-5.000000F,-6.000000F,6.000000F,5.000000F,7.000000F,new CubeDeformation(0.000000F)),PartPose.offsetAndRotation(0.000000F,0.000000F,0.000000F,0.000000F,0.087266F,0.000000F));
        part3.addOrReplaceChild("cube3",CubeListBuilder.create().texOffs(0,26).addBox(-3.000000F,0.000000F,-1.000000F,6.000000F,0.000000F,5.000000F,new CubeDeformation(0.000000F)),PartPose.offsetAndRotation(0.000000F,0.000000F,1.000000F,0.000000F,0.218166F,0.000000F));
        part0.addOrReplaceChild("cube4",CubeListBuilder.create().texOffs(22,26).addBox(-2.000000F,-4.000000F,-1.000000F,4.000000F,4.000000F,4.000000F,new CubeDeformation(-0.100000F)),PartPose.offsetAndRotation(-3.000000F,0.000000F,-6.000000F,0.000000F,0.000000F,0.000000F));
        part0.addOrReplaceChild("cube5",CubeListBuilder.create().texOffs(26,14).addBox(-2.000000F,-3.000000F,-1.000000F,4.000000F,3.000000F,5.000000F,new CubeDeformation(-0.100000F)),PartPose.offsetAndRotation(3.000000F,0.000000F,-6.000000F,0.000000F,0.000000F,0.000000F));
        PartDefinition part1=tiny.addOrReplaceChild("part1",CubeListBuilder.create(),PartPose.offsetAndRotation(-4.000000F,24.000000F,0.000000F,0.000000F,0.000000F,0.000000F));
        part1.addOrReplaceChild("cube6",CubeListBuilder.create().texOffs(30,4).addBox(-1.000000F,-2.000000F,-1.000000F,2.000000F,2.000000F,2.000000F,new CubeDeformation(0.100000F)),PartPose.offsetAndRotation(0.000000F,0.000000F,0.000000F,0.000000F,0.000000F,0.000000F));
        part1.addOrReplaceChild("cube7",CubeListBuilder.create().texOffs(26,22).addBox(-2.000000F,0.000000F,-3.000000F,4.000000F,0.000000F,4.000000F,new CubeDeformation(0.000000F)),PartPose.offsetAndRotation(-2.000000F,0.000000F,0.000000F,0.000000F,0.000000F,0.000000F));
        PartDefinition part2=tiny.addOrReplaceChild("part2",CubeListBuilder.create(),PartPose.offsetAndRotation(4.000000F,24.000000F,0.000000F,0.000000F,0.000000F,0.000000F));
        part2.addOrReplaceChild("cube8",CubeListBuilder.create().texOffs(30,8).addBox(-1.000000F,-2.000000F,-1.000000F,2.000000F,2.000000F,2.000000F,new CubeDeformation(0.100000F)),PartPose.offsetAndRotation(0.000000F,0.000000F,0.000000F,0.000000F,0.000000F,0.000000F));
        part2.addOrReplaceChild("cube9",CubeListBuilder.create().texOffs(30,0).addBox(-2.000000F,0.000000F,-3.000000F,4.000000F,0.000000F,4.000000F,new CubeDeformation(0.000000F)),PartPose.offsetAndRotation(2.000000F,0.000000F,0.000000F,0.000000F,0.000000F,0.000000F));
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public void setupAnim(AvatarRenderState state){
        super.setupAnim(state);
        tiny.visible=true;
        float time=(state.walkAnimationPos*.35F)%3F, weight=Math.min(1F,state.walkAnimationSpeed*3F);
        animate(part1,time,weight,KEYS_part1);
        animate(part2,time,weight,KEYS_part2);
        animate(part3,time,weight,KEYS_part3);
    }
    private static final float[][] KEYS_part1={{0.000000F,-0.000000F,-0.000000F,0.000000F},{0.500000F,-0.052972F,0.166379F,-0.309850F},{1.000000F,-0.000000F,0.349066F,0.000000F},{1.750000F,-0.000000F,-0.523599F,0.000000F},{2.583330F,0.035336F,-0.131095F,-0.264117F},{2.875000F,-0.000000F,-0.000000F,0.000000F}};
    private static final float[][] KEYS_part2={{0.000000F,-0.000000F,-0.000000F,0.000000F},{1.000000F,-0.000000F,0.567232F,0.000000F},{1.583330F,-0.050054F,-0.136614F,0.352490F},{1.750000F,-0.000000F,-0.349066F,0.000000F},{2.916670F,-0.000000F,-0.000000F,0.000000F}};
    private static final float[][] KEYS_part3={{0.000000F,-0.000000F,-0.000000F,0.000000F},{1.875000F,-0.000000F,0.174533F,0.000000F},{3.000000F,-0.000000F,-0.000000F,0.000000F}};
    private static void animate(ModelPart p,float t,float weight,float[][] keys){
        float[] a=keys[0], b=keys[keys.length-1];
        for(int i=1;i<keys.length;i++)if(t<=keys[i][0]){a=keys[i-1];b=keys[i];break;}
        float f=Mth.clamp((t-a[0])/Math.max(.001F,b[0]-a[0]),0,1);
        p.xRot+=Mth.lerp(f,a[1],b[1])*weight;p.yRot+=Mth.lerp(f,a[2],b[2])*weight;p.zRot+=Mth.lerp(f,a[3],b[3])*weight;
    }
}
