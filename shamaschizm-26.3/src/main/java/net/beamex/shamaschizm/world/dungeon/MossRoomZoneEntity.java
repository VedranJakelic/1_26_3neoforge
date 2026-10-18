package net.beamex.shamaschizm.world.dungeon;

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

/** Persistent invisible volume retaining the tag of a generated moss room. */
public final class MossRoomZoneEntity extends Entity {
    private static final EntityDataAccessor<Integer> SIZE_X =
            SynchedEntityData.defineId(MossRoomZoneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SIZE_Y =
            SynchedEntityData.defineId(MossRoomZoneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SIZE_Z =
            SynchedEntityData.defineId(MossRoomZoneEntity.class, EntityDataSerializers.INT);

    public MossRoomZoneEntity(EntityType<?> type, Level level) {
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
        int x = box.maxX() - box.minX() + 1;
        int y = box.maxY() - box.minY() + 1;
        int z = box.maxZ() - box.minZ() + 1;
        this.entityData.set(SIZE_X, x);
        this.entityData.set(SIZE_Y, y);
        this.entityData.set(SIZE_Z, z);
        this.snapTo(box.minX() + x / 2.0D, box.minY() + y / 2.0D, box.minZ() + z / 2.0D);
    }

    private AABB bounds() {
        double x = this.entityData.get(SIZE_X) / 2.0D;
        double y = this.entityData.get(SIZE_Y) / 2.0D;
        double z = this.entityData.get(SIZE_Z) / 2.0D;
        return new AABB(this.getX() - x, this.getY() - y, this.getZ() - z,
                this.getX() + x, this.getY() + y, this.getZ() + z);
    }

    public static boolean contains(ServerLevel level, BlockPos pos) {
        Vec3 point = Vec3.atCenterOf(pos);
        for (MossRoomZoneEntity zone : level.getEntitiesOfClass(
                MossRoomZoneEntity.class, new AABB(pos).inflate(96.0D))) {
            if (zone.bounds().contains(point)) return true;
        }
        return false;
    }

    @Override protected void addAdditionalSaveData(ValueOutput out) {
        out.putInt("SizeX", this.entityData.get(SIZE_X));
        out.putInt("SizeY", this.entityData.get(SIZE_Y));
        out.putInt("SizeZ", this.entityData.get(SIZE_Z));
    }

    @Override protected void readAdditionalSaveData(ValueInput in) {
        this.entityData.set(SIZE_X, Mth.clamp(in.getIntOr("SizeX", 1), 1, 512));
        this.entityData.set(SIZE_Y, Mth.clamp(in.getIntOr("SizeY", 1), 1, 512));
        this.entityData.set(SIZE_Z, Mth.clamp(in.getIntOr("SizeZ", 1), 1, 512));
        this.noPhysics = true;
        this.setPermanentlyInvulnerable(true);
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith(Entity other) { return false; }
    @Override public PushReaction getPistonPushReaction() { return PushReaction.IGNORE_ENTITY; }
    @Override public boolean isIgnoringBlockTriggers() { return true; }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }
}
