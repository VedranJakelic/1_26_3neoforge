package net.beamex.shamaschizm.event;

import java.util.List;
import java.util.Optional;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.ModEntities;
import net.beamex.shamaschizm.registry.ModDataComponents;
import net.beamex.shamaschizm.world.Schizm;
import net.beamex.shamaschizm.world.ai.SchizmShieldFrontlinerGoal;
import net.beamex.shamaschizm.world.dungeon.GardenZoneEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Schizm-specific natural spawn tables, depth density, and equipment. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class SchizmSpawnEvents {
    private static final int DEPTH_START_Y = 160;
    private static final int WITHER_SKELETON_MAX_Y = 140;
    private static final int ANCIENT_ZOMBIE_MAX_Y = 150;
    private static final float ANCIENT_ZOMBIE_CHANCE = 0.25F;
    private static final String EQUIPMENT_DEPTH_TAG = "ShamaschizmEquipmentDepth";
    private static final String ANCIENT_LOADOUT_TAG = "ShamaschizmAncientLoadout";
    public static final String SHIELD_FRONTLINER_TAG = "ShamaschizmShieldFrontliner";
    private static final int SPEAR_LOADOUT = 1;
    private static final int SHIELD_LOADOUT = 2;

    private SchizmSpawnEvents() {}

    /**
     * Replaces plains spawn lists so old and new Schizm chunks behave identically.
     * Group sizes stop at four because that is vanilla's hard cluster limit for
     * these monsters; deeper entries therefore request a full four-mob cluster.
     */
    @SubscribeEvent
    public static void potentialSpawns(LevelEvent.PotentialSpawns event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(Schizm.KEY)) return;

        if (GardenZoneEntity.contains(level, event.getPos())) {
            event.setCanceled(true);
            return;
        }

        if (event.getMobCategory() == MobCategory.AMBIENT) {
            clear(event);
            // Scale continuously from the upper Schizm at Y=160 to maximum
            // density at Y=0 and below. Both selection weight and pack size
            // increase, so depth affects actual roach numbers rather than only
            // their share relative to regular centipedes.
            float roachDepth = Mth.clamp((210.0F - event.getPos().getY()) / 160.0F, 0.0F, 1.0F);
            int adultWeight = Math.round(Mth.lerp(roachDepth, 24.0F, 100.0F));
            int babyWeight = Math.round(Mth.lerp(roachDepth, 20.0F, 90.0F));
            int adultMin = 2 + Math.round(roachDepth);
            int adultMax = 4 + Math.round(roachDepth * 2.0F);
            int babyMin = 2 + Math.round(roachDepth * 3.0F);
            int babyMax = 7 + Math.round(roachDepth * 4.0F);
            add(event, ModEntities.ROACH.get(), adultWeight, adultMin, adultMax);
            add(event, ModEntities.BABY_ROACH.get(), babyWeight, babyMin, babyMax);
            return;
        }
        if (event.getMobCategory() != MobCategory.MONSTER) return;

        clear(event);
        int minGroup = event.getPos().getY() < 80 ? 4 : 3;
        add(event, EntityTypes.ZOMBIE, 70, minGroup, 4);
        add(event, EntityTypes.SKELETON, 70, minGroup, 4);
        add(event, EntityTypes.SPIDER, 38, minGroup, 4);
        add(event, EntityTypes.HUSK, 15, minGroup, 4);
        // One chain per spawn roll; GiantCentipedeEntity enforces the separate
        // limit of at most two living chains in each 100 x 100 area.
        add(event, ModEntities.GIANT_CENTIPEDE.get(), 8, 1, 1);
        // Creepers remain deliberately uncommon and form smaller clusters.
        add(event, EntityTypes.CREEPER, 6, 2, 3);
        if (event.getPos().getY() < WITHER_SKELETON_MAX_Y) {
            add(event, EntityTypes.WITHER_SKELETON, 20, minGroup, 4);
        }
        // Witches and endermen are intentionally absent, preserving the
        // existing Schizm rule for those two types.
    }

    /**
     * Uses a depth-scaled local density ceiling instead of rejecting each member
     * randomly. This yields fewer monsters deep down without breaking clusters
     * into isolated single mobs.
     */
    @SubscribeEvent
    public static void checkNaturalSpawn(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getSpawnType() != EntitySpawnReason.NATURAL
                || event.getEntityType().getCategory() != MobCategory.MONSTER) return;
        ServerLevel level = event.getLevel().getLevel();
        if (!level.dimension().equals(Schizm.KEY)) return;

        BlockPos pos = event.getPos();
        if (GardenZoneEntity.contains(level, pos)) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
            return;
        }

        int cap = Math.round(Mth.lerp(depth(level, pos.getY()), 12.0F, 4.0F));
        AABB neighborhood = new AABB(pos).inflate(32.0D, 16.0D, 32.0D);
        int nearby = level.getEntitiesOfClass(Monster.class, neighborhood,
                monster -> monster.isAlive()).size();
        if (nearby >= cap) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }

    /** Blocks natural/spawner-style mob creation anywhere inside garden1. */
    @SubscribeEvent
    public static void finalizeSpawn(FinalizeSpawnEvent event) {
        ServerLevel level = event.getLevel().getLevel();
        if (!level.dimension().equals(Schizm.KEY)) return;

        if (GardenZoneEntity.contains(level, event.getEntity().blockPosition())
                && blockedGardenReason(event.getSpawnType())) {
            event.setSpawnCancelled(true);
            return;
        }

        if (event.getSpawnType() == EntitySpawnReason.NATURAL
                && event.getEntity() instanceof Monster
                && canReceiveDepthEquipment(event.getEntity())) {
            if (event.getEntity() instanceof Zombie zombie
                    && zombie.blockPosition().getY() < ANCIENT_ZOMBIE_MAX_Y
                    && zombie.getRandom().nextFloat() < ANCIENT_ZOMBIE_CHANCE) {
                zombie.getPersistentData().putInt(
                        ANCIENT_LOADOUT_TAG,
                        zombie.getRandom().nextBoolean() ? SPEAR_LOADOUT : SHIELD_LOADOUT);
                return;
            }

            float depth = depth(level, event.getEntity().blockPosition().getY());
            if (depth > 0.0F) {
                // EntityJoinLevelEvent runs after vanilla finalizeSpawn, allowing
                // this equipment to augment/replace vanilla's finalized loadout.
                event.getEntity().getPersistentData().putFloat(EQUIPMENT_DEPTH_TAG, depth);
            }
        }
    }

    @SubscribeEvent
    public static void equipAfterFinalization(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(Schizm.KEY)
                || !(event.getEntity() instanceof Mob mob)) return;

        if (mob instanceof Zombie zombie && mob.getPersistentData().contains(ANCIENT_LOADOUT_TAG)) {
            int loadout = mob.getPersistentData().getIntOr(ANCIENT_LOADOUT_TAG, SPEAR_LOADOUT);
            mob.getPersistentData().remove(ANCIENT_LOADOUT_TAG);
            equipAncientZombie(zombie, loadout == SHIELD_LOADOUT);
        } else if (mob.getPersistentData().contains(EQUIPMENT_DEPTH_TAG)) {
            float depth = mob.getPersistentData().getFloatOr(EQUIPMENT_DEPTH_TAG, 0.0F);
            mob.getPersistentData().remove(EQUIPMENT_DEPTH_TAG);
            equipForDepth(level, mob, Mth.clamp(depth, 0.0F, 1.0F));
        }

        if (mob instanceof Zombie zombie
                && mob.getPersistentData().getIntOr(SHIELD_FRONTLINER_TAG, 0) == 1) {
            configureShieldFrontliner(zombie);
        }
    }

    private static void equipAncientZombie(Zombie zombie, boolean shieldFrontliner) {
        RandomSource random = zombie.getRandom();
        List<EquipmentSlot> availableSlots = new java.util.ArrayList<>(List.of(
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET));

        // Uniformly choose two, three, or all four distinct armor slots.
        int pieces = 2 + random.nextInt(3);
        for (int i = 0; i < pieces; i++) {
            int selected = random.nextInt(availableSlots.size());
            EquipmentSlot slot = availableSlots.remove(selected);
            zombie.setItemSlot(slot, ancientArmorPiece(slot, random));
        }

        if (shieldFrontliner) {
            zombie.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            zombie.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            zombie.getPersistentData().putInt(SHIELD_FRONTLINER_TAG, 1);
        } else {
            zombie.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.COPPER_SPEAR));
            zombie.getPersistentData().remove(SHIELD_FRONTLINER_TAG);
        }
    }

    private static ItemStack ancientArmorPiece(EquipmentSlot slot, RandomSource random) {
        ItemStack stack = new ItemStack(switch (slot) {
            case HEAD -> Items.NETHERITE_HELMET;
            case CHEST -> Items.NETHERITE_CHESTPLATE;
            case LEGS -> Items.NETHERITE_LEGGINGS;
            case FEET -> Items.NETHERITE_BOOTS;
            default -> throw new IllegalArgumentException("Not an armor slot: " + slot);
        });
        stack.set(ModDataComponents.RAISED_ENCHANT_CAP, true);
        stack.set(ModDataComponents.ANCIENT_APPEARANCE, true);

        // Every mob-spawned ancient armor piece has only 10–20 uses remaining.
        int remaining = 10 + random.nextInt(11);
        stack.setDamageValue(stack.getMaxDamage() - remaining);
        return stack;
    }

    private static void configureShieldFrontliner(Zombie zombie) {
        boolean alreadyConfigured = zombie.getGoalSelector().getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof SchizmShieldFrontlinerGoal);
        if (!alreadyConfigured) {
            // Priority 0 wins movement/look control over normal attack and door-breaking goals.
            zombie.getGoalSelector().addGoal(0, new SchizmShieldFrontlinerGoal(zombie));
        }
        zombie.addEffect(new MobEffectInstance(MobEffects.SPEED, -1, 0, false, false));
    }

    private static void equipForDepth(ServerLevel level, Mob mob, float depth) {
        RandomSource random = mob.getRandom();
        float armorChance = Mth.lerp(depth, 0.20F, 0.75F);
        float weaponChance = Mth.lerp(depth, 0.15F, 0.65F);

        if (random.nextFloat() < armorChance) {
            boolean iron = random.nextFloat() < Mth.lerp(depth, 0.35F, 0.90F);
            float pieceChance = Mth.lerp(depth, 0.50F, 0.90F);
            boolean equipped = false;
            for (EquipmentSlot slot : List.of(
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
                if (mob.getItemBySlot(slot).isEmpty() && random.nextFloat() < pieceChance) {
                    mob.setItemSlot(slot, new ItemStack(armorPiece(iron, slot)));
                    equipped = true;
                }
            }
            if (!equipped && mob.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
                mob.setItemSlot(EquipmentSlot.CHEST,
                        new ItemStack(iron ? Items.IRON_CHESTPLATE : Items.CHAINMAIL_CHESTPLATE));
            }
        }

        if (random.nextFloat() >= weaponChance) return;
        if (mob instanceof AbstractSkeleton skeleton && !(mob instanceof WitherSkeleton)) {
            ItemStack bow = EnchantmentHelper.enchantItem(
                    random, new ItemStack(Items.BOW), 12 + Math.round(depth * 18.0F),
                    level.registryAccess(), Optional.empty());
            skeleton.setItemSlot(EquipmentSlot.MAINHAND, bow);
            skeleton.reassessWeaponGoal();
        } else if (mob instanceof Zombie || mob instanceof WitherSkeleton) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            if (mob instanceof AbstractSkeleton skeleton) skeleton.reassessWeaponGoal();
        }
    }

    private static boolean canReceiveDepthEquipment(Mob mob) {
        return mob instanceof Zombie || mob instanceof AbstractSkeleton;
    }

    private static Item armorPiece(boolean iron, EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> iron ? Items.IRON_HELMET : Items.CHAINMAIL_HELMET;
            case CHEST -> iron ? Items.IRON_CHESTPLATE : Items.CHAINMAIL_CHESTPLATE;
            case LEGS -> iron ? Items.IRON_LEGGINGS : Items.CHAINMAIL_LEGGINGS;
            case FEET -> iron ? Items.IRON_BOOTS : Items.CHAINMAIL_BOOTS;
            default -> throw new IllegalArgumentException("Not an armor slot: " + slot);
        };
    }

    private static float depth(ServerLevel level, int y) {
        if (y >= DEPTH_START_Y) return 0.0F;
        return Mth.clamp((DEPTH_START_Y - y) / (float)(DEPTH_START_Y - level.getMinY()), 0.0F, 1.0F);
    }

    private static boolean blockedGardenReason(EntitySpawnReason reason) {
        return switch (reason) {
            case NATURAL, CHUNK_GENERATION, SPAWNER, TRIAL_SPAWNER,
                 PATROL, REINFORCEMENT, EVENT -> true;
            default -> false;
        };
    }

    private static void clear(LevelEvent.PotentialSpawns event) {
        for (Weighted<MobSpawnSettings.SpawnerData> existing
                : List.copyOf(event.getSpawnerDataList())) {
            event.removeSpawnerData(existing);
        }
    }

    private static void add(LevelEvent.PotentialSpawns event,
                            net.minecraft.world.entity.EntityType<? extends Mob> type,
                            int weight, int min, int max) {
        event.addSpawnerData(new Weighted<>(new MobSpawnSettings.SpawnerData(type, net.minecraft.util.valueproviders.UniformInt.of(min, max)), weight));
    }
}
