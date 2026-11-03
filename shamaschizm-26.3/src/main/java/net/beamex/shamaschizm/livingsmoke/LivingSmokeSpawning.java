package net.beamex.shamaschizm.livingsmoke;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.Schizm;
import net.beamex.shamaschizm.world.dungeon.GardenZoneEntity;
import net.beamex.shamaschizm.saints.audio.ArenaZones;
import net.beamex.shamaschizm.experimental.BossGardenLocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Deep Schizm-only, rare, solitary, darkness-weighted spawn attempts. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class LivingSmokeSpawning {
    private static final int GLOBAL_CAP = 8;
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel server)
                || !server.dimension().equals(Schizm.KEY)
                || server.getGameTime() % 140 != 0
                || server.getDifficulty() == Difficulty.PEACEFUL
                || !server.getGameRules().get(GameRules.SPAWN_MOBS)) return;
        int living = 0;
        for (var entity : server.getAllEntities()) {
            if (entity instanceof LivingSmoke smoke && smoke.isAlive() && ++living >= GLOBAL_CAP) return;
        }
        for (var player : server.players()) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator() || player.getY() > 125) continue;
            double depth = Math.clamp((130 - player.getY()) / 130, 0, 1);
            if (server.getRandom().nextDouble() > .035 + .14 * depth) continue;
            for (int attempt = 0; attempt < 12; attempt++) {
                double angle = server.getRandom().nextDouble() * Math.PI * 2;
                double radius = 19 + server.getRandom().nextDouble() * 15;
                Vec3 test = player.position().add(Math.cos(angle)*radius,
                        (server.getRandom().nextDouble()-.5)*10+1,
                        Math.sin(angle)*radius);
                BlockPos pos = BlockPos.containing(test);
                if (!server.hasChunksAt(pos.offset(-2,-2,-2),pos.offset(2,2,2))
                        || !server.getWorldBorder().isWithinBounds(pos)
                        || server.getMaxLocalRawBrightness(pos) > 5
                        || GardenZoneEntity.contains(server,pos)
                        || ArenaZones.get(server).arena(pos)
                        || BossGardenLocks.get(server).insideArena(pos)) continue;
                if (!server.getEntitiesOfClass(LivingSmoke.class, new AABB(pos).inflate(24)).isEmpty()) continue;
                if (server.players().stream().anyMatch(p -> p.isAlive() && !p.isSpectator()
                        && p.distanceToSqr(test) < 16*16)) continue;
                var mob = LivingSmokeRegistration.TYPE.create(server, EntitySpawnReason.NATURAL);
                if (mob == null) return;
                mob.snapTo(test.x,test.y,test.z,server.getRandom().nextFloat()*360,0);
                if (!server.noCollision(mob,mob.getBoundingBox().inflate(.15))
                        || server.containsAnyLiquid(mob.getBoundingBox())) continue;
                if (server.addFreshEntity(mob)) return;
            }
        }
    }
}
