package net.beamex.shamaschizm.earthworm;

import net.beamex.shamaschizm.eclipse.Eclipses;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.ModEntities;
import net.beamex.shamaschizm.entity.custom.RoachEntity;
import net.beamex.shamaschizm.experimental.BossGardenLocks;
import net.beamex.shamaschizm.saints.audio.ArenaZones;
import net.beamex.shamaschizm.world.Schizm;
import net.beamex.shamaschizm.world.dungeon.GardenZoneEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Extra bounded group attempts, independent of vanilla's small ambient cap. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class SchizmBugSpawning {
    // Tuning: all frequencies are attempts, not guaranteed successful spawns.
    private static final int INTERVAL_TICKS = 20;
    private static final int UPPER_Y = 160;
    private static final int DEEP_Y = 60;
    private static final int SHALLOW_GROUP_ATTEMPTS = 1;
    private static final int DEEP_GROUP_ATTEMPTS = 6;
    private static final int SHALLOW_LOCAL_CAP = 16;
    private static final int DEEP_LOCAL_CAP = 96;
    private static final int LEVEL_CAP = 768;
    private static final int MAX_PLACEMENT_ATTEMPTS_PER_PASS = 192;
    private static final int MAX_ADDITIONS_PER_PASS = 40;

    private SchizmBugSpawning() {}

    private static boolean bug(Entity entity) {
        return entity instanceof RoachEntity || entity instanceof EarthwormEntity;
    }

    private static float depth(int y) {
        return Mth.clamp((UPPER_Y - y) / (float)(UPPER_Y - DEEP_Y), 0F, 1F);
    }

    private static boolean allowed(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.below())
                || !level.getWorldBorder().isWithinBounds(pos)
                || !Eclipses.habitat(level, pos)
                || !Eclipses.active(level, pos) && level.getMaxLocalRawBrightness(pos) > 7
                || GardenZoneEntity.contains(level, pos)
                || ArenaZones.get(level).arena(pos)
                || BossGardenLocks.get(level).insideArena(pos)) return false;
        Vec3 point = Vec3.atBottomCenterOf(pos).add(0, .1, 0);
        for (var player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            if (player.position().distanceToSqr(point) < 8 * 8) return false;
            Vec3 towards = point.subtract(player.getEyePosition());
            if (towards.lengthSqr() <= 48 * 48
                    && player.getLookAngle().dot(towards.normalize()) >= .20
                    && level.clip(new ClipContext(player.getEyePosition(), point,
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                            .getType() == HitResult.Type.MISS) return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(level.dimension().equals(Schizm.KEY) || level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                    && net.beamex.shamaschizm.eclipse.EclipseOrbit.nearest(level.getOverworldClockTime()).active(level.getOverworldClockTime()))
                || level.getGameTime() % INTERVAL_TICKS != 0
                || !level.getGameRules().get(GameRules.SPAWN_MOBS)) return;

        int total = 0;
        int ordinaryTotal = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity.isAlive() && bug(entity)) {
                if (++total >= LEVEL_CAP) return;
                if (!net.beamex.shamaschizm.world.dungeon.DeepRoomZones.small(level, entity.blockPosition())) ordinaryTotal++;
            }
        }
        var players = new java.util.ArrayList<>(level.players());
        if (players.isEmpty()) return;
        // Rotate the first player so one player cannot monopolize the budget.
        int start = (int)((level.getGameTime() / INTERVAL_TICKS) % players.size());
        int attemptsLeft = MAX_PLACEMENT_ATTEMPTS_PER_PASS;
        int ordinaryAttemptsLeft = 160;
        int ordinaryAdditionsLeft = 32;
        int additionsLeft = Math.min(MAX_ADDITIONS_PER_PASS, LEVEL_CAP - total);
        for (int index = 0; index < players.size(); index++) {
            var player = players.get((start + index) % players.size());
            if (!player.isAlive() || player.isSpectator() || !Eclipses.habitat(level, player.blockPosition())) continue;
            int groups = net.beamex.shamaschizm.world.dungeon.DeepRoomZones.bugHabitat(level, player.blockPosition()) ? 6
                    : Math.round(Mth.lerp(depth(Eclipses.y(level, player.blockPosition())), SHALLOW_GROUP_ATTEMPTS, DEEP_GROUP_ATTEMPTS));
            for (int group = 0; group < groups; group++) {
                double angle = level.getRandom().nextDouble() * Math.PI * 2;
                double radius = 12 + level.getRandom().nextDouble() * 12;
                BlockPos center = BlockPos.containing(player.getX() + Math.cos(angle) * radius,
                        player.getY(), player.getZ() + Math.sin(angle) * radius);
                // Never load new chunks for these attempts, including group spread.
                if (!level.hasChunksAt(center.offset(-5, -4, -5), center.offset(5, 4, 5))) continue;
                boolean deepRoom = net.beamex.shamaschizm.world.dungeon.DeepRoomZones.small(level, center);
                boolean taggedDeep = net.beamex.shamaschizm.world.dungeon.DeepRoomZones.contains(level, center);
                if (!deepRoom && (level.getGameTime() % 40 != 0 || ordinaryTotal >= 256)) continue;
                int cap = deepRoom ? (taggedDeep ? 264 : 240) : Math.round(Mth.lerp(depth(Eclipses.y(level, center)), SHALLOW_LOCAL_CAP, DEEP_LOCAL_CAP) * (taggedDeep ? 1.2F : 1F));
                int nearby = level.getEntitiesOfClass(Mob.class, new AABB(center).inflate(32, 12, 32),
                        mob -> mob.isAlive() && bug(mob)).size();
                int room = Math.min(cap - nearby, additionsLeft);
                if (!deepRoom) room = Math.min(room, 256 - ordinaryTotal);
                if (!deepRoom) room = Math.min(room, ordinaryAdditionsLeft);
                if (room <= 0) continue;
                boolean brood = deepRoom && level.getRandom().nextInt(12) == 0;
                int roll = brood ? 1 : level.getRandom().nextInt(5);
                EntityType<? extends Mob> type = roll == 0 ? ModEntities.ROACH.get()
                        : roll <= (deepRoom ? 1 : 2) ? ModEntities.BABY_ROACH.get() : Earthworms.TYPE;
                float density = 1F;
                int size = type == Earthworms.TYPE
                        ? 3 + Math.round(density * 7) + level.getRandom().nextInt(7)
                        : type == ModEntities.BABY_ROACH.get()
                        ? 6 + Math.round(density * 10) + level.getRandom().nextInt(9)
                        : 3 + Math.round(density * 3) + level.getRandom().nextInt(5);
                if (!deepRoom) size = type == Earthworms.TYPE ? 1 + level.getRandom().nextInt(4)
                        : type == ModEntities.BABY_ROACH.get() ? 4 + level.getRandom().nextInt(5)
                        : 2 + level.getRandom().nextInt(3);
                if (!deepRoom && taggedDeep) size = Math.max(size + 1, Math.round(size * 1.2F));
                if (brood) size = 20 + level.getRandom().nextInt(9);
                for (int member = 0; member < Math.min(size, room); member++) {
                    if (attemptsLeft < 4 || additionsLeft <= 0) return;
                    if (!deepRoom && ordinaryAttemptsLeft < 4) break;
                    attemptsLeft -= 4;
                    if (!deepRoom) ordinaryAttemptsLeft -= 4;
                    var spawned = SpawnUtil.trySpawnMob(type, EntitySpawnReason.EVENT, level,
                            center, 4, 3, 3,
                            (world, floor, floorState, pos, state) ->
                                    net.beamex.shamaschizm.world.dungeon.DeepRoomZones.small(world, pos) == deepRoom
                                    && allowed(world, pos)
                                    && SpawnUtil.Strategy.ON_TOP_OF_COLLIDER_NO_LEAVES
                                            .canSpawnOn(world, floor, floorState, pos, state), true);
                    if (spawned.isPresent()) { additionsLeft--; if (!deepRoom) { ordinaryTotal++; ordinaryAdditionsLeft--; } }
                }
            }
        }
    }
}

