package net.beamex.shamaschizm.elf.client;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.effect.ModEffects;
import net.beamex.shamaschizm.elf.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID,value=Dist.CLIENT)
public final class ElfClient {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(ElfRegistration.ELF,ElfRenderer::new);}
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e){e.registerLayerDefinition(ElfModel.LAYER,ElfModel::layer);}
    private static Vec3 direction(){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||!mc.player.isAlive()||!mc.player.hasEffect(ModEffects.TRIPPING)){
            ElfNetwork.lockedElf=-1;return null;
        }
        var entity=mc.level.getEntity(ElfNetwork.lockedElf);
        if(!(entity instanceof ElfEntity)||entity.distanceToSqr(mc.player)>64){ElfNetwork.lockedElf=-1;return null;}
        return entity.position().add(0,0.85,0).subtract(mc.player.getEyePosition());
    }
    private static float yaw(Vec3 d){return (float)(Math.atan2(d.z,d.x)*180/Math.PI)-90;}
    private static float pitch(Vec3 d){return (float)(-Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))*180/Math.PI);}
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        if(ElfNetwork.lockedElf<0)return;Vec3 d=direction();if(d==null)return;
        var p=Minecraft.getInstance().player;p.setYRot(yaw(d));p.setXRot(pitch(d));
    }
    @SubscribeEvent public static void angles(ViewportEvent.ComputeCameraAngles e){
        if(ElfNetwork.lockedElf<0)return;Vec3 d=direction();if(d==null)return;
        e.setYaw(yaw(d));e.setPitch(pitch(d));e.setRoll(0);
    }
}
