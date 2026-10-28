package net.beamex.shamaschizm.localportal;

import java.util.*;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-authoritative traversal prototype. Does not yet predict crossings on the client. */
@EventBusSubscriber(modid = "shamaschizm")
public final class DoorwayTraversal {
    private static final Map<ServerPlayer, Track> TRACKS = new WeakHashMap<>();
    private static final class Track {
        Vec3 previous;
        int cooldown;
        long revision = -1;
        int nextDiagnosticTick;
        Track(Vec3 p) { previous = p; }
    }

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.level().dimension().equals(Schizm.KEY) || !player.isAlive()) {
            if (TRACKS.remove(player) != null) PacketDistributor.sendToPlayer(player, new DoorwayNetwork.State(List.of()));
            return;
        }
        var level = player.level();
        var data = DoorwayData.get(level);
        Track track = TRACKS.computeIfAbsent(player, p -> new Track(p.position()));
        Vec3 current = player.position();
        if (track.revision != data.revision() || player.tickCount % 20 == 0) {
            PacketDistributor.sendToPlayer(player, new DoorwayNetwork.State(data.all().stream()
                    .filter(d -> d.origin().distanceToSqr(current) < 48 * 48).toList()));
            track.revision = data.revision();
        }
        Vec3 previous = track.previous;
        track.previous = current;
        if (track.cooldown > 0) { track.cooldown--; return; }
        // Riding, spectator movement and unrelated long-range teleports are not portal crossings.
        if (player.isPassenger() || player.isSpectator() || player.isSleeping()
                || previous.distanceToSqr(current) > 16 * 16) return;

        for (Doorway source : data.all()) {
            if (!source.enabled() || !source.crossed(previous, current, player.getBbWidth(), player.getBbHeight())) continue;
            Doorway target = data.get(source.target());
            if (target == null || !target.enabled() || !target.target().equals(source.name())) continue;
            double clearance = player.getBbWidth() / 2.0 + DoorwayExitPlacement.EPSILON;
            Vec3 destination = findExit(player, source, target, previous, current);
            if (destination == null) {
                if (player.tickCount >= track.nextDiagnosticTick) {
                    logObstruction(player, source, target, previous, current);
                    track.nextDiagnosticTick = player.tickCount + 100;
                }
                Vec3 local = source.local(previous);
                Vec3 stop = source.world(local.x, local.y, Math.max(clearance, local.z));
                player.teleportTo(level, stop.x, stop.y, stop.z, Set.of(), player.getYRot(), player.getXRot(), true);
                player.setDeltaMovement(Vec3.ZERO);
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
                player.sendSystemMessage(Component.literal("The linked doorway's exit is obstructed."));
                track.previous = stop; track.cooldown = 10;
                return;
            }
            Vec3 velocity = source.transformVector(target, player.getDeltaMovement());
            // Transform horizontal heading separately: looking straight up/down still preserves yaw.
            double yawRadians = Math.toRadians(player.getYRot());
            Vec3 heading = source.transformVector(target, new Vec3(-Math.sin(yawRadians), 0, Math.cos(yawRadians)));
            float yaw = (float)Math.toDegrees(Math.atan2(-heading.x, heading.z));
            float yawDelta = yaw - player.getYRot();
            double fall = player.fallDistance;
            player.teleportTo(level, destination.x, destination.y, destination.z, Set.of(), yaw, player.getXRot(), true);
            PacketDistributor.sendToPlayer(player, new DoorwayNetwork.Turn(yawDelta));
            player.setDeltaMovement(velocity);
            player.fallDistance = fall;
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
            track.previous = destination; track.cooldown = 3;
            return;
        }
    }
    static Vec3 findExit(Entity player, Doorway source, Doorway target,
                                 Vec3 previous, Vec3 current) {
        AABB bounds = traversalBounds(player);
        double width = bounds.getXsize();
        if (width > target.width() || bounds.getYsize() > target.height()) return null;
        Vec3 exact = DoorwayExitPlacement.anchor(source, target, previous, current, width);
        for (Vec3 candidate : DoorwayExitPlacement.cornerCandidates(target, exact, width)) {
            var local = target.local(candidate.add(0, bounds.minY - current.y, 0));
            if (Math.abs(local.x) + width / 2 > target.width() / 2 + DoorwayExitPlacement.EPSILON
                    || local.y < -0.02 || local.y + bounds.getYsize() > target.height() + 0.02) continue;
            Vec3 result = tryExit(player, source, target, current, candidate, bounds);
            if (result != null) return result;
        }
        return null;
    }

    private static Vec3 tryExit(Entity player, Doorway source, Doorway target,
                                Vec3 current, Vec3 anchor, AABB bounds) {
        var level = player.level();
        var box = bounds.move(anchor.subtract(current));
        if (box.maxY > target.origin().y + target.height() + 0.02) return null;
        if (!withinWorld(player, box)) return null;
        loadExitChunks(player, box);

        // Only step over block geometry below the player's normal step height.
        // Entity collisions and full-height obstructions are still rejected.
        var obstacles = new ArrayList<net.minecraft.world.phys.AABB>();
        for (var shape : level.getBlockCollisions(player, box.deflate(DoorwayExitPlacement.EPSILON)))
            obstacles.addAll(shape.toAabbs());
        double lift = DoorwayExitPlacement.floorLift(box, obstacles, Math.min(0.6, player.maxUpStep()));
        if (!Double.isFinite(lift)) return null;
        anchor = anchor.add(0, lift, 0);
        box = bounds.move(anchor.subtract(current));
        if (box.maxY > target.origin().y + target.height() + 0.02) return null;
        if (!withinWorld(player, box) || !level.noCollision(player, box.deflate(DoorwayExitPlacement.EPSILON))) return null;

        // The player's movement after the crossing is a movement to resolve, not
        // a reason to reject an otherwise clear portal (notably at a tight bend).
        Vec3 intended = source.transformPoint(target, current).add(0, lift, 0);
        double clearance = bounds.getXsize() / 2.0 + DoorwayExitPlacement.EPSILON;
        if (target.distance(intended) < clearance)
            intended = intended.add(target.facing().normal.scale(clearance - target.distance(intended)));
        Vec3 motion = intended.subtract(anchor);
        var swept = box.expandTowards(motion);
        if (!withinWorld(player, swept)) return anchor;
        loadExitChunks(player, swept);
        Vec3 allowed = net.minecraft.world.entity.Entity.collideBoundingBox(player, motion, box, level,
                level.getEntityCollisions(player, swept));
        Vec3 result = anchor.add(allowed);
        var resultBox = bounds.move(result.subtract(current));
        return withinWorld(player, resultBox) && level.noCollision(player, resultBox.deflate(DoorwayExitPlacement.EPSILON))
                ? result : anchor;
    }

    /** Start projectile collision simulation at the exit, without skipping targets in residual travel. */
    static Vec3 findProjectileExit(Entity entity, Doorway source, Doorway target, Vec3 before, Vec3 end) {
        double width=entity.getBbWidth(), height=entity.getBbHeight();
        if(width>target.width()||height>target.height())return null;
        Vec3 exact=DoorwayExitPlacement.anchor(source,target,before,end,width);
        for(Vec3 candidate:DoorwayExitPlacement.cornerCandidates(target,exact,width)) {
            Vec3 local=target.local(candidate);
            if(Math.abs(local.x)+width/2>target.width()/2+DoorwayExitPlacement.EPSILON
                    ||local.y<-.02||local.y+height>target.height()+.02)continue;
            AABB box=entity.getBoundingBox().move(candidate.subtract(entity.position()));
            if(!withinWorld(entity,box))continue;
            loadExitChunks(entity,box);
            // Entities at the exit must remain hittable by the projectile's own next collision sweep.
            if(!entity.level().getBlockCollisions(entity,box.deflate(DoorwayExitPlacement.EPSILON)).iterator().hasNext())return candidate;
        }
        return null;
    }

    /** Conservative square footprint also covers passenger offsets after a quarter turn. */
    static AABB traversalBounds(Entity root) {
        AABB bounds = root.getBoundingBox();
        for (Entity passenger : root.getIndirectPassengers()) bounds = bounds.minmax(passenger.getBoundingBox());
        double radius = Math.max(Math.max(root.getX() - bounds.minX, bounds.maxX - root.getX()),
                Math.max(root.getZ() - bounds.minZ, bounds.maxZ - root.getZ()));
        return new AABB(root.getX() - radius, bounds.minY, root.getZ() - radius,
                root.getX() + radius, bounds.maxY, root.getZ() + radius);
    }

    private static void logObstruction(ServerPlayer player, Doorway source, Doorway target,
                                       Vec3 previous, Vec3 current) {
        Vec3 anchor = DoorwayExitPlacement.anchor(source, target, previous, current, player.getBbWidth());
        var box = player.getBoundingBox().move(anchor.subtract(current));
        var blocks = new ArrayList<String>();
        for (var pos : net.minecraft.core.BlockPos.betweenClosed(
                net.minecraft.core.BlockPos.containing(box.minX, box.minY, box.minZ),
                net.minecraft.core.BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            var state = player.level().getBlockState(pos);
            if (!state.getCollisionShape(player.level(), pos).isEmpty()) blocks.add(pos.toShortString() + "=" + state);
        }
        org.slf4j.LoggerFactory.getLogger("shamaschizm.localportal").warn(
                "Exit rejected: source={} target={} from={} to={} anchor={} box={} bounds={} blocks={} entityShapes={}",
                source, target, previous, current, anchor, box, withinWorld(player, box), blocks,
                player.level().getEntityCollisions(player, box).size());
    }

    private static boolean withinWorld(Entity player, net.minecraft.world.phys.AABB box) {
        var level = player.level();
        return box.minY >= level.getMinY() && box.maxY <= level.getMaxY()
                && level.getWorldBorder().isWithinBounds(box);
    }

    private static void loadExitChunks(Entity player, net.minecraft.world.phys.AABB box) {
        for (int x = ((int)Math.floor(box.minX)) >> 4; x <= ((int)Math.floor(box.maxX)) >> 4; x++)
            for (int z = ((int)Math.floor(box.minZ)) >> 4; z <= ((int)Math.floor(box.maxZ)) >> 4; z++)
                player.level().getChunk(x, z);
    }

}
