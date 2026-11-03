package net.beamex.shamaschizm.localportal.client;

import java.util.List;
import net.beamex.shamaschizm.localportal.Doorway;
import net.beamex.shamaschizm.localportal.DoorwayNetwork;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Receives local-portal metadata for the destination-view renderer.
 *
 * The old debug geometry (colored outlines and orientation ticks) is deliberately
 * not submitted. This class and doorways() remain in place because the portal
 * view renderer uses the synchronized list to select active portals.
 */
@EventBusSubscriber(modid = "shamaschizm", value = Dist.CLIENT)
public final class DoorwayDebugRenderer {
    private static List<Doorway> visible = List.of();

    private DoorwayDebugRenderer() {}

    public static List<Doorway> doorways() {
        return visible;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        var pending = DoorwayNetwork.PENDING.getAndSet(null);
        if (mc.level == null || !mc.level.dimension().equals(Schizm.KEY)) {
            visible = List.of();
        } else if (pending != null) {
            visible = pending;
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        visible = List.of();
        DoorwayNetwork.PENDING.set(null);
    }
}
