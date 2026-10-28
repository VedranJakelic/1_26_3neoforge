package net.beamex.shamaschizm.localportal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.beamex.shamaschizm.localportal.*;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Honest debug outlines: this is not the destination-view renderer. */
@EventBusSubscriber(modid = "shamaschizm", value = Dist.CLIENT)
public final class DoorwayDebugRenderer {
    private static List<Doorway> visible = List.of();
    public static List<Doorway> doorways() { return visible; }
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath("shamaschizm", "textures/misc/local_portal_debug.png");
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        var pending = DoorwayNetwork.PENDING.getAndSet(null);
        if (mc.level == null || !mc.level.dimension().equals(Schizm.KEY)) visible = List.of();
        else if (pending != null) visible = pending;
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        visible = List.of(); DoorwayNetwork.PENDING.set(null);
    }
    @SubscribeEvent public static void submit(SubmitCustomGeometryEvent event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().equals(Schizm.KEY) || visible.isEmpty()) return;
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        var poses = event.getPoseStack(); poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        List<Doorway> snapshot = visible;
        event.getSubmitNodeCollector().submitCustomGeometry(poses, RenderTypes.entityCutout(WHITE, false), (pose, out) -> {
            for (Doorway d : snapshot) {
                double w = d.width() / 2, h = d.height(), t = 0.025;
                rectangle(pose, out, d, -w, 0, -w + t, h);
                rectangle(pose, out, d, w - t, 0, w, h);
                rectangle(pose, out, d, -w, 0, w, t);
                rectangle(pose, out, d, -w, h - t, w, h);
                // A short upright tick half a block toward the approach side identifies the front.
                quad(pose, out, d, d.world(-t, 0, 0.5), d.world(t, 0, 0.5),
                        d.world(t, 0.25, 0.5), d.world(-t, 0.25, 0.5));
            }
        });
        poses.popPose();
    }
    private static void rectangle(PoseStack.Pose pose, VertexConsumer out, Doorway d, double x0, double y0, double x1, double y1) {
        quad(pose, out, d, d.world(x0,y0,0), d.world(x1,y0,0), d.world(x1,y1,0), d.world(x0,y1,0));
    }
    private static void quad(PoseStack.Pose pose, VertexConsumer out, Doorway d, Vec3 a, Vec3 b, Vec3 c, Vec3 e) {
        for (Vec3 p : new Vec3[]{a,b,c,e,e,c,b,a}) out.addVertex(pose,(float)p.x,(float)p.y,(float)p.z)
                .setColor(d.enabled() ? 30 : 255, d.enabled() ? 220 : 175, d.enabled() ? 255 : 30, 255)
                .setUv(0.5f,0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880)
                .setNormal(pose, (float)d.facing().normal.x, 0, (float)d.facing().normal.z);
    }
}
