package net.beamex.shamaschizm.event;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.SkeletonVariantRegistration;
import net.beamex.shamaschizm.entity.custom.SkeletonVariantEntity;
import net.beamex.shamaschizm.world.Schizm;
import net.beamex.shamaschizm.world.dungeon.GardenZoneEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SpawnUtil;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Solitary and mixed-group spawning plus the guaranteed enchanted bow. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class SkeletonVariantSpawnEvents {
    public static final int DEEPER_Y = 140;
    private static final int SOLO_WEIGHT = 4;
    private static final int DEEP_SOLO_WEIGHT = 8;
    private static final float MIXED_ROLL = 0.12F;
    private static final float DEEP_MIXED_ROLL = 0.22F;

    private SkeletonVariantSpawnEvents() {}

    @SubscribeEvent
    public static void registerSpawnPlacement(RegisterSpawnPlacementsEvent event) {
        event.register(
                SkeletonVariantRegistration.TYPE,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SkeletonVariantEntity::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** Runs after both existing Schizm spawn-list handlers have finished clearing/rebuilding. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void addSolitarySpawn(LevelEvent.PotentialSpawns event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(Schizm.KEY)
                || event.getMobCategory() != MobCategory.MONSTER
                || GardenZoneEntity.contains(level, event.getPos())) return;

        boolean alreadyPresent = event.getSpawnerDataList().stream()
                .anyMatch(entry -> entry.value().type() == SkeletonVariantRegistration.TYPE);
        if (!alreadyPresent) {
            int weight = event.getPos().getY() < DEEPER_Y ? DEEP_SOLO_WEIGHT : SOLO_WEIGHT;
            event.addSpawnerData(new Weighted<>(new MobSpawnSettings.SpawnerData(
                    SkeletonVariantRegistration.TYPE, net.minecraft.util.valueproviders.ConstantInt.of(1)), weight));
        }
    }

    /**
     * Each member of another natural hostile pack may roll until one companion
     * succeeds. The nearby check ensures a pack never gains two variants.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void addMixedCompanion(FinalizeSpawnEvent event) {
        ServerLevel level = event.getLevel().getLevel();
        Mob source = event.getEntity();
        if (event.isSpawnCancelled()
                || event.getSpawnType() != EntitySpawnReason.NATURAL
                || !level.dimension().equals(Schizm.KEY)
                || !(source instanceof Monster)
                || source.getType() == SkeletonVariantRegistration.TYPE
                || GardenZoneEntity.contains(level, source.blockPosition())) return;

        AABB groupArea = source.getBoundingBox().inflate(14.0D, 6.0D, 14.0D);
        if (!level.getEntitiesOfClass(SkeletonVariantEntity.class, groupArea).isEmpty()) return;

        float chance = source.getY() < DEEPER_Y ? DEEP_MIXED_ROLL : MIXED_ROLL;
        if (source.getRandom().nextFloat() >= chance) return;

        SpawnUtil.trySpawnMob(
                SkeletonVariantRegistration.TYPE,
                EntitySpawnReason.EVENT,
                level,
                source.blockPosition(),
                8,
                5,
                3,
                SpawnUtil.Strategy.ON_TOP_OF_COLLIDER_NO_LEAVES,
                true);
    }

    /** Runs after existing depth-equipment handlers, so this bow always wins. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void equipBow(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof SkeletonVariantEntity skeleton)
                || skeleton.getPersistentData().getBooleanOr(
                        SkeletonVariantEntity.EQUIPPED_TAG, false)) return;

        ItemStack bow = enchantedBow(level, skeleton.getRandom());
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, bow);
        skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
        skeleton.getPersistentData().putBoolean(SkeletonVariantEntity.EQUIPPED_TAG, true);
        skeleton.reassessWeaponGoal();
    }

    private static ItemStack enchantedBow(ServerLevel level, RandomSource random) {
        ItemStack bow = new ItemStack(Items.BOW);
        HolderLookup.RegistryLookup<Enchantment> enchantments = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);
        bow.enchant(enchantments.getOrThrow(Enchantments.FLAME), 1);

        switch (random.nextInt(3)) {
            case 0 -> bow.enchant(enchantments.getOrThrow(Enchantments.POWER), 5);
            case 1 -> bow.enchant(enchantments.getOrThrow(Enchantments.PUNCH), 2);
            default -> bow.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), 3);
        }
        return bow;
    }
}
