package net.beamex.shamaschizm.event;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.ModEntities;
import net.beamex.shamaschizm.entity.custom.RoachEntity;
import net.beamex.shamaschizm.entity.custom.BabyRoachEntity;
import net.beamex.shamaschizm.entity.custom.GiantCentipedeEntity;
import net.beamex.shamaschizm.entity.custom.RegularCentipedeEntity;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class RoachSpawnEvents {
    private static final float MIXED_FAMILY_CHANCE_PER_GROUP = 0.15F;

    private RoachSpawnEvents() {
    }

    @SubscribeEvent
    public static void registerSpawnPlacement(RegisterSpawnPlacementsEvent event) {
        event.register(
                ModEntities.ROACH.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                RoachEntity::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                ModEntities.BABY_ROACH.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BabyRoachEntity::canBabySpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                ModEntities.GIANT_CENTIPEDE.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                GiantCentipedeEntity::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                ModEntities.REGULAR_CENTIPEDE.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                RegularCentipedeEntity::canRegularCentipedeSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** Marks some newly finalized adults; their first safe server tick creates the family. */
    @SubscribeEvent
    public static void markMixedFamily(FinalizeSpawnEvent event) {
        ServerLevel level = event.getLevel().getLevel();
        if (event.getSpawnType() != EntitySpawnReason.NATURAL
                || !level.dimension().equals(Schizm.KEY)
                || event.getEntity().getType() != ModEntities.ROACH.get()) {
            return;
        }

        // Natural group members are finalized one at a time. Only the first adult,
        // before another adult from its cluster is already present nearby, may roll.
        boolean adultAlreadyNearby = !level.getEntitiesOfClass(
                RoachEntity.class,
                event.getEntity().getBoundingBox().inflate(8.0D),
                candidate -> candidate.getType() == ModEntities.ROACH.get()).isEmpty();
        if (adultAlreadyNearby
                || event.getEntity().getRandom().nextFloat() >= MIXED_FAMILY_CHANCE_PER_GROUP) {
            return;
        }

        int familySize = 6 + event.getEntity().getRandom().nextInt(5);
        event.getEntity().getPersistentData().putInt(
                BabyRoachEntity.PENDING_FAMILY_SIZE_TAG, familySize);
    }
}
