package net.beamex.shamaschizm.wisdom;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.Schizm;
import net.beamex.shamaschizm.world.dungeon.GardenZoneEntity;
import net.beamex.shamaschizm.saints.audio.ArenaZones;
import net.beamex.shamaschizm.experimental.BossGardenLocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Bounded solo attempts: deeper and spacious rooms are favored, never required for wandering. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class WisdomSpawning {
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Schizm.KEY)
                || level.getGameTime() % 100 != 0 || level.getDifficulty() == Difficulty.PEACEFUL
                || !level.getGameRules().get(GameRules.SPAWN_MOBS)) return;
        int count = 0;
        for (var entity : level.getAllEntities()) if (entity instanceof TaintedWisdom && entity.isAlive() && ++count >= 12) return;
        int attempts = 0;
        for (var player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            double depth = Math.clamp((160 - player.getY()) / 100, 0, 1);
            if (level.getRandom().nextDouble() > .035 + .20 * depth) continue;
            for (int attempt = 0; attempt < 4 && attempts++ < 16; attempt++) {
                double angle = level.getRandom().nextDouble() * Math.PI * 2, radius = 24 + level.getRandom().nextDouble() * 16;
                Vec3 probe = player.position().add(Math.cos(angle) * radius, 4, Math.sin(angle) * radius);
                BlockPos block = BlockPos.containing(probe);
                if (!level.hasChunksAt(block.offset(-10, -20, -10), block.offset(10, 8, 10))) continue;
                var floor = level.clip(new ClipContext(probe, probe.add(0, -20, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (floor.getType() != HitResult.Type.BLOCK) continue;
                Vec3 spawn = floor.getLocation().add(0, 2.5 + level.getRandom().nextDouble(), 0);
                BlockPos pos = BlockPos.containing(spawn);
                if (!level.getWorldBorder().isWithinBounds(pos) || level.getMaxLocalRawBrightness(pos) > 7
                        || GardenZoneEntity.contains(level, pos) || ArenaZones.get(level).arena(pos)
                        || BossGardenLocks.get(level).insideArena(pos)) continue;
                if (level.players().stream().anyMatch(p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(spawn) < 24 * 24)) continue;
                if (!level.getEntitiesOfClass(TaintedWisdom.class, new AABB(pos).inflate(24)).isEmpty()) continue;
                double openness = 0;
                for (Vec3 axis : new Vec3[]{new Vec3(1,0,0), new Vec3(-1,0,0), new Vec3(0,0,1), new Vec3(0,0,-1)}) {
                    var ray = level.clip(new ClipContext(spawn, spawn.add(axis.scale(8)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                    openness += spawn.distanceTo(ray.getLocation());
                }
                if (openness < 20 && level.getRandom().nextFloat() > .12F) continue;
                var mob = WisdomRegistration.TYPE.create(level, EntitySpawnReason.NATURAL);
                if (mob == null) return;
                mob.snapTo(spawn.x, spawn.y, spawn.z, level.getRandom().nextFloat() * 360, 0);
                if (!level.noCollision(mob, mob.getBoundingBox().inflate(.15)) || level.containsAnyLiquid(mob.getBoundingBox())) continue;
                if (level.addFreshEntity(mob)) { if (++count >= 12) return; break; }
            }
            if (attempts >= 16) return;
        }
    }
}
