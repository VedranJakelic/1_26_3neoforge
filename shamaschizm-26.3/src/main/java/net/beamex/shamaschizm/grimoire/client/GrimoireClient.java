package net.beamex.shamaschizm.grimoire.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.grimoire.GrimoireRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid=Shamaschizm.MOD_ID,value=Dist.CLIENT)
public final class GrimoireClient {
    @SubscribeEvent public static void read(PlayerInteractEvent.RightClickItem event) {
        Minecraft mc=Minecraft.getInstance();
        if(event.getEntity()!=mc.player || !GrimoireRegistration.isBook(event.getItemStack())) return;
        if(mc.gui.screen()==null) mc.gui.setScreen(new GrimoireScreen());
        event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true);
    }
}
