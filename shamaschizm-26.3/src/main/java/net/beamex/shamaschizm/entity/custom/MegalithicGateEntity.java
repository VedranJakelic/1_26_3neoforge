package net.beamex.shamaschizm.entity.custom;

import java.util.ArrayList;
import java.util.List;
import net.beamex.shamaschizm.entity.GateRegistration;
import net.beamex.shamaschizm.entity.MegalithicGateRegistration;
import net.beamex.shamaschizm.world.block.BrazierBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A permanent 3x3 gate unlocked by every nearby brazier being lit. */
public final class MegalithicGateEntity extends PathfinderMob {
    public static final int OPEN_ANIMATION_TICKS = 123;
    public static final int FIRE_TRACE_TICKS = 20;
    private static final int REMOVE_COLLISION_TICK = 100;
    private static final int PERIODIC_CHECK_TICKS = 40;
    private static final double BRAZIER_RADIUS_SQUARED = BrazierBlock.GATE_RADIUS * BrazierBlock.GATE_RADIUS;

    private static final EntityDataAccessor<Boolean> OPEN =
            SynchedEntityData.defineId(MegalithicGateEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> OPEN_TICKS =
            SynchedEntityData.defineId(MegalithicGateEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Direction> FACING =
            SynchedEntityData.defineId(MegalithicGateEntity.class, EntityDataSerializers.DIRECTION);

    private final AnimationState clientOpenAnimation = new AnimationState();
    private List<BlockPos> unlockBraziers = List.of();
    private int fireTraceTicks;
    private boolean checkRequested = true;

    public MegalithicGateEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        setPermanentlyInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override protected void registerGoals() {}

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        super.defineSynchedData(data);
        data.define(OPEN, false);
        data.define(OPEN_TICKS, 0);
        data.define(FACING, Direction.NORTH);
    }

    public boolean isOpen() { return entityData.get(OPEN); }
    public int getOpenTicks() { return entityData.get(OPEN_TICKS); }
    public Direction getGateFacing() { return entityData.get(FACING); }
    public AnimationState getClientOpenAnimation() { return clientOpenAnimation; }

    public void setGateFacing(Direction direction) {
        if (!direction.getAxis().isHorizontal()) direction = Direction.NORTH;
        entityData.set(FACING, direction);
        float yaw = switch (direction) {
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            case EAST -> -90.0F;
            default -> 180.0F;
        };
        setYRot(yaw);
        setYHeadRot(yaw);
        yBodyRot = yaw;
    }

    @Override
    public float rotate(Rotation rotation) {
        setGateFacing(rotation.rotate(getGateFacing()));
        return getYRot();
    }

    @Override
    public float mirror(Mirror mirror) {
        float before = getYRot();
        setGateFacing(mirror.mirror(getGateFacing()));
        return 2.0F * getYRot() - before;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (OPEN.equals(accessor)) setBoundingBox(makeBoundingBox(position()));
        if (FACING.equals(accessor)) refreshDimensions();
    }

    public void requestBrazierCheck() {
        if (!isOpen()) checkRequested = true;
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        fallDistance = 0.0F;
        if (level().isClientSide()) {
            if (isOpen() && !clientOpenAnimation.isStarted()) {
                clientOpenAnimation.start(tickCount - getOpenTicks());
            } else if (!isOpen()) {
                clientOpenAnimation.stop();
            }
            return;
        }

        if(!phraseUnlocked && level() instanceof ServerLevel server && net.beamex.shamaschizm.experimental.BossGardenLocks.isLockedGate(server,blockPosition())){
            boolean wasOpen=isOpen();entityData.set(OPEN,false);entityData.set(OPEN_TICKS,0);fireTraceTicks=0;unlockBraziers=List.of();
            if(wasOpen || tickCount<=1 || tickCount%20==0)installCollisionBlocks();
            return;
        }
        if (isOpen()) {
            int previousTicks = getOpenTicks();

            if (previousTicks < OPEN_ANIMATION_TICKS) {
                int ticks = previousTicks + 1;
                entityData.set(OPEN_TICKS, ticks);

                // Stop slightly early so existing particles disappear by the time
                // the opening animation finishes.
                if (ticks <= OPEN_ANIMATION_TICKS - 20
                        && (ticks == 1 || ticks % 3 == 0)) {
                    spawnOpeningStoneParticles(ticks == 1 ? 30 : 14);
                }

                if (ticks == REMOVE_COLLISION_TICK) {
                    removeCollisionBlocks();
                }
            }

            return;
        }

        if (fireTraceTicks > 0) {
            List<BlockPos> current = findBraziersWhenAllLit();
            if (current.isEmpty()) {
                fireTraceTicks = 0;
                unlockBraziers = List.of();
                checkRequested = false;
                return;
            }
            unlockBraziers = current;
            traceFireParticles();
            fireTraceTicks++;
            if (fireTraceTicks > FIRE_TRACE_TICKS) openGate();
        } else if (checkRequested || tickCount <= 2 || tickCount % PERIODIC_CHECK_TICKS == 0) {
            checkRequested = false;
            List<BlockPos> braziers = findBraziersWhenAllLit();
            if (!braziers.isEmpty()) {
                unlockBraziers = braziers;
                fireTraceTicks = 1;
                traceFireParticles();
            }
        }

        if (tickCount <= 1 || tickCount % 20 == 0) installCollisionBlocks();
    }

    private List<BlockPos> findBraziersWhenAllLit() {
        BlockPos center = blockPosition();
        int radius = (int)Math.ceil(BrazierBlock.GATE_RADIUS);
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos cursor : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            double dx = cursor.getX() + 0.5D - getX();
            double dy = cursor.getY() + 0.5D - (getY() + 1.5D);
            double dz = cursor.getZ() + 0.5D - getZ();
            if (dx * dx + dy * dy + dz * dz > BRAZIER_RADIUS_SQUARED) continue;
            var state = level().getBlockState(cursor);
            if (!state.is(MegalithicGateRegistration.BRAZIER)) continue;
            if (!state.getValue(BrazierBlock.LIT)) return List.of();
            found.add(cursor.immutable());
        }
        // Zero braziers must never unlock a gate.
        return found;
    }

