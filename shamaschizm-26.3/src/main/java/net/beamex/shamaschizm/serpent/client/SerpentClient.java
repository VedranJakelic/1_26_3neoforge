package net.beamex.shamaschizm.serpent.client;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.serpent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID,value=Dist.CLIENT)
public final class SerpentClient {
 private static boolean jumpDown;
 @SubscribeEvent public static void renderer(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(Serpents.TYPE,SerpentRenderer::new);}
 @SubscribeEvent public static void tick(ClientTickEvent.Post e){
  var mc=Minecraft.getInstance();boolean down=mc.player!=null&&mc.gui.screen()==null&&mc.options.keyJump.isDown();
  if(down&&!jumpDown&&mc.level!=null)for(var serpent:mc.level.getEntitiesOfClass(SeaSerpent.class,mc.player.getBoundingBox().inflate(8)))if(serpent.heldId()==mc.player.getId()){mc.player.connection.send(new ServerboundCustomPayloadPacket(new SerpentEscape.Press(serpent.getId())));break;}
  jumpDown=down;
 }
}
