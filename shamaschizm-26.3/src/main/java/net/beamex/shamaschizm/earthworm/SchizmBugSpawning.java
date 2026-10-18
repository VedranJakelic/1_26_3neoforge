package net.beamex.shamaschizm.earthworm;

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
    private static final int INTERVAL_TICKS = 40;
    private static final int UPPER_Y = 160;
    private static final int DEEP_Y = 60;
    private static final int SHALLOW_GROUP_ATTEMPTS = 1;
    private static final int DEEP_GROUP_ATTEMPTS = 6;
    private static final int SHALLOW_LOCAL_CAP = 16;
    private static final int DEEP_LOCAL_CAP = 96;
    private static final int LEVEL_CAP = 256;
    private static final int MAX_PLACEMENT_ATTEMPTS_PER_PASS = 160;
    private static final int MAX_ADDITIONS_PER_PASS = 32;

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
                || level.getMaxLocalRawBrightness(pos) > 7
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
                || !level.dimension().equals(Schizm.KEY)
                || level.getGameTime() % INTERVAL_TICKS != 0
                || !level.getGameRules().get(GameRules.SPAWN_MOBS)) return;

        int total = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity.isAlive() && bug(entity) && ++total >= LEVEL_CAP) return;
        }
        var players = new java.util.ArrayList<>(level.players());
        if (players.isEmpty()) return;
        // Rotate the first player so one player cannot monopolize the budget.
        int start = (int)((level.getGameTime() / INTERVAL_TICKS) % players.size());
        int attemptsLeft = MAX_PLACEMENT_ATTEMPTS_PER_PASS;
        int additionsLeft = Math.min(MAX_ADDITIONS_PER_PASS, LEVEL_CAP - total);
        for (int index = 0; index < players.size(); index++) {
            var player = players.get((start + index) % players.size());
            if (!player.isAlive() || player.isSpectator()) continue;
            int groups = Math.round(Mth.lerp(depth(player.blockPosition().getY()),
                    SHALLOW_GROUP_ATTEMPTS, DEEP_GROUP_ATTEMPTS));
            for (int group = 0; group < groups; group++) {
                double angle = level.getRandom().nextDouble() * Math.PI * 2;
                double radius = 12 + level.getRandom().nextDouble() * 12;
                BlockPos center = BlockPos.containing(player.getX() + Math.cos(angle) * radius,
                        player.getY(), player.getZ() + Math.sin(angle) * radius);
                // Never load new chunks for these attempts, including group spread.
                if (!level.hasChunksAt(center.offset(-5, -4, -5), center.offset(5, 4, 5))) continue;
                int cap = Math.round(Mth.lerp(depth(center.getY()), SHALLOW_LOCAL_CAP, DEEP_LOCAL_CAP));
                int nearby = level.getEntitiesOfClass(Mob.class, new AABB(center).inflate(32, 12, 32),
                        mob -> mob.isAlive() && bug(mob)).size();
                int room = Math.min(cap - nearby, additionsLeft);
                if (room <= 0) continue;
                int roll = level.getRandom().nextInt(5);
                EntityType<? extends Mob> type = roll == 0 ? ModEntities.ROACH.get()
                        : roll <= 2 ? ModEntities.BABY_ROACH.get() : Earthworms.TYPE;
                int size = type == Earthworms.TYPE ? 1 + level.getRandom().nextInt(4)
                        : type == ModEntities.BABY_ROACH.get() ? 4 + level.getRandom().nextInt(5)
                        : 2 + level.getRandom().nextInt(3);
                for (int member = 0; member < Math.min(size, room); member++) {
                    if (attemptsLeft < 4 || additionsLeft <= 0) return;
                    attemptsLeft -= 4;
                    var spawned = SpawnUtil.trySpawnMob(type, EntitySpawnReason.EVENT, level,
                            center, 4, 3, 3,
                            (world, floor, floorState, pos, state) -> allowed(world, pos)
                                    && SpawnUtil.Strategy.ON_TOP_OF_COLLIDER_NO_LEAVES
                                            .canSpawnOn(world, floor, floorState, pos, state), true);
                    if (spawned.isPresent()) additionsLeft--;
                }
            }
        }
    }
}
