package net.beamex.shamaschizm.entity.custom;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.beamex.shamaschizm.entity.CoffinRegistration;
import net.beamex.shamaschizm.world.Schizm;
import net.beamex.shamaschizm.world.dungeon.GardenZoneEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Skeleton variant plus the special coffin awakening sequence. */
public final class SkeletonVariantEntity extends Skeleton {
    public static final String EQUIPPED_TAG = "ShamaschizmSkeletonVariantEquipped";
    private static final int AWAKENING_TICKS = 80;

    private static final EntityDataAccessor<Boolean> COFFIN_SUMMONING =
            SynchedEntityData.defineId(SkeletonVariantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> COFFIN_TICKS =
            SynchedEntityData.defineId(SkeletonVariantEntity.class, EntityDataSerializers.INT);

    private Direction coffinFacing = Direction.NORTH;

    public SkeletonVariantEntity(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        super.defineSynchedData(data);
        data.define(COFFIN_SUMMONING, false);
        data.define(COFFIN_TICKS, 0);
    }

    public static boolean canSpawn(EntityType<SkeletonVariantEntity> type,
                                   ServerLevelAccessor level,
                                   EntitySpawnReason reason,
                                   BlockPos pos,
                                   RandomSource random) {
        return level.getLevel().dimension().equals(Schizm.KEY)
                && !GardenZoneEntity.contains(level.getLevel(), pos)
                && Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }

    public void waitInsideCoffin(Direction facing) {
        this.coffinFacing = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
        this.entityData.set(COFFIN_TICKS, 0);
        this.entityData.set(COFFIN_SUMMONING, false);
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setPose(Pose.SLEEPING);
        this.setAggressive(false);
        this.setDeltaMovement(Vec3.ZERO);
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        applyCoffinSleepingFacing();
    }

    public void beginCoffinAwakening(Direction facing) {
        this.coffinFacing = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
        this.entityData.set(COFFIN_TICKS, AWAKENING_TICKS);
        this.setPose(Pose.STANDING);
        this.clearSleepingPos();
        this.setNoAi(true);
        this.setNoGravity(false);
        this.setDeltaMovement(Vec3.ZERO);
        applyCoffinFacing();
        beginSummoning((ServerLevel)this.level());
    }

    public boolean isCoffinSummoning() {
        return this.entityData.get(COFFIN_SUMMONING);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        int remaining = this.entityData.get(COFFIN_TICKS);
        if (remaining <= 0) return;

        this.setDeltaMovement(Vec3.ZERO);
        remaining--;
        this.entityData.set(COFFIN_TICKS, remaining);
        if (remaining == 0) finishSummoning((ServerLevel)this.level());
    }

    private void beginSummoning(ServerLevel level) {
        this.setPose(Pose.STANDING);
        this.setNoGravity(false);
        this.entityData.set(COFFIN_SUMMONING, true);
        this.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.25F, 0.9F);

        SpellVisualEntity spell = CoffinRegistration.SPELL.create(level, EntitySpawnReason.EVENT);
        if (spell != null) {
            spell.bindTo(this);
            level.addFreshEntity(spell);
        }
    }

    private void applyCoffinFacing() {
        float yaw = switch (this.coffinFacing) {
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            case EAST -> -90.0F;
            default -> 180.0F;
        };
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.yBodyRot = yaw;
    }

    /** Sleeping render rotation uses a different convention from normal mob yaw. */
    private void applyCoffinSleepingFacing() {
        float yaw = switch (this.coffinFacing) {
            case SOUTH -> 90.0F;
            case WEST -> 0.0F;
            case NORTH -> 270.0F;
            case EAST -> 180.0F;
            default -> 270.0F;
        };
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.yBodyRot = yaw;
    }

    private void finishSummoning(ServerLevel level) {
        this.entityData.set(COFFIN_SUMMONING, false);
        this.setPose(Pose.STANDING);
        this.setNoAi(false);
        this.setNoGravity(false);
        this.setItemSlot(EquipmentSlot.MAINHAND, coffinBow(level));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
        this.reassessWeaponGoal();
        this.playSound(SoundEvents.WITHER_SPAWN, 1.5F, 1.0F);
        spawnWitherSkeletons(level);
    }

    private ItemStack coffinBow(ServerLevel level) {
        ItemStack bow = new ItemStack(Items.BOW);
        HolderLookup.RegistryLookup<Enchantment> registry = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);
        bow.enchant(registry.getOrThrow(Enchantments.FLAME), 1);
        bow.enchant(registry.getOrThrow(Enchantments.POWER), 5);
        return bow;
    }

    private void spawnWitherSkeletons(ServerLevel level) {
        List<BlockPos> candidates = new ArrayList<>();
        BlockPos origin = this.blockPosition();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (dx == 0 && dz == 0) continue;
                candidates.add(origin.offset(dx, 0, dz));
            }
        }
        candidates.sort(Comparator.comparingDouble(pos -> pos.distSqr(origin)));

        int spawned = 0;
        for (BlockPos pos : candidates) {
            if (spawned >= 2) break;
            BlockPos floor = pos.below();
            if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) continue;

            // Snow layers, carpet, and similarly thin blocks occupy the nominal spawn block.
            // Place the feet on their collision surface instead of rejecting the location.
            BlockState feetState = level.getBlockState(pos);
            VoxelShape feetShape = feetState.getCollisionShape(level, pos);
            double surfaceHeight = feetShape.isEmpty() ? 0.0D : feetShape.max(Direction.Axis.Y);
            if (surfaceHeight >= 1.0D) continue;
            double spawnY = pos.getY() + surfaceHeight;
            if (!level.noCollision(EntityTypes.WITHER_SKELETON.getSpawnAABB(
                    pos.getX() + 0.5D, spawnY, pos.getZ() + 0.5D))) continue;

            WitherSkeleton wither = EntityTypes.WITHER_SKELETON.create(level, EntitySpawnReason.EVENT);
            if (wither == null) continue;
            wither.snapTo(pos.getX() + 0.5D, spawnY, pos.getZ() + 0.5D,
                    this.random.nextFloat() * 360.0F, 0.0F);
            wither.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
            wither.setPersistenceRequired();
            level.addFreshEntity(wither);
            spawned++;
        }
    }

    @Override public int getMaxSpawnClusterSize() { return 1; }
    @Override public boolean isMaxGroupSizeReached(int size) { return size >= 1; }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("CoffinAwakeningTicks", this.entityData.get(COFFIN_TICKS));
        output.putBoolean("CoffinSummoning", this.isCoffinSummoning());
        output.putInt("CoffinFacing", this.coffinFacing.get2DDataValue());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        int ticks = input.getIntOr("CoffinAwakeningTicks", 0);
        this.entityData.set(COFFIN_TICKS, ticks);
        this.entityData.set(COFFIN_SUMMONING, input.getBooleanOr("CoffinSummoning", false));
        this.coffinFacing = Direction.from2DDataValue(
                input.getIntOr("CoffinFacing", Direction.NORTH.get2DDataValue()));
        if (ticks > 0) {
            this.setNoAi(true);
            this.setNoGravity(false);
            this.setPose(Pose.STANDING);
            applyCoffinFacing();
        } else if (this.getPose() == Pose.SLEEPING) {
            applyCoffinSleepingFacing();
        }
    }
}
