package net.beamex.shamaschizm.grimoire.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.grimoire.GrimoireRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.LecternScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid=Shamaschizm.MOD_ID,value=Dist.CLIENT)
public final class GrimoireLecternClient {
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        Minecraft mc=Minecraft.getInstance();
        // Opening arrives before the slot contents. Wait for vanilla's server-synchronized stack.
        if(mc.player!=null && mc.gui.screen() instanceof LecternScreen screen
                && mc.player.containerMenu==screen.getMenu()
                && GrimoireRegistration.isBook(screen.getMenu().getBook())){
            mc.gui.setScreen(new GrimoireLecternScreen(screen.getMenu()));
        }
    }
    private GrimoireLecternClient(){}
}
