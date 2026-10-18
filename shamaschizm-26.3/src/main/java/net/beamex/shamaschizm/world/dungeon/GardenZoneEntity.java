package net.beamex.shamaschizm.world.dungeon;

import net.beamex.shamaschizm.garden.GardenData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Persistent, invisible description of the exact generated garden room volume. */
public final class GardenZoneEntity extends Entity {
    private static final EntityDataAccessor<Integer> SIZE_X =
            SynchedEntityData.defineId(GardenZoneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SIZE_Y =
            SynchedEntityData.defineId(GardenZoneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SIZE_Z =
            SynchedEntityData.defineId(GardenZoneEntity.class, EntityDataSerializers.INT);
    private boolean recordedInSavedData;

    public GardenZoneEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setPermanentlyInvulnerable(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        data.define(SIZE_X, 1);
        data.define(SIZE_Y, 1);
        data.define(SIZE_Z, 1);
    }

    public void configure(BoundingBox box) {
        int sizeX = box.maxX() - box.minX() + 1;
        int sizeY = box.maxY() - box.minY() + 1;
        int sizeZ = box.maxZ() - box.minZ() + 1;
        this.entityData.set(SIZE_X, sizeX);
        this.entityData.set(SIZE_Y, sizeY);
        this.entityData.set(SIZE_Z, sizeZ);
        this.snapTo(box.minX() + sizeX / 2.0D, box.minY() + sizeY / 2.0D,
                box.minZ() + sizeZ / 2.0D);
    }

    /** Block-edge-aligned AABB of the structure template, including its full height. */
    public AABB zoneBounds() {
        double halfX = this.entityData.get(SIZE_X) / 2.0D;
        double halfY = this.entityData.get(SIZE_Y) / 2.0D;
        double halfZ = this.entityData.get(SIZE_Z) / 2.0D;
        return new AABB(this.getX() - halfX, this.getY() - halfY, this.getZ() - halfZ,
                this.getX() + halfX, this.getY() + halfY, this.getZ() + halfZ);
    }

    /** Integer template bounds used by the persistent per-dimension garden index. */
    public BoundingBox structureBox() {
        int sizeX = this.entityData.get(SIZE_X);
        int sizeY = this.entityData.get(SIZE_Y);
        int sizeZ = this.entityData.get(SIZE_Z);
        int minX = Mth.floor(this.getX() - sizeX / 2.0D);
        int minY = Mth.floor(this.getY() - sizeY / 2.0D);
        int minZ = Mth.floor(this.getZ() - sizeZ / 2.0D);
        return new BoundingBox(minX, minY, minZ,
                minX + sizeX - 1, minY + sizeY - 1, minZ + sizeZ - 1);
    }

    /** True when the center of a block lies inside a generated garden1 room. */
    public static boolean contains(ServerLevel level, BlockPos pos) {
        Vec3 point = Vec3.atCenterOf(pos);
        for (GardenZoneEntity zone : level.getEntitiesOfClass(
                GardenZoneEntity.class, new AABB(pos).inflate(96.0D))) {
            if (zone.zoneBounds().contains(point)) return true;
        }
        return false;
    }

    @Override
    public void tick() {
        // This also migrates garden zones created before GardenData was wired
        // into dungeon generation as soon as their chunk is loaded once.
        if (!this.level().isClientSide() && !this.recordedInSavedData
                && this.level() instanceof ServerLevel serverLevel) {
            GardenData.get(serverLevel).add(this.structureBox());
            this.recordedInSavedData = true;
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("SizeX", this.entityData.get(SIZE_X));
        output.putInt("SizeY", this.entityData.get(SIZE_Y));
        output.putInt("SizeZ", this.entityData.get(SIZE_Z));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(SIZE_X, Math.max(1, input.getIntOr("SizeX", 1)));
        this.entityData.set(SIZE_Y, Math.max(1, input.getIntOr("SizeY", 1)));
        this.entityData.set(SIZE_Z, Math.max(1, input.getIntOr("SizeZ", 1)));
        this.noPhysics = true;
        this.setPermanentlyInvulnerable(true);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(Entity other) {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE_ENTITY;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }
}