    private void traceFireParticles() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        double t = Math.min(1.0D, fireTraceTicks / (double)FIRE_TRACE_TICKS);
        Vec3 target = new Vec3(getX(), getY() + 1.5D, getZ());
        for (BlockPos sourcePos : unlockBraziers) {
            Vec3 source = new Vec3(sourcePos.getX() + 0.5D, sourcePos.getY() + 0.78D, sourcePos.getZ() + 0.5D);
            Vec3 point = source.lerp(target, t).add(0.0D, Math.sin(Math.PI * t) * 0.9D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z,
                    3, 0.055D, 0.055D, 0.055D, 0.005D);
        }
    }

    public void openByPhrase() {
        if (!(level() instanceof ServerLevel)) return;
        phraseUnlocked=true;openGate();
    }

    private boolean phraseUnlocked;
    private void openGate() {
        if (!(level() instanceof ServerLevel) || entityData.get(OPEN)) return;
        entityData.set(OPEN, true);
        entityData.set(OPEN_TICKS, 0);
        fireTraceTicks = 0;
        unlockBraziers = List.of();
        playSound(MegalithicGateRegistration.OPEN_SOUND, 1.7F, 1.0F);
        playSound(SoundEvents.ZOMBIE_VILLAGER_CURE, 1.25F, 0.85F);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.5D, getZ(),
                    30, 0.45D, 0.75D, 0.25D, 0.035D);
        }
    }

    /** Stone fragments scrape from the complete 3x3 perimeter while the slab rises. */
    private void spawnOpeningStoneParticles(int count) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        BlockParticleOption stone = new BlockParticleOption(
                ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
        Direction across = getGateFacing().getClockWise();
        Direction depth = getGateFacing();
        for (int i = 0; i < count; i++) {
            double acrossOffset;
            double height;
            int edge = getRandom().nextInt(4);
            if (edge == 0 || edge == 1) {
                acrossOffset = edge == 0 ? -1.48D : 1.48D;
                height = 0.08D + getRandom().nextDouble() * 2.84D;
            } else {
                acrossOffset = -1.45D + getRandom().nextDouble() * 2.9D;
                height = edge == 2 ? 0.06D : 2.96D;
            }
            double depthOffset = getRandom().triangle(0.0D, 0.14D);
            double x = getX() + across.getStepX() * acrossOffset + depth.getStepX() * depthOffset;
            double y = getY() + height;
            double z = getZ() + across.getStepZ() * acrossOffset + depth.getStepZ() * depthOffset;
            serverLevel.sendParticles(stone, x, y, z, 1,
                    0.035D, 0.035D, 0.035D, 0.055D);
        }
    }

    public boolean canInstallCollisionBlocks() {
        for (BlockPos pos : collisionPositions()) {
            var state = level().getBlockState(pos);
            if (!state.canBeReplaced() && !state.is(GateRegistration.GATE_BARRIER)) return false;
        }
        return true;
    }

    public void installCollisionBlocks() {
        if (isOpen()) return;
        for (BlockPos pos : collisionPositions()) {
            var state = level().getBlockState(pos);
            if (state.canBeReplaced()) {
                level().setBlock(pos, GateRegistration.GATE_BARRIER.defaultBlockState(), 3);
            } else if (state.is(GateRegistration.GATE_BARRIER) && tickCount <= 1
                    && level() instanceof ServerLevel serverLevel) {
                serverLevel.sendBlockUpdated(pos, state, state, 3);
                serverLevel.getChunkSource().getLightEngine().checkBlock(pos);
            }
        }
    }

    private void removeCollisionBlocks() {
        for (BlockPos pos : collisionPositions()) {
            if (level().getBlockState(pos).is(GateRegistration.GATE_BARRIER)) {
                level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private BlockPos[] collisionPositions() {
        BlockPos base = BlockPos.containing(getX(), getY(), getZ());
        Direction across = getGateFacing().getClockWise();
        BlockPos[] result = new BlockPos[9];
        int index = 0;
        for (int y = 0; y < 3; y++) {
            for (int offset = -1; offset <= 1; offset++) {
                result[index++] = base.above(y).relative(across, offset);
            }
        }
        return result;
    }

    // Distance culling must use the visible gate size, not its empty open hitbox.
    @Override public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 2.29166667D * 64.0D * Entity.getViewScale();
        return distance < range * range;
    }
    @Override public boolean isPickable() { return !isOpen() && super.isPickable(); }
    @Override public boolean canBeCollidedWith(Entity other) { return !isOpen() && super.canBeCollidedWith(other); }
    @Override public boolean canCollideWith(Entity other) { return !isOpen() && super.canCollideWith(other); }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        if (isOpen()) return new AABB(position, position);
        double halfWidth = 1.5D;
        double halfDepth = 0.4375D;
        return getGateFacing().getAxis() == Direction.Axis.Z
                ? new AABB(position.x - halfWidth, position.y, position.z - halfDepth,
                position.x + halfWidth, position.y + 3.0D, position.z + halfDepth)
                : new AABB(position.x - halfDepth, position.y, position.z - halfWidth,
                position.x + halfDepth, position.y + 3.0D, position.z + halfWidth);
    }

    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public void push(Entity entity) {}
    @Override public void push(double x, double y, double z) {}
    @Override protected void pushEntities() {}
    @Override public boolean removeWhenFarAway(double distanceSquared) { return false; }

    @Override
    public void onRemoval(Entity.RemovalReason reason) {
        if (!level().isClientSide() && reason.shouldDestroy()) removeCollisionBlocks();
        super.onRemoval(reason);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("PhraseUnlocked",phraseUnlocked);
        output.putBoolean("MegalithicGateOpen", isOpen());
        output.putInt("MegalithicGateOpenTicks", getOpenTicks());
        output.putInt("MegalithicGateFacing", getGateFacing().get2DDataValue());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        phraseUnlocked=input.getBooleanOr("PhraseUnlocked",false);
        entityData.set(OPEN, input.getBooleanOr("MegalithicGateOpen", false));
        entityData.set(OPEN_TICKS, input.getIntOr("MegalithicGateOpenTicks", 0));
        setGateFacing(Direction.from2DDataValue(input.getIntOr(
                "MegalithicGateFacing", Direction.NORTH.get2DDataValue())));
        fireTraceTicks = 0;
        unlockBraziers = List.of();
        checkRequested = !isOpen();
        setPersistenceRequired();
        setPermanentlyInvulnerable(true);
    }
}
