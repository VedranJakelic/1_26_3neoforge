package net.beamex.shamaschizm.plantedsword.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.plantedsword.PlantedSwordEntity;
import net.beamex.shamaschizm.plantedsword.PlantedSwords;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;

/** Client-only keys and rendering; no client classes are loaded on a dedicated server. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class PlantedSwordClient {
    private static boolean consumedPress;
    private PlantedSwordClient() {}

    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(PlantedSwords.TYPE, PlantedSwordRenderer::new);
    }

    @SubscribeEvent
    public static void release(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null || !mc.options.keyUse.isDown()) consumedPress = false;
    }

    @SubscribeEvent
    public static void use(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null || mc.player.isSpectator()) return;
        // Also suppress offhand processing and held-button repeats after the
        // server removes the sword, so the same click cannot pick it back up.
        if (consumedPress) {
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        if (!mc.hasControlDown() || !mc.hasShiftDown()
                || !mc.player.getOffhandItem().isEmpty()
                || !PlantedSwords.canPlant(mc.player.getMainHandItem())) return;
        // A deliberate click on an existing planted sword remains a pickup.
        if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof PlantedSwordEntity) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        consumedPress = true;
        mc.player.connection.send(new ServerboundCustomPayloadPacket(new PlantedSwords.Plant()));
    }
}
