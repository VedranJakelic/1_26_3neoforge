package net.beamex.shamaschizm.earthworm;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.Schizm;
import net.beamex.shamaschizm.world.dungeon.GardenZoneEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class EarthwormSpawns {
    private EarthwormSpawns() {}

    @SubscribeEvent
    public static void placements(RegisterSpawnPlacementsEvent event) {
        event.register(Earthworms.TYPE, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) ->
                        level.getLevel().dimension().equals(Schizm.KEY)
                        && !GardenZoneEntity.contains(level.getLevel(), pos)
                        && level.getMaxLocalRawBrightness(pos) <= 7
                        && Mob.checkMobSpawnRules(type, level, reason, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    // Run after SchizmSpawnEvents replaces the ambient list. Do not receive
    // canceled events: garden and other protected-room exclusions still win.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void potentialSpawns(LevelEvent.PotentialSpawns event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(Schizm.KEY)
                || event.getMobCategory() != MobCategory.AMBIENT
                || GardenZoneEntity.contains(level, event.getPos())) return;
        float depth = Mth.clamp((210F - event.getPos().getY()) / 160F, 0F, 1F);
        int weight = Math.round(Mth.lerp(depth, 24F, 90F));
        event.addSpawnerData(new Weighted<>(
                new MobSpawnSettings.SpawnerData(Earthworms.TYPE, UniformInt.of(1, 4)), weight));
    }
}
