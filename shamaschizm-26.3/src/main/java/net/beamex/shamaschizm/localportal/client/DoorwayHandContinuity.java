package net.beamex.shamaschizm.localportal.client;

import net.beamex.shamaschizm.localportal.DoorwayNetwork;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** Keep first-person hand sway relative to the view when the doorway rotates the world. */
@EventBusSubscriber(modid = "shamaschizm", value = Dist.CLIENT)
public final class DoorwayHandContinuity {
    @SubscribeEvent public static void register(RegisterClientPayloadHandlersEvent event) {
        // Runs on the client main thread, in packet order after the vanilla teleport.
        // Do not defer this to a client tick: a frame could otherwise use stale hand rotation.
        event.register(DoorwayNetwork.Turn.TYPE, (packet, context) -> {
            var player = Minecraft.getInstance().player;
            if (player == null || !Float.isFinite(packet.yawDelta())) return;
            // Retain both interpolation endpoints and existing mouse-induced hand sway.
            // Use the raw yaw difference, including wraparound, just like the teleport.
            player.yBob += packet.yawDelta();
            player.yBobO += packet.yawDelta();
        });
    }
}
