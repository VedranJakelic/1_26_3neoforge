package net.beamex.shamaschizm.localportal.client;

import net.beamex.shamaschizm.localportal.DoorwayNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** Rebase interpolation at the actual client teleport, before another frame can render. */
@EventBusSubscriber(modid = "shamaschizm", value = Dist.CLIENT)
public final class DoorwayHandContinuity {
    private static Object armedPlayer;
    private static int armedTick;
    private static Snapshot snapshot;
    private record Snapshot(Vec3 movement, float yaw, float pitch, float oldYaw, float oldPitch,
                            float bobYaw, float oldBobYaw, float bobPitch, float oldBobPitch) {}
    @SubscribeEvent public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(DoorwayNetwork.Turn.TYPE, (packet, context) -> {
            var player = Minecraft.getInstance().player;
            if (player == null || !Float.isFinite(packet.yawDelta())) return;
            armedPlayer = player;
            armedTick = player.tickCount;
        });
    }
    public static void beforeTeleport() {
        snapshot = null;
        var p = Minecraft.getInstance().player;
        if (p != null && armedPlayer == p && p.tickCount - armedTick >= 0 && p.tickCount - armedTick <= 2) {
            snapshot = new Snapshot(p.oldPosition().subtract(p.position()), p.getYRot(), p.getXRot(),
                    p.yRotO, p.xRotO, p.yBob, p.yBobO, p.xBob, p.xBobO);
        }
        armedPlayer = null;
    }
    public static void afterTeleport() {
        var s = snapshot;
        snapshot = null;
        var p = Minecraft.getInstance().player;
        if (p == null || s == null) return;
        // The actual client change includes yaw wraparound and any recent mouse motion.
        float yaw = p.getYRot() - s.yaw(), pitch = p.getXRot() - s.pitch();
        p.yBob = s.bobYaw() + yaw;
        p.yBobO = s.oldBobYaw() + yaw;
        p.xBob = s.bobPitch() + pitch;
        p.xBobO = s.oldBobPitch() + pitch;
        // Preserve small walking interpolation, in the destination coordinate frame.
        double angle = Math.toRadians(yaw), c = Math.cos(angle), sin = Math.sin(angle);
        var v = s.movement();
        Vec3 previous = p.position().add(c * v.x - sin * v.z, v.y, sin * v.x + c * v.z);
        p.setOldPosAndRot(previous, s.oldYaw() + yaw, s.oldPitch() + pitch);
    }
}
