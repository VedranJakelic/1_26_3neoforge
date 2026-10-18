package net.beamex.shamaschizm.building.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

/** Hide only the web selection outline, including the high-contrast outline pass. */
@EventBusSubscriber(modid = "shamaschizm", value = Dist.CLIENT)
public final class WebOutlineClient {
    private static final Identifier WEB = Identifier.fromNamespaceAndPath("shamaschizm", "spanning_web");

    @SubscribeEvent
    public static void outline(ExtractBlockOutlineRenderStateEvent event) {
        if (WEB.equals(BuiltInRegistries.BLOCK.getKey(event.getBlockState().getBlock()))) {
            event.setCanceled(true);
        }
    }

    private WebOutlineClient() {}
}
