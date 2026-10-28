package net.beamex.shamaschizm.localportal.client;

import net.beamex.shamaschizm.localportal.*;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class PortalInteractionClient {
    private static boolean holding;
    private static int lastHold=-1, lastUse=-100;
    private static Object player;
    public static boolean input(PortalInteractionPacket.Action action,boolean down) {
        var mc=Minecraft.getInstance();
        if(player!=mc.player){player=mc.player;holding=false;lastHold=-1;lastUse=-100;}
        if(mc.player==null||mc.level==null)return false;
        double reach=action==PortalInteractionPacket.Action.USE?Math.max(mc.player.blockInteractionRange(),mc.player.entityInteractionRange()):
            Math.max(mc.player.blockInteractionRange(),mc.player.getAttackRangeWith(mc.player.getMainHandItem()).effectiveMaxRange(mc.player));
        var ray=down&&mc.gui.screen()==null&&!mc.player.isSpectator()&&mc.level.dimension().equals(Schizm.KEY)
            ?PortalInteractionRay.trace(mc.player,DoorwayDebugRenderer.doorways(),reach):null;
        boolean remote=ray!=null;
        if(!remote){if(holding){send(PortalInteractionPacket.Action.RELEASE);holding=false;}return false;}
        if(!PortalViewRenderer.readyForInteraction(ray.source())){
            if(holding){send(PortalInteractionPacket.Action.RELEASE);holding=false;}
            return true;
        }
        int tick=mc.player.tickCount;
        if(!holding&&action!=PortalInteractionPacket.Action.USE&&mc.gameMode!=null)mc.gameMode.stopDestroyBlock();
        if(action==PortalInteractionPacket.Action.HOLD){
            holding=true;
            if(lastHold!=tick){send(action);lastHold=tick;if(tick%5==0)mc.player.swing(InteractionHand.MAIN_HAND,SwingAnimation.DEFAULT,true);}
        }else if(action==PortalInteractionPacket.Action.USE){
            if(tick-lastUse>=4){send(action);lastUse=tick;mc.player.swing(InteractionHand.MAIN_HAND,SwingAnimation.DEFAULT,true);}
        }else{send(action);holding=true;mc.player.swingAndResetAttackStrength(InteractionHand.MAIN_HAND,SwingAnimation.DEFAULT,true);}
        return true;
    }
    private static void send(PortalInteractionPacket.Action action){ClientPacketDistributor.sendToServer(new PortalInteractionPacket(action));}
}
