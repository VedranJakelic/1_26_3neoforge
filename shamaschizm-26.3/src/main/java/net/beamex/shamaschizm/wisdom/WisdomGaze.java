package net.beamex.shamaschizm.wisdom;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.localportal.DoorwayData;
import net.beamex.shamaschizm.localportal.PortalInteractionRay;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Player-specific, server-authoritative gaze; remote portal replicas need no client ray guessing. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class WisdomGaze {
    public record Lens(float strength, float proximity) implements CustomPacketPayload {
        public static final Type<Lens> TYPE = new Type<>(Shamaschizm.id("wisdom_lens"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Lens> CODEC = new StreamCodec<>() {
            public Lens decode(RegistryFriendlyByteBuf b) { return new Lens(Math.clamp(b.readFloat(), 0, 1),Math.clamp(b.readFloat(),0,1)); }
            public void encode(RegistryFriendlyByteBuf b, Lens p) { b.writeFloat(p.strength);b.writeFloat(p.proximity); }
        };
        public Type<Lens> type() { return TYPE; }
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToClient(Lens.TYPE, Lens.CODEC);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 2 != 0) return;
        float strength = 0, proximity = 0;
        if (player.isAlive() && !player.isSpectator()) {
            Vec3 eye = player.getEyePosition(), end = eye.add(player.getLookAngle().scale(10));
            var portal = player.level().dimension().equals(Schizm.KEY)
                    ? PortalInteractionRay.trace(player, DoorwayData.get(player.level()).all(), 10) : null;
            if (portal == null) strength = find(player, eye, end, 0);
            else strength = Math.max(find(player,eye,eye.add(player.getLookAngle().scale(portal.before())),0),
                    find(player, portal.start(), portal.end(), portal.before()));
            proximity=proximity(player);
        }
        PacketDistributor.sendToPlayer(player, new Lens(strength,proximity));
    }
    private static float proximity(ServerPlayer player){
        var level=player.level();Vec3 eye=player.getEyePosition();double nearest=10;
        for(var mob:level.getEntitiesOfClass(TaintedWisdom.class,new AABB(eye,eye).inflate(10),TaintedWisdom::isAlive))
            nearest=Math.min(nearest,eye.distanceTo(mob.getBoundingBox().getCenter()));
        if(level.dimension().equals(Schizm.KEY)){
            var data=DoorwayData.get(level);
            var openings=data.all().stream().filter(d->d.enabled()&&d.distance(eye)>=0&&d.origin().distanceToSqr(eye)<100)
                    .sorted(java.util.Comparator.comparingDouble(d->d.origin().distanceToSqr(eye))).limit(2).toList();
            for(var source:openings){
                var target=data.get(source.target());if(target==null)continue;
                for(var mob:level.getEntitiesOfClass(TaintedWisdom.class,new AABB(target.origin(),target.origin()).inflate(10+target.height()),TaintedWisdom::isAlive)){
                    var path=net.beamex.shamaschizm.localportal.PortalSoundPath.find(source,target,eye,mob.getBoundingBox().getCenter(),10);
                    if(path!=null)nearest=Math.min(nearest,path.distance());
                }
            }
        }
        return (float)Math.clamp(1-nearest/10,0,1);
    }
    private static float find(ServerPlayer player, Vec3 start, Vec3 end, double before) {
        var level = player.level();
        double nearest = Double.POSITIVE_INFINITY;
        var wall = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double clearDistance = wall.getType() == HitResult.Type.MISS ? start.distanceTo(end) : start.distanceTo(wall.getLocation());
        for (var mob : level.getEntitiesOfClass(TaintedWisdom.class, new AABB(start, end).inflate(.6), TaintedWisdom::isAlive)) {
            var hit = mob.getBoundingBox().inflate(.12).clip(start, end);
            if (hit.isEmpty()) continue;
            double along = start.distanceTo(hit.get());
            if (along <= clearDistance + 1e-5 && before + along <= 10) nearest = Math.min(nearest, before + along);
        }
        return Double.isFinite(nearest) ? (float)(.35 + .65 * (1 - nearest / 10)) : 0;
    }
}
