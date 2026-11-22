package net.beamex.shamaschizm.wisdom.giant.client;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.wisdom.giant.GiantWisdomNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Input;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID,value=Dist.CLIENT)
public final class GiantWisdomClient {
    private static int crouch,ticks;private static float strength;private static boolean warning;
    private static Object player,world;
    @SubscribeEvent public static void register(RegisterClientPayloadHandlersEvent e){
        e.register(GiantWisdomNetwork.Quake.TYPE,(p,c)->c.enqueueWork(()->{
            var mc=Minecraft.getInstance();
            if(player!=mc.player||world!=mc.level){ticks=crouch=0;}
            player=mc.player;world=mc.level;
            // Do not replace a hit-induced earthquake with a weaker proximity warning.
            if(p.warning()&&ticks>0&&!warning)return;
            strength=Float.isFinite(p.strength())?Math.clamp(p.strength(),0,1):0;
            crouch=Math.max(crouch,Math.clamp(p.crouch(),0,20));ticks=p.warning()?10:18;warning=p.warning();
        }));
    }
    private static boolean valid(){var mc=Minecraft.getInstance();return mc.player!=null&&mc.player==player&&mc.level==world&&mc.player.isAlive()&&mc.getCameraEntity()==mc.player;}
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        if(!valid()){ticks=crouch=0;return;}if(Minecraft.getInstance().isPaused())return;
        if(ticks>0)ticks--;if(crouch>0)crouch--;
    }
    @SubscribeEvent public static void input(MovementInputUpdateEvent e){
        if(crouch==0||!valid())return;
        var a=e.getInput().keyPresses;e.getInput().keyPresses=new Input(a.forward(),a.backward(),a.left(),a.right(),false,true,false);
    }
    @SubscribeEvent public static void camera(ViewportEvent.ComputeCameraAngles e){
        if(ticks==0||!valid())return;
        double t=(Minecraft.getInstance().player.tickCount+e.getPartialTick());
        float fade=ticks/(warning?10F:18F),a=strength*fade;
        // Render-only offsets: never modify player aim or accumulate rotations between portal views.
        e.setPitch(e.getPitch()+(float)(Math.sin(t*(warning?.5:2.4))*a*2.2));
        e.setYaw(e.getYaw()+(float)(Math.sin(t*(warning?.3:1.7))*a*1.3));
        e.setRoll(e.getRoll()+(float)(Math.sin(t*(warning?.4:2.1))*a*2.8));
    }
}
