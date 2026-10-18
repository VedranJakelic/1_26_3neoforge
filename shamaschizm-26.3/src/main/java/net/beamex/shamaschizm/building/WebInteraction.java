package net.beamex.shamaschizm.building;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;


public final class WebInteraction {
    public record Contact(SpanningWebEntity web, Vec3 point) {}
    public static Contact trace(Player player) { return trace(player, player.getEyePosition(), player.getLookAngle()); }
    public static Contact trace(Player player, Vec3 from, Vec3 direction) {
        if (!player.isAlive() || player.isSpectator()) return null;
        Vec3 to = from.add(direction.scale(player.blockInteractionRange()));
        var block = player.level().clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        double limit = block.getType() == HitResult.Type.MISS ? from.distanceToSqr(to) : from.distanceToSqr(block.getLocation());
        var area = new AABB(from, to).inflate(5);
        for (var entity : player.level().getEntities(player, area, e -> e.isPickable() && !e.isSpectator())) {
            Vec3 hit = entity.getBoundingBox().clip(from, to).orElse(null);
            if (hit != null) limit = Math.min(limit, from.distanceToSqr(hit));
        }
        Contact result = null;
        for (var web : player.level().getEntitiesOfClass(SpanningWebEntity.class, area, e -> !e.isRemoved())) {
            Vec3 hit = web.rectangle().clip(from, to);
            if (hit != null && from.distanceToSqr(hit) <= limit + 1.0E-7) {
                limit = from.distanceToSqr(hit); result = new Contact(web, hit);
            }
        }
        return result;
    }
    public record Mine(int entityId, boolean down) implements CustomPacketPayload {
        public static final Type<Mine> TYPE = new Type<>(Shamaschizm.id("mine_spanning_web"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Mine> CODEC = new StreamCodec<>() {
            public Mine decode(RegistryFriendlyByteBuf buffer) { return new Mine(buffer.readVarInt(), buffer.readBoolean()); }
            public void encode(RegistryFriendlyByteBuf buffer, Mine payload) { buffer.writeVarInt(payload.entityId); buffer.writeBoolean(payload.down); }
        };
        public Type<Mine> type() { return TYPE; }
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(Mine.TYPE, Mine.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player
                    && player.level().getEntity(payload.entityId) instanceof SpanningWebEntity web)
                web.mine(player, payload.down);
        });
    }
    private WebInteraction() {}
}
