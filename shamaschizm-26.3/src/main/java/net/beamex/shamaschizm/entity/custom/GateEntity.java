package net.beamex.shamaschizm.entity.custom;

import net.beamex.shamaschizm.entity.GateRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A persistent, immobile 3x3 gate whose panels open after a key is inserted. */
public final class GateEntity extends PathfinderMob {
    public static final int OPEN_ANIMATION_TICKS = 60;
    private static final int REMOVE_COLLISION_TICK = 40;

    private static final EntityDataAccessor<Boolean> OPEN =
            SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> OPEN_TICKS =
            SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Direction> FACING =
            SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.DIRECTION);

    private final AnimationState clientOpenAnimation = new AnimationState();

    public GateEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setPersistenceRequired();
        this.setPermanentlyInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void registerGoals() {
        // The gate is architectural, not an AI-controlled creature.
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        super.defineSynchedData(data);
        data.define(OPEN, false);
        data.define(OPEN_TICKS, 0);
        data.define(FACING, Direction.NORTH);
    }

    public void setGateFacing(Direction direction) {
        if (!direction.getAxis().isHorizontal()) direction = Direction.NORTH;
        this.entityData.set(FACING, direction);
        float yaw = switch (direction) {
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

    public Direction getGateFacing() {
        return this.entityData.get(FACING);
    }

    @Override
    public float rotate(Rotation rotation) {
        this.setGateFacing(rotation.rotate(this.getGateFacing()));
        return this.getYRot();
    }

    @Override
    public float mirror(Mirror mirror) {
        float before = this.getYRot();
        this.setGateFacing(mirror.mirror(this.getGateFacing()));
        float after = this.getYRot();
        // StructureTemplate combines this return value with the entity's
        // current yaw, so account for setGateFacing having already changed it.
        return 2.0F * after - before;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (OPEN.equals(accessor)) setBoundingBox(makeBoundingBox(position()));
        if (FACING.equals(accessor)) this.refreshDimensions();
    }

    public boolean isOpen() {
        return this.entityData.get(OPEN);
    }

    public int getOpenTicks() {
        return this.entityData.get(OPEN_TICKS);
    }

    public AnimationState getClientOpenAnimation() {
        return this.clientOpenAnimation;
    }

    /** Called by both the entity hitbox and its invisible collision blocks. */
    public InteractionResult tryOpen(Player player, InteractionHand hand) {
        if(this.level() instanceof ServerLevel server && net.beamex.shamaschizm.experimental.BossGardenLocks.isLockedGate(server,blockPosition())){
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Defeat the Catacomb Saints to unlock the garden door."));
            return InteractionResult.SUCCESS_SERVER;
        }
        ItemStack stack = player.getItemInHand(hand);
        boolean ordinaryKey = stack.is(Items.TRIAL_KEY);
        boolean ominousKey = stack.is(Items.OMINOUS_TRIAL_KEY);
        if (this.isOpen() || (!ordinaryKey && !ominousKey)) return InteractionResult.PASS;
        if (this.level().isClientSide()) return InteractionResult.SUCCESS;

        if (ordinaryKey && !player.hasInfiniteMaterials()) {
            stack.consume(1, player);
        }
        this.entityData.set(OPEN, true);
        this.entityData.set(OPEN_TICKS, 0);
        this.playSound(GateRegistration.GATE_OPEN, 1.6F, 1.0F);
        this.spawnUnlockParticles();
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult result = this.tryOpen(player, hand);
        return result == InteractionResult.PASS ? super.mobInteract(player, hand) : result;
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
        this.fallDistance = 0.0F;
        if (this.level().isClientSide()) {
            if (this.isOpen() && !this.clientOpenAnimation.isStarted()) {
                this.clientOpenAnimation.start(this.tickCount - this.getOpenTicks());
            } else if (!this.isOpen()) {
                this.clientOpenAnimation.stop();
            }
            return;
        }

        if(this.level() instanceof ServerLevel server && net.beamex.shamaschizm.experimental.BossGardenLocks.isLockedGate(server,blockPosition())){
            boolean wasOpen=isOpen();this.entityData.set(OPEN,false);this.entityData.set(OPEN_TICKS,0);
            if(wasOpen || tickCount<=1 || tickCount%20==0)installCollisionBlocks();
            return;
        }
        if (this.isOpen()) {
            int ticks = Math.min(OPEN_ANIMATION_TICKS, this.getOpenTicks() + 1);
            this.entityData.set(OPEN_TICKS, ticks);
            if (ticks == REMOVE_COLLISION_TICK) this.removeCollisionBlocks();
        } else if (this.tickCount <= 1 || this.tickCount % 20 == 0) {
            this.installCollisionBlocks();
        }
    }

    public boolean canInstallCollisionBlocks() {
        for (BlockPos pos : this.collisionPositions()) {
            if (!this.level().getBlockState(pos).canBeReplaced()) return false;
        }
        return true;
    }

    public void installCollisionBlocks() {
        if (this.isOpen()) return;
        for (BlockPos pos : this.collisionPositions()) {
            var existing = this.level().getBlockState(pos);
            if (existing.canBeReplaced()) {
                this.level().setBlock(pos, GateRegistration.GATE_BARRIER.defaultBlockState(), 3);
            } else if (this.tickCount <= 1 && existing.is(GateRegistration.GATE_BARRIER)
                    && this.level() instanceof ServerLevel serverLevel) {
                // Refresh barriers saved by the older opaque implementation so
                // neighboring faces and lighting are rebuilt after an update.
                serverLevel.sendBlockUpdated(pos, existing, existing, 3);
                serverLevel.getChunkSource().getLightEngine().checkBlock(pos);
            }
        }
    }

    private void removeCollisionBlocks() {
        for (BlockPos pos : this.collisionPositions()) {
            if (this.level().getBlockState(pos).is(GateRegistration.GATE_BARRIER)) {
                this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private BlockPos[] collisionPositions() {
        BlockPos base = BlockPos.containing(this.getX(), this.getY(), this.getZ());
        Direction across = this.getGateFacing().getClockWise();
        BlockPos[] positions = new BlockPos[9];
        int index = 0;
        for (int y = 0; y < 3; y++) {
            for (int offset = -1; offset <= 1; offset++) {
                positions[index++] = base.above(y).relative(across, offset);
            }
        }
        return positions;
    }

    private void spawnUnlockParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        BlockParticleOption stone = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
        for (int step = 0; step < 7; step++) {
            serverLevel.sendParticles(stone, this.getX(), this.getY() + 0.25D + step * 0.42D, this.getZ(),
                    3, 0.07D, 0.07D, 0.07D, 0.035D);
        }
    }

    // Distance culling must use the visible gate size, not its empty open hitbox.
    @Override public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 2.08000000D * 64.0D * Entity.getViewScale();
        return distance < range * range;
    }
    @Override public boolean isPickable() { return !isOpen() && super.isPickable(); }
    @Override public boolean canBeCollidedWith(Entity other) { return !isOpen() && super.canBeCollidedWith(other); }
    @Override public boolean canCollideWith(Entity other) { return !isOpen() && super.canCollideWith(other); }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        if (isOpen()) return new AABB(position, position);
        double halfWidth = 1.5D;
        double halfDepth = 0.12D;
        return this.getGateFacing().getAxis() == Direction.Axis.Z
                ? new AABB(position.x - halfWidth, position.y, position.z - halfDepth,
                           position.x + halfWidth, position.y + 3.0D, position.z + halfDepth)
                : new AABB(position.x - halfDepth, position.y, position.z - halfWidth,
                           position.x + halfDepth, position.y + 3.0D, position.z + halfWidth);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {}

    @Override
    public void push(double x, double y, double z) {}

    @Override
    protected void pushEntities() {
        // Closed collision is provided by GateBarrierBlock. An entity must not
        // apply the normal living-entity shove, especially after it is open.
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    @Override
    public void onRemoval(Entity.RemovalReason reason) {
        if (!this.level().isClientSide() && reason.shouldDestroy()) {
            this.removeCollisionBlocks();
        }
        super.onRemoval(reason);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("GateOpen", this.isOpen());
        output.putInt("GateOpenTicks", this.getOpenTicks());
        output.putInt("GateFacing", this.getGateFacing().get2DDataValue());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(OPEN, input.getBooleanOr("GateOpen", false));
        this.entityData.set(OPEN_TICKS, input.getIntOr("GateOpenTicks", 0));
        this.setGateFacing(Direction.from2DDataValue(input.getIntOr("GateFacing", Direction.NORTH.get2DDataValue())));
        this.setPersistenceRequired();
        this.setPermanentlyInvulnerable(true);
    }
}
