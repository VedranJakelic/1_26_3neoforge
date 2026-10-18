package net.beamex.shamaschizm.event.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.PortalAtmosphere;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Smooths the once-per-second server value before the sky layer reads it. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class PortalAtmosphereClient {
    private static float proximity;
    private static Object lastLevel;

    private PortalAtmosphereClient() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != lastLevel) {
            lastLevel = minecraft.level;
            proximity = 0.0F;
            PortalAtmosphere.clientTargetProximity = 0.0F;
        }
        if (minecraft.level == null || minecraft.player == null || minecraft.isPaused()) return;
        float target = minecraft.level.dimension().equals(Level.OVERWORLD)
                ? PortalAtmosphere.clientTargetProximity : 0.0F;
        proximity = Mth.approach(proximity, target, 1.0F / 40.0F);
    }

    public static float proximity() {
        return Mth.clamp(proximity, 0.0F, 1.0F);
    }
}
