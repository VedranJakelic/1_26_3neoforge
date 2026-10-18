package net.beamex.shamaschizm.entity.custom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.beamex.shamaschizm.entity.ModEntities;
import net.beamex.shamaschizm.sound.ModSounds;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

/** One independently damageable head or body section in a synchronized centipede chain. */
public final class GiantCentipedeEntity extends PathfinderMob {
    public static final int BODY = 0;
    public static final int HEAD = 1;
    private static final Direction[] HORIZONTAL_DIRECTIONS = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };
    private static final double TRACK_RANGE_SQR = 60.0D * 60.0D;
    private static final double CROUCH_DETECTION_SQR = 6.0D * 6.0D;
    private static final double MIN_GOAL_DISTANCE_GAIN = 0.30D;
    /** The exported shell is 0.625 blocks long; slight overlap keeps every joint closed. */
    private static final double PART_SPACING = 0.58D;
    private static final int MIN_BRAVE_SECTIONS = 4;
    private static final double CHASE_SPEED = 0.43D;
    private static final double WANDER_SPEED = 0.32D;
    private static final double RECONNECT_RANGE = 10.0D;
    private static final double RECONNECT_MIN_PROGRESS = 0.35D;
    private static final double MIN_DRIVER_DISPLACEMENT_SQR = 0.45D * 0.45D;
    private static final int DRIVER_STUCK_CHECK_TICKS = 20;
    private static final int SECOND_HEAD_STALL_WINDOW = 120;
    private static final int SELF_RECONNECT_DURATION = 300;
    private static final int SELF_RECONNECT_HEAD_TURN = 40;
    private static final double SELF_RECONNECT_DISTANCE_SQR = 0.85D * 0.85D;
    private static final int DEATH_CLOUD_DURATION = 15 * 60 * 20;

    private static final EntityDataAccessor<Integer> PART_KIND =
            SynchedEntityData.defineId(GiantCentipedeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PART_INDEX =
            SynchedEntityData.defineId(GiantCentipedeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DRIVER =
            SynchedEntityData.defineId(GiantCentipedeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> MOVING =
            SynchedEntityData.defineId(GiantCentipedeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> VISUAL_ROLL =
            SynchedEntityData.defineId(GiantCentipedeEntity.class, EntityDataSerializers.FLOAT);
    /** Direction from this part toward its supporting block. */
    private static final EntityDataAccessor<Direction> SUPPORT =
            SynchedEntityData.defineId(GiantCentipedeEntity.class, EntityDataSerializers.DIRECTION);

    private UUID chainId = UUID.randomUUID();
    private @Nullable UUID aggroPlayer;
    private boolean initialized;
    private int healthBodySections = -1;
    private boolean suppressChainDeath;
    private int attackCooldown;
    private int nextCrawlSoundTick;
    private int nextWanderTurn;
    private @Nullable UUID reconnectHead;
    private double reconnectDistance = Double.NaN;
    private int nextReconnectProgressCheck;
    private int reconnectNavigationUntil;
    private int unsupportedTicks;
    private static final int BLOCK_BREAK_TICKS = 20;
    private @Nullable BlockPos breakingBlock;
    private int breakingBlockTicks;
    private Vec3 driverWatchPosition = Vec3.ZERO;
    private int nextDriverMovementCheck;
    private boolean driverWatchInitialized;
    private @Nullable UUID lastStalledHead;
    private int lastHeadStallTick;
    private boolean selfReconnectMode;
    private int selfReconnectUntil;
    private int selfReconnectSwapTick;
    private Vec3 wanderDirection = Vec3.ZERO;
    private @Nullable BlockPos lightTarget;
    private int nextLightScan;
    private int lightEatTicks;
    private @Nullable BlockPos watchedLightProgressTarget;
    private Vec3 lightProgressGoal = Vec3.ZERO;
    private double lightProgressDistance = Double.NaN;
    private int nextLightProgressCheck;
    /** A light that this chain could see but could not physically reach. */
    private @Nullable BlockPos rejectedLightTarget;
    private int rejectedLightUntil;
    private @Nullable UUID watchedTarget;
    /** Fixed goal snapshot for the current one-second progress window. */
    private Vec3 progressWindowGoal = Vec3.ZERO;
    private double progressWindowTargetDistance = Double.NaN;
    private int nextProgressCheck;
    private int vanillaPathfindingUntil;
    private @Nullable BlockPos climbApproach;
    private @Nullable Direction climbWall;
    private int nextClimbSearch;
    private int nextDriverReversalTick;
    private float visualRollO;

    public GiantCentipedeEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.xpReward = 4;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 36.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.42D)
                .add(Attributes.FOLLOW_RANGE, 60.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.55D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        super.defineSynchedData(data);
        data.define(PART_KIND, HEAD);
        data.define(PART_INDEX, 0);
        data.define(DRIVER, true);
        data.define(MOVING, false);
        data.define(VISUAL_ROLL, 0.0F);
        data.define(SUPPORT, Direction.DOWN);
    }

    @Override
    protected void registerGoals() {
        // Movement is chain-aware and three-dimensional; vanilla ground goals
        // cannot preserve a multipart body on walls and ceilings.
    }

    public static boolean canSpawn(EntityType<GiantCentipedeEntity> type, ServerLevelAccessor accessor,
                                   EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        ServerLevel level = accessor.getLevel();
        if (!level.dimension().equals(Schizm.KEY)
                || !Mob.checkMobSpawnRules(type, accessor, reason, pos, random)) return false;
        AABB hundredByHundred = new AABB(pos.getX() - 50.0D, pos.getY() - 2048.0D, pos.getZ() - 50.0D,
                pos.getX() + 50.0D, pos.getY() + 2048.0D, pos.getZ() + 50.0D);
        long chains = level.getEntitiesOfClass(GiantCentipedeEntity.class, hundredByHundred,
                part -> part.isDriver() && part.isAlive()).stream().map(GiantCentipedeEntity::getChainId).distinct().count();
        return chains < 2;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor accessor, DifficultyInstance difficulty,
                                                   EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData result = super.finalizeSpawn(accessor, difficulty, reason, groupData);
        if (!this.initialized) {
            int sections = chooseSectionCount(accessor.getLevel(), this.blockPosition().getY(), this.random);
            initializeFullChain(accessor.getLevel(), sections);
        }
        return result;
    }

    private static int chooseSectionCount(ServerLevel level, int y, RandomSource random) {
        // Deeper Schizm positions steadily bias the roll upward without ever
        // leaving the requested 5-12 section range.
        int depthBonus = Mth.clamp((170 - y) / 24, 0, 7);
        int randomRoom = 7 - depthBonus;
        return 5 + depthBonus + (randomRoom > 0 ? random.nextInt(randomRoom + 1) : 0);
    }

    /** Egg hatching must bypass the normal five-to-twelve-section spawn roll. */
    public void initializeEggChain(ServerLevel level) {
        if (!this.initialized) initializeFullChain(level, 2);
    }

    private void initializeFullChain(ServerLevel level, int sectionCount) {
        this.chainId = UUID.randomUUID();
        this.initialized = true;
        this.setPartKind(HEAD);
        this.setDriver(true);
        this.setPartIndex(0);
        this.setPersistenceRequired();
        List<GiantCentipedeEntity> chain = new ArrayList<>();
        chain.add(this);
        Vec3 backwards = Vec3.directionFromRotation(0.0F, this.getYRot()).scale(-PART_SPACING);
        for (int i = 1; i <= sectionCount; i++) {
            GiantCentipedeEntity body = createPart(level, BODY, false, i,
                    this.position().add(backwards.scale(i)), this.getYRot());
            if (body != null) chain.add(body);
        }
        GiantCentipedeEntity rear = createPart(level, HEAD, false, chain.size(),
                this.position().add(backwards.scale(sectionCount + 1)), this.getYRot() + 180.0F);
        if (rear != null) chain.add(rear);
        assignChain(chain, this.chainId);
    }

    private @Nullable GiantCentipedeEntity createPart(ServerLevel level, int kind, boolean driver,
                                                       int index, Vec3 position, float yaw) {
        GiantCentipedeEntity part = ModEntities.GIANT_CENTIPEDE.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (part == null) return null;
        part.chainId = this.chainId;
        part.initialized = true;
        part.setPartKind(kind);
        part.setDriver(driver);
        part.setPartIndex(index);
        part.snapTo(position.x, position.y, position.z, yaw, 0.0F);
        part.setHealth(kind == HEAD ? part.getMaxHealth() : 16.0F);
        part.setPersistenceRequired();
        level.addFreshEntity(part);
        return part;
    }

    @Override
    public void tick() {
        this.visualRollO = this.entityData.get(VISUAL_ROLL);
        super.tick();
        this.setNoGravity(true);
        this.fallDistance = 0.0D;
        if (this.level().isClientSide()) {
            spawnHeadDripParticle();
            return;
        }
        if (!(this.level() instanceof ServerLevel level) || !this.initialized) return;
        if (this.attackCooldown > 0) this.attackCooldown--;
        if (!this.isDriver()) {
            // The driver performs the ordered joint solve. Non-driving entity
            // ticks must not independently re-solve links in an arbitrary
            // world tick order.
            this.setNoGravity(true);
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        if (tickBreakingBlock(level)) {
            this.setDeltaMovement(Vec3.ZERO);
            this.entityData.set(MOVING, false);
            return;
        }

        tickDriver(level);
        if (!this.isAlive() || this.isRemoved()) return;
        recoverFromDriverStall(level);
        List<GiantCentipedeEntity> currentChain = loadedChain(level, this.chainId);
        // Migrate old saves only when the complete indexed chain is loaded.
        if (this.tickCount % 20 == 0 && currentChain.size() >= 2
                && currentChain.getFirst().isHead() && currentChain.getLast().isHead()) {
            boolean complete = true;
            for (int i = 0; i < currentChain.size(); i++) {
                if (currentChain.get(i).getPartIndex() != i) complete = false;
            }
            if (complete) updateChainHealth(currentChain);
        }
        if (currentChain.isEmpty()) return;
        GiantCentipedeEntity currentDriver = currentChain.get(0);
        currentDriver.entityData.set(MOVING,
                currentDriver.getDeltaMovement().lengthSqr() > 0.003D);
        solveFollowerJoints(level, currentChain);
        currentDriver.updateSupport(level);
        if (currentDriver.getDeltaMovement().lengthSqr() > 0.003D) currentDriver.playCrawlSound();
    }

    /** Client-only green tears emitted independently by both endpoint heads. */
    private void spawnHeadDripParticle() {
        if (!this.isHead() || !this.isAlive() || this.isInvisible()
                || this.random.nextInt(6) != 0) {
            return;
        }
        double spread = this.getBbWidth() * 0.28D;
        double x = this.getX() + (this.random.nextDouble() * 2.0D - 1.0D) * spread;
        double y = this.getY() + this.getBbHeight() * 0.28D;
        double z = this.getZ() + (this.random.nextDouble() * 2.0D - 1.0D) * spread;
        this.level().addParticle(ParticleTypes.FALLING_SPORE_BLOSSOM,
                x, y, z, 0.0D, -0.025D, 0.0D);
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 multiplier) {
        if (!state.is(Blocks.COBWEB) && !(state.getBlock() instanceof net.beamex.shamaschizm.building.SpanningWebBlock))
            super.makeStuckInBlock(state, multiplier);
    }

    /** A fighting chain with eleven or more body sections must stay separate. */
    private static boolean blocksRecombination(ServerLevel level, List<GiantCentipedeEntity> chain) {
        if (chain.size() - 2 <= 10) return false;
        for (GiantCentipedeEntity part : chain) {
            if (part.isDriver() && part.isAlive() && part.findTrackablePlayer(level) != null) return true;
        }
        return false;
    }

    private void tickDriver(ServerLevel level) {
        List<GiantCentipedeEntity> chain = loadedChain(level, this.chainId);
        if (this.selfReconnectMode && blocksRecombination(level, chain)) clearSelfReconnect(chain);
        if (this.selfReconnectMode) {
            tickSelfReconnect(level, chain);
            return;
        }
        if (this.tickCount % 5 == 0 && mergeIfTouchingAnotherChain(level, chain)) return;
        int sections = Math.max(0, chain.size() - 2);
        if (sections < MIN_BRAVE_SECTIONS) {
            if (tickReconnect(level, chain)) return;
            fleeNearestPlayer(level);
            return;
        }

        Player target = findTrackablePlayer(level);
        if (target == null) {
            this.setTarget(null);
            resetPursuitWatch();
            // Long chains also seek one another whenever they would otherwise
            // wander. Short chains retain the stronger priority above and do
            // this even while a player is nearby.
            if (tickReconnect(level, chain)) return;
            if (tickTorchHunt(level)) return;
            tickWander(level);
            return;
        }
        if (this.reconnectHead != null || this.reconnectNavigationUntil > 0) {
            clearReconnectNavigation();
        }
        this.setTarget(target);
        boolean stuck = isPursuitStuck(target);
        if (stuck && reverseDriverWhenStuck(chain, target, target.getEyePosition())) return;
        if (tickVerticalRoute(level, target)) return;
        if (stuck) beginVanillaPathfinding(target);
        if (tickVanillaPathfinding(level, target)) return;
        Vec3 toTarget = target.getEyePosition().subtract(this.position());
        Vec3 normal = supportNormal();
        Vec3 surfaceToward = surfaceDirection(toTarget, normal);
        Vec3 tangent = normal.cross(surfaceToward);
        if ((this.chainId.hashCode() & 1) == 0) tangent = tangent.scale(-1.0D);
        double distance = Math.sqrt(this.distanceToSqr(target));
        double closingWeight = Mth.clamp((distance - 2.0D) / 18.0D, 0.20D, 0.72D);
        Vec3 desired = tangent.scale(0.82D).add(surfaceToward.scale(closingWeight)).normalize().scale(CHASE_SPEED);

        desired = transitionOntoObstacle(level, desired, toTarget, CHASE_SPEED);
        desired = desired.add(supportNormal().scale(-0.055D));
        moveChainPart(desired, 0.48D);
        faceDriverFromDistances(level, desired);
        tryContactAttack(level, target);
    }

    /**
     * Ground navigation can only reach the floor below a vertically separated
     * target. This stage instead finds a reachable air block beside a full
     * wall, walks to that wall, attaches to it, and climbs toward the target.
     */
    private boolean tickVerticalRoute(ServerLevel level, Player target) {
        Vec3 toTarget = target.getEyePosition().subtract(this.position());
        double verticalGap = toTarget.y;
        if (Math.abs(verticalGap) < 2.25D || this.hasLineOfSight(target)) {
            clearVerticalRoute();
            return false;
        }
        if (this.vanillaPathfindingUntil > 0) finishVanillaPathfinding();

        Direction support = getSupportDirection();
        BlockPos supportPos = this.blockPosition().relative(support);
        boolean onFullVerticalWall = support.getAxis().isHorizontal()
                && solid(level, supportPos)
                && isFullClimbableSurface(level.getBlockState(supportPos));
        if (onFullVerticalWall) {
            this.getNavigation().stop();
            this.setNoGravity(true);
            clearClimbApproach();

            double verticalDirection = verticalGap > 0.0D ? 1.0D : -1.0D;
            Vec3 preferred = new Vec3(toTarget.x * 0.18D, verticalDirection, toTarget.z * 0.18D);
            Vec3 desired = surfaceDirection(preferred, supportNormal()).scale(0.40D);
            desired = transitionOntoObstacle(level, desired, preferred, 0.40D)
                    .add(supportNormal().scale(-0.055D));
            moveChainPart(desired, 0.46D);
            faceDriverFromDistances(level, desired);
            tryContactAttack(level, target);
            return true;
        }

        if (this.climbApproach == null || this.climbWall == null
                || this.tickCount >= this.nextClimbSearch
                || !validClimbApproach(level, this.climbApproach, this.climbWall, verticalGap)) {
            findClimbApproach(level, target, verticalGap);
        }
        if (this.climbApproach == null || this.climbWall == null) return false;

        Vec3 destination = new Vec3(this.climbApproach.getX() + 0.5D,
                this.climbApproach.getY(), this.climbApproach.getZ() + 0.5D);
        Vec3 offset = destination.subtract(this.position());
        if (offset.multiply(1.0D, 0.0D, 1.0D).lengthSqr() <= 0.85D * 0.85D
                && Math.abs(offset.y) <= 1.25D) {
            this.getNavigation().stop();
            this.setNoGravity(true);
            this.entityData.set(SUPPORT, this.climbWall);
            clearClimbApproach();

            Vec3 desired = new Vec3(0.0D, verticalGap > 0.0D ? 0.40D : -0.40D, 0.0D)
                    .add(supportNormal().scale(-0.055D));
            moveChainPart(desired, 0.46D);
            faceDriverFromDistances(level, desired);
            return true;
        }

        // Use ground navigation only to reach the selected wall base, never the
        // unreachable elevated player position.
        this.setNoGravity(false);
        if (this.tickCount % 10 == 0 || this.getNavigation().isDone()) {
            boolean moving = this.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.35D);
            if (!moving) this.nextClimbSearch = this.tickCount;
        }
        this.getLookControl().setLookAt(destination.x, destination.y, destination.z, 30.0F, 30.0F);
        return true;
    }

    private void findClimbApproach(ServerLevel level, Player target, double verticalGap) {
        clearClimbApproach();
        this.nextClimbSearch = this.tickCount + 40;
        BlockPos origin = this.blockPosition();
        double bestScore = Double.MAX_VALUE;
        int verticalStep = verticalGap > 0.0D ? 1 : -1;

        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                if (dx * dx + dz * dz > 12 * 12) continue;
                BlockPos candidate = origin.offset(dx, 0, dz);
                Vec3 destination = new Vec3(candidate.getX() + 0.5D,
                        candidate.getY(), candidate.getZ() + 0.5D);
                AABB movedBox = this.getBoundingBox().move(destination.subtract(this.position()));
                if (!level.noBlockCollision(this, movedBox)
                        || !solid(level, candidate.below())
                        || level.getBlockState(candidate.below()).getBlock() instanceof WallBlock) continue;

                for (Direction wall : HORIZONTAL_DIRECTIONS) {
                    if (!validClimbApproach(level, candidate, wall, verticalGap)) continue;
                    BlockPos wallPos = candidate.relative(wall);
                    // Prefer a wall that continues in the required vertical
                    // direction and lies horizontally closer to the target.
                    boolean continues = solid(level, wallPos.offset(0, verticalStep, 0))
                            && isFullClimbableSurface(level.getBlockState(
                            wallPos.offset(0, verticalStep, 0)));
                    double targetDistance = destination.multiply(1.0D, 0.0D, 1.0D)
                            .distanceTo(target.position().multiply(1.0D, 0.0D, 1.0D));
                    double score = destination.distanceTo(this.position())
                            + targetDistance * 0.20D + (continues ? 0.0D : 4.0D);
                    if (score >= bestScore) continue;

                    if (candidate.distSqr(origin) > 2.0D) {
                        var path = this.getNavigation().createPath(candidate, 0);
                        if (path == null || !path.canReach()) continue;
                    }
                    bestScore = score;
                    this.climbApproach = candidate.immutable();
                    this.climbWall = wall;
                }
            }
        }
    }

    private boolean validClimbApproach(ServerLevel level, BlockPos approach,
                                       Direction wall, double verticalGap) {
        if (!level.hasChunkAt(approach) || !wall.getAxis().isHorizontal()) return false;
        BlockPos wallPos = approach.relative(wall);
        Vec3 destination = new Vec3(approach.getX() + 0.5D,
                approach.getY(), approach.getZ() + 0.5D);
        AABB movedBox = this.getBoundingBox().move(destination.subtract(this.position()));
        return level.noBlockCollision(this, movedBox)
                && solid(level, approach.below())
                && !(level.getBlockState(approach.below()).getBlock() instanceof WallBlock)
                && solid(level, wallPos)
                && isFullClimbableSurface(level.getBlockState(wallPos))
                && (verticalGap > 0.0D || solid(level, wallPos.below()));
    }

    private void clearClimbApproach() {
        this.climbApproach = null;
        this.climbWall = null;
    }

    private void clearVerticalRoute() {
        clearClimbApproach();
        this.nextClimbSearch = 0;
    }

    /**
     * Measures success against a fixed snapshot of the goal rather than raw
     * movement speed. Sideways motion, circling, and jitter all fail unless the
     * driver actually reduces its distance to that goal.
     */
    private boolean isPursuitStuck(Player target) {
        if (!target.getUUID().equals(this.watchedTarget)
                || !Double.isFinite(this.progressWindowTargetDistance)) {
            this.watchedTarget = target.getUUID();
            this.progressWindowGoal = target.getEyePosition();
            this.progressWindowTargetDistance = this.position().distanceTo(this.progressWindowGoal);
            this.nextProgressCheck = this.tickCount + 20;
            return false;
        }
        if (this.tickCount < this.nextProgressCheck || this.tickCount < this.vanillaPathfindingUntil) return false;
        double goalDistanceGain = this.progressWindowTargetDistance
                - this.position().distanceTo(this.progressWindowGoal);
        this.progressWindowGoal = target.getEyePosition();
        this.progressWindowTargetDistance = this.position().distanceTo(this.progressWindowGoal);
        this.nextProgressCheck = this.tickCount + 20;
        boolean contactStillBlocked = this.distanceToSqr(target) > 2.35D * 2.35D
                || !this.hasLineOfSight(target);
        return contactStillBlocked && goalDistanceGain < MIN_GOAL_DISTANCE_GAIN;
    }

    /**
     * Promotes the opposite endpoint when the current driver cannot make
     * progress. Reversing the complete index order preserves every hard joint;
     * it does not teleport, recreate, or disconnect any section.
     */
    private boolean reverseDriverWhenStuck(List<GiantCentipedeEntity> chain,
                                           @Nullable Player target, Vec3 destination) {
        if (chain.size() < 2) return false;

        // If control already moved away from the other endpoint recently,
        // both ends have now failed at the same route. Close the old ends into
        // a loop and reopen the chain elsewhere instead of swapping forever.
        if (this.lastStalledHead != null
                && !this.lastStalledHead.equals(this.getUUID())
                && this.tickCount - this.lastHeadStallTick <= SECOND_HEAD_STALL_WINDOW) {
            if (beginSelfReconnect(chain)) return true;
        }
        if (this.tickCount < this.nextDriverReversalTick) return false;

        UUID retainedChainId = this.chainId;
        UUID stalledHead = this.getUUID();
        int stalledAt = this.tickCount;
        Collections.reverse(chain);
        for (GiantCentipedeEntity part : chain) {
            part.getNavigation().stop();
            part.setDeltaMovement(Vec3.ZERO);
            part.setNoGravity(true);
            part.entityData.set(MOVING, false);
            part.vanillaPathfindingUntil = 0;
            part.clearVerticalRoute();
            part.watchedTarget = null;
            part.progressWindowGoal = Vec3.ZERO;
            part.progressWindowTargetDistance = Double.NaN;
            part.nextProgressCheck = part.tickCount + 20;
            // A short cooldown prevents rapid oscillation, but still lets the
            // other endpoint take over quickly if it is trapped as well.
            part.nextDriverReversalTick = part.tickCount + 40;
            part.driverWatchInitialized = false;
            part.lastStalledHead = stalledHead;
            part.lastHeadStallTick = stalledAt;
        }
        assignChain(chain, retainedChainId);

        GiantCentipedeEntity newDriver = chain.get(0);
        newDriver.setTarget(target);
        newDriver.watchedTarget = target == null ? null : target.getUUID();
        newDriver.progressWindowGoal = destination;
        newDriver.progressWindowTargetDistance = target == null
                ? Double.NaN : newDriver.position().distanceTo(destination);
        newDriver.nextProgressCheck = newDriver.tickCount + 20;
        newDriver.driverWatchPosition = newDriver.position();
        newDriver.nextDriverMovementCheck = newDriver.tickCount + DRIVER_STUCK_CHECK_TICKS;
        newDriver.driverWatchInitialized = true;
        Vec3 towardTarget = destination.subtract(newDriver.position());
        if (towardTarget.lengthSqr() > 1.0E-5D) newDriver.faceMovement(towardTarget);
        return true;
    }

    /**
     * Mode-independent stuck detection. This compares world position over a
     * full second, so animation, velocity, turning in place, and collision
     * jitter cannot be mistaken for useful movement.
     */
    private void recoverFromDriverStall(ServerLevel level) {
        if (!this.isDriver() || this.selfReconnectMode) return;
        if (!this.driverWatchInitialized) {
            this.driverWatchPosition = this.position();
            this.nextDriverMovementCheck = this.tickCount + DRIVER_STUCK_CHECK_TICKS;
            this.driverWatchInitialized = true;
            return;
        }
        if (this.tickCount < this.nextDriverMovementCheck) return;

        double displacementSqr = this.position().distanceToSqr(this.driverWatchPosition);
        this.driverWatchPosition = this.position();
        this.nextDriverMovementCheck = this.tickCount + DRIVER_STUCK_CHECK_TICKS;

        boolean commandedToMove = this.getDeltaMovement().lengthSqr() > 0.01D
                || !this.getNavigation().isDone();
        if (!commandedToMove || displacementSqr >= MIN_DRIVER_DISPLACEMENT_SQR) {
            return;
        }

        List<GiantCentipedeEntity> chain = loadedChain(level, this.chainId);
        if (breakBlocksTrappingChain(level, chain)) {
            // Give the current head a fresh progress window after opening the
            // obstruction instead of immediately reversing the whole chain.
            this.driverWatchPosition = this.position();
            this.nextDriverMovementCheck = this.tickCount + DRIVER_STUCK_CHECK_TICKS;
            return;
        }
        if (nearestSupportDirection(level) == null) return;

        Player playerTarget = this.getTarget() instanceof Player player ? player : null;
        BlockPos stalledLight = this.lightTarget == null ? null : this.lightTarget.immutable();
        Vec3 destination;
        if (playerTarget != null) {
            destination = playerTarget.getEyePosition();
        } else if (stalledLight != null) {
            destination = Vec3.atCenterOf(stalledLight);
        } else {
            Vec3 forward = this.getLookAngle();
            destination = this.position().add(forward.x * 4.0D, 0.0D, forward.z * 4.0D);
        }

        if (!reverseDriverWhenStuck(chain, playerTarget, destination)) {
            // During the brief reversal cooldown, cancel a failed path and
            // force a fresh wandering direction instead of pushing forever.
            this.getNavigation().stop();
            this.wanderDirection = Vec3.ZERO;
            this.nextWanderTurn = 0;
            return;
        }

        GiantCentipedeEntity newDriver = loadedChain(level, this.chainId).stream()
                .filter(GiantCentipedeEntity::isDriver)
                .findFirst().orElse(null);
        if (newDriver != null) {
            newDriver.wanderDirection = Vec3.ZERO;
            newDriver.nextWanderTurn = 0;
            if (stalledLight != null) {
                newDriver.rejectedLightTarget = stalledLight;
                newDriver.rejectedLightUntil = newDriver.tickCount + 200;
                newDriver.nextLightScan = newDriver.tickCount + 1;
            }
        }
    }

    /** Starts a timed break for one destructible block overlapping the chain. */
    private boolean breakBlocksTrappingChain(ServerLevel level, List<GiantCentipedeEntity> chain) {
        if (!EventHooks.canEntityGrief(level, this)) return false;
        if (this.breakingBlock != null) return true;

        for (GiantCentipedeEntity part : chain) {
            AABB sectionBox = part.getBoundingBox().deflate(0.035D);
            int minX = Mth.floor(sectionBox.minX);
            int minY = Mth.floor(sectionBox.minY);
            int minZ = Mth.floor(sectionBox.minZ);
            int maxX = Mth.floor(sectionBox.maxX - 1.0E-7D);
            int maxY = Mth.floor(sectionBox.maxY - 1.0E-7D);
            int maxZ = Mth.floor(sectionBox.maxZ - 1.0E-7D);

            for (BlockPos position : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
                BlockState state = level.getBlockState(position);
                if (state.isAir() || state.hasBlockEntity()
                        || state.getDestroySpeed(level, position) < 0.0F
                        || !state.canEntityDestroy(level, position, this)) {
                    continue;
                }

                boolean intersects = state.getCollisionShape(level, position).toAabbs().stream()
                        .map(box -> box.move(position.getX(), position.getY(), position.getZ()))
                        .anyMatch(sectionBox::intersects);
                if (!intersects) continue;

                this.breakingBlock = position.immutable();
                this.breakingBlockTicks = 0;
                this.getNavigation().stop();
                this.setDeltaMovement(Vec3.ZERO);
                return true;
            }
        }
        return false;
    }

    /** Advances one block from intact to broken over exactly twenty server ticks. */
    private boolean tickBreakingBlock(ServerLevel level) {
        if (this.breakingBlock == null) return false;
        BlockPos pos = this.breakingBlock;
        BlockState state = level.getBlockState(pos);
        if (!EventHooks.canEntityGrief(level, this)
                || state.isAir() || state.hasBlockEntity()
                || state.getDestroySpeed(level, pos) < 0.0F
                || !state.canEntityDestroy(level, pos, this)) {
            clearBreakingBlock(level);
            return false;
        }

        this.breakingBlockTicks++;
        int crackStage = Math.min(9, this.breakingBlockTicks * 10 / BLOCK_BREAK_TICKS);
        level.destroyBlockProgress(this.getId(), pos, crackStage);
        if (this.breakingBlockTicks < BLOCK_BREAK_TICKS) return true;

        if (EventHooks.onEntityDestroyBlock(this, pos, state)) {
            // false means the block is removed without producing item drops.
            if (level.destroyBlock(pos, false, this)) weatherSurroundingStone(level, pos);
        }
        clearBreakingBlock(level);
        return true;
    }

    private void weatherSurroundingStone(ServerLevel level, BlockPos broken) {
        if (!EventHooks.canEntityGrief(level, this)) return;
        for (BlockPos pos : BlockPos.betweenClosed(broken.offset(-1, -1, -1), broken.offset(1, 1, 1))) {
            if (pos.equals(broken) || !level.hasChunkAt(pos)
                    || !level.getWorldBorder().isWithinBounds(pos)) continue;
            BlockState state = level.getBlockState(pos);
            BlockState replacement;
            if (state.is(Blocks.STONE_BRICKS) || state.is(Blocks.INFESTED_STONE_BRICKS)) {
                replacement = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            } else if (state.is(Blocks.STONE)
                    || state.is(net.beamex.shamaschizm.world.block.SchizmStoneRegistration.SCHIZM_STONE)) {
                replacement = Blocks.COBBLESTONE.defaultBlockState();
            } else continue;
            if (state.canEntityDestroy(level, pos, this)
                    && EventHooks.onEntityDestroyBlock(this, pos, state)) {
                level.setBlock(pos, replacement, 3);
            }
        }
    }

    private void clearBreakingBlock(ServerLevel level) {
        if (this.breakingBlock != null) {
            level.destroyBlockProgress(this.getId(), this.breakingBlock, -1);
        }
        this.breakingBlock = null;
        this.breakingBlockTicks = 0;
    }

    private boolean beginSelfReconnect(List<GiantCentipedeEntity> chain) {
        if (this.level() instanceof ServerLevel level && blocksRecombination(level, chain)) return false;
        if (chain.size() < 5) {
            // There are not enough interior sections to create two genuinely
            // new endpoint heads. Fall back to the normal head swap.
            this.lastStalledHead = null;
            this.lastHeadStallTick = 0;
            return false;
        }
        for (GiantCentipedeEntity part : chain) {
            part.selfReconnectMode = true;
            part.selfReconnectUntil = part.tickCount + SELF_RECONNECT_DURATION;
            part.selfReconnectSwapTick = part.tickCount + SELF_RECONNECT_HEAD_TURN;
            part.lastStalledHead = null;
            part.lastHeadStallTick = 0;
            part.driverWatchInitialized = false;
            part.getNavigation().stop();
            part.setDeltaMovement(Vec3.ZERO);
        }
        return true;
    }

    /**
     * The endpoint heads take turns navigating toward one another. Once the
     * old endpoints touch, the ordered chain is rotated around an interior
     * break: the old heads become body sections and the two sections bordering
     * the new break become the replacement heads.
     */
    private void tickSelfReconnect(ServerLevel level, List<GiantCentipedeEntity> chain) {
        if (chain.size() < 5 || this.tickCount >= this.selfReconnectUntil) {
            clearSelfReconnect(chain);
            this.wanderDirection = Vec3.ZERO;
            this.nextWanderTurn = 0;
            return;
        }
        if (breakBlocksTrappingChain(level, chain)) return;

        GiantCentipedeEntity otherHead = chain.get(chain.size() - 1);
        if (this.distanceToSqr(otherHead) <= SELF_RECONNECT_DISTANCE_SQR) {
            reopenChainAtInteriorLink(chain);
            return;
        }

        if (this.tickCount >= this.selfReconnectSwapTick) {
            swapSelfReconnectDriver(chain);
            return;
        }

        Vec3 towardOtherHead = otherHead.position().subtract(this.position());
        if (this.hasLineOfSight(otherHead)) {
            this.getNavigation().stop();
            this.setNoGravity(true);
            Vec3 desired = surfaceDirection(towardOtherHead, supportNormal()).scale(0.38D);
            desired = transitionOntoObstacle(level, desired, towardOtherHead, 0.38D)
                    .add(supportNormal().scale(-0.05D));
            moveChainPart(desired, 0.46D);
            faceDriverFromDistances(level, desired);
        } else {
            this.setNoGravity(false);
            if (this.tickCount % 10 == 0 || this.getNavigation().isDone()) {
                this.getNavigation().moveTo(otherHead, 1.45D);
            }
            this.getLookControl().setLookAt(otherHead, 30.0F, 30.0F);
        }
    }

    private void swapSelfReconnectDriver(List<GiantCentipedeEntity> chain) {
        UUID retainedChainId = this.chainId;
        Collections.reverse(chain);
        for (GiantCentipedeEntity part : chain) {
            part.getNavigation().stop();
            part.setDeltaMovement(Vec3.ZERO);
            part.setNoGravity(true);
            part.selfReconnectMode = true;
        }
        assignChain(chain, retainedChainId);
        GiantCentipedeEntity newDriver = chain.get(0);
        newDriver.selfReconnectSwapTick = newDriver.tickCount + SELF_RECONNECT_HEAD_TURN;
    }

    private void reopenChainAtInteriorLink(List<GiantCentipedeEntity> chain) {
        int minimumBreak = 1;
        int maximumBreak = chain.size() - 3;
        int center = (minimumBreak + maximumBreak) / 2;
        int variation = Math.min(2, Math.max(0, (maximumBreak - minimumBreak) / 2));
        int breakAfter = Mth.clamp(center + (variation == 0
                ? 0 : this.random.nextInt(variation * 2 + 1) - variation),
                minimumBreak, maximumBreak);

        List<GiantCentipedeEntity> reordered = new ArrayList<>(chain.size());
        reordered.addAll(chain.subList(breakAfter + 1, chain.size()));
        reordered.addAll(chain.subList(0, breakAfter + 1));
        assignChain(reordered, this.chainId);
        clearSelfReconnect(reordered);

        for (GiantCentipedeEntity part : reordered) {
            part.getNavigation().stop();
            part.setDeltaMovement(Vec3.ZERO);
            part.driverWatchInitialized = false;
            part.wanderDirection = Vec3.ZERO;
            part.nextWanderTurn = 0;
        }
    }

    private static void clearSelfReconnect(List<GiantCentipedeEntity> chain) {
        for (GiantCentipedeEntity part : chain) {
            part.selfReconnectMode = false;
            part.selfReconnectUntil = 0;
            part.selfReconnectSwapTick = 0;
            part.lastStalledHead = null;
            part.lastHeadStallTick = 0;
            part.getNavigation().stop();
        }
    }

    private void beginVanillaPathfinding(Player target) {
        // Four seconds gives the ground navigator time to drop a wall- or
        // ceiling-bound driver onto a floor and route it around the obstacle.
        this.vanillaPathfindingUntil = this.tickCount + 80;
        this.setNoGravity(false);
        // Release the driver from the surface it was pressing into. Without
        // this small outward impulse, collision can keep the navigation node
        // on the wrong side of a corner.
        this.setDeltaMovement(supportNormal().scale(0.12D));
        this.getNavigation().stop();
        this.getNavigation().moveTo(target, 1.35D);
    }

    /**
     * Temporarily hands control to the inherited ground navigation, matching a
     * zombie-style pathfinder. Gravity is restored during this mode so a
     * wall-stuck driver can return to a valid floor path. Fall damage remains
     * disabled by causeFallDamage().
     */
    private boolean tickVanillaPathfinding(ServerLevel level, Player target) {
        if (this.vanillaPathfindingUntil <= 0) return false;
        if (this.tickCount >= this.vanillaPathfindingUntil) {
            finishVanillaPathfinding();
            return false;
        }
        this.setNoGravity(false);
        if (this.tickCount % 10 == 0 || this.getNavigation().isDone()) {
            this.getNavigation().moveTo(target, 1.35D);
        }
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        tryContactAttack(level, target);
        return true;
    }

    private void finishVanillaPathfinding() {
        this.vanillaPathfindingUntil = 0;
        this.getNavigation().stop();
        this.setNoGravity(true);
        this.progressWindowGoal = Vec3.ZERO;
        this.progressWindowTargetDistance = Double.NaN;
        this.nextProgressCheck = this.tickCount + 20;
    }

    private void resetPursuitWatch() {
        this.watchedTarget = null;
        this.progressWindowGoal = Vec3.ZERO;
        this.progressWindowTargetDistance = Double.NaN;
        this.nextProgressCheck = 0;
        clearVerticalRoute();
        if (this.vanillaPathfindingUntil > 0) finishVanillaPathfinding();
    }

    /** All chains join opportunistically when two endpoint heads touch. */
    private boolean mergeIfTouchingAnotherChain(ServerLevel level, List<GiantCentipedeEntity> ownChain) {
        for (GiantCentipedeEntity ownHead : ownChain) {
            if (!ownHead.isHead() || !ownHead.isAlive()) continue;
            for (GiantCentipedeEntity otherHead : level.getEntitiesOfClass(
                    GiantCentipedeEntity.class, ownHead.getBoundingBox().inflate(1.35D),
                    candidate -> candidate.isHead() && candidate.isAlive()
                            && !candidate.chainId.equals(this.chainId))) {
                if (ownHead.distanceToSqr(otherHead) <= 1.35D * 1.35D) {
                    if (mergeAtHeads(level, ownHead, otherHead)) return true;
                }
            }
        }
        return false;
    }

    /** Uses the same torch/candle definition and mob-griefing hooks as the Schizm zombies. */
    private boolean tickTorchHunt(ServerLevel level) {
        boolean nearbyPlayer = level.players().stream().anyMatch(player -> player.isAlive()
                && !player.isCreative() && !player.isSpectator() && player.distanceToSqr(this) <= 64.0D);
        if (nearbyPlayer) {
            this.lightTarget = null;
            this.lightEatTicks = 0;
            resetLightProgress();
            return false;
        }

        if (this.lightTarget != null && (!validLight(level, this.lightTarget)
                || !visibleLight(level, this.lightTarget))) {
            this.lightTarget = null;
            this.lightEatTicks = 0;
            resetLightProgress();
        }
        if (this.lightTarget == null) {
            if (this.tickCount < this.nextLightScan || !EventHooks.canEntityGrief(level, this)) return false;
            this.nextLightScan = this.tickCount + 80 + this.random.nextInt(41);
            if (this.tickCount >= this.rejectedLightUntil) this.rejectedLightTarget = null;
            BlockPos center = this.blockPosition();
            double best = 10.0D * 10.0D;
            for (BlockPos candidate : BlockPos.betweenClosed(
                    center.offset(-10, -10, -10), center.offset(10, 10, 10))) {
                if (this.rejectedLightTarget != null && candidate.equals(this.rejectedLightTarget)) continue;
                double distance = candidate.distSqr(center);
                if (distance >= best || !validLight(level, candidate) || !visibleLight(level, candidate)) continue;
                best = distance;
                this.lightTarget = candidate.immutable();
            }
        }
        if (this.lightTarget == null) return false;

        Vec3 destination = Vec3.atCenterOf(this.lightTarget);
        Vec3 toLight = destination.subtract(this.position());
        if (toLight.lengthSqr() <= 2.0D * 2.0D) {
            resetLightProgress();
            this.setDeltaMovement(Vec3.ZERO);
            if (++this.lightEatTicks >= BLOCK_BREAK_TICKS) consumeLight(level, this.lightTarget);
            return true;
        }

        // A target directly above/below the current surface has no tangential
        // direction. Continuing would command a zero vector forever beneath
        // ceiling or ledge torches, so reject it immediately instead.
        Vec3 lightSurfaceNormal = supportNormal();
        Vec3 rawSurfaceDirection = toLight.subtract(
                lightSurfaceNormal.scale(toLight.dot(lightSurfaceNormal)));
        if (rawSurfaceDirection.lengthSqr() < 0.01D) {
            rejectCurrentLight();
            return true;
        }

        if (isLightHuntStuck(destination)) {
            // Swapping heads cannot solve this case: both endpoints would aim
            // at the same unreachable block (commonly the floor directly below
            // a high torch). Reject this light for ten seconds and let the same
            // driver select a different light on the following tick.
            rejectCurrentLight();
            return true;
        }

        this.lightEatTicks = 0;
        Vec3 normal = supportNormal();
        Vec3 desired = surfaceDirection(toLight, normal).scale(0.37D);
        desired = transitionOntoObstacle(level, desired, toLight, 0.37D)
                .add(supportNormal().scale(-0.05D));
        moveChainPart(desired, 0.42D);
        faceDriverFromDistances(level, desired);
        return true;
    }

    private void rejectCurrentLight() {
        if (this.lightTarget != null) this.rejectedLightTarget = this.lightTarget.immutable();
        this.rejectedLightUntil = this.tickCount + 200;
        this.lightTarget = null;
        this.lightEatTicks = 0;
        this.nextLightScan = this.tickCount + 1;
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        resetLightProgress();
    }

    private boolean isLightHuntStuck(Vec3 destination) {
        if (this.lightTarget == null) return false;
        if (!this.lightTarget.equals(this.watchedLightProgressTarget)
                || !Double.isFinite(this.lightProgressDistance)) {
            this.watchedLightProgressTarget = this.lightTarget.immutable();
            this.lightProgressGoal = destination;
            this.lightProgressDistance = this.position().distanceTo(destination);
            this.nextLightProgressCheck = this.tickCount + 20;
            return false;
        }
        if (this.tickCount < this.nextLightProgressCheck) return false;
        double goalDistanceGain = this.lightProgressDistance
                - this.position().distanceTo(this.lightProgressGoal);
        this.lightProgressGoal = destination;
        this.lightProgressDistance = this.position().distanceTo(destination);
        this.nextLightProgressCheck = this.tickCount + 20;
        return goalDistanceGain < MIN_GOAL_DISTANCE_GAIN;
    }

    private void resetLightProgress() {
        this.watchedLightProgressTarget = null;
        this.lightProgressGoal = Vec3.ZERO;
        this.lightProgressDistance = Double.NaN;
        this.nextLightProgressCheck = 0;
    }

    private boolean validLight(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) return false;
        BlockState state = level.getBlockState(pos);
        boolean light = state.getBlock() instanceof BaseTorchBlock || AbstractCandleBlock.isLit(state);
        return light && !state.hasBlockEntity() && state.getDestroySpeed(level, pos) >= 0.0F
                && state.canEntityDestroy(level, pos, this);
    }

    private boolean visibleLight(ServerLevel level, BlockPos pos) {
        var hit = level.clip(new ClipContext(this.getEyePosition(), Vec3.atCenterOf(pos),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    private void consumeLight(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (validLight(level, pos) && EventHooks.canEntityGrief(level, this)
                && EventHooks.onEntityDestroyBlock(this, pos, state)) {
            if (state.getBlock() instanceof AbstractCandleBlock) {
                if (AbstractCandleBlock.isLit(state)) AbstractCandleBlock.extinguish(null, state, level, pos);
            } else {
                if (level.destroyBlock(pos, false, this)) weatherSurroundingStone(level, pos);
            }
        }
        this.lightTarget = null;
        this.lightEatTicks = 0;
        resetLightProgress();
        this.nextLightScan = this.tickCount + 40;
    }

    private void tickWander(ServerLevel level) {
        Vec3 normal = supportNormal();
        if (this.tickCount >= this.nextWanderTurn || this.wanderDirection.lengthSqr() < 0.01D) {
            Vec3 randomDirection = new Vec3(
                    this.random.nextDouble() * 2.0D - 1.0D,
                    this.random.nextDouble() * 2.0D - 1.0D,
                    this.random.nextDouble() * 2.0D - 1.0D);
            randomDirection = randomDirection.subtract(normal.scale(randomDirection.dot(normal)));
            if (randomDirection.lengthSqr() < 0.01D) {
                randomDirection = Math.abs(normal.y) > 0.8D
                        ? new Vec3(1.0D, 0.0D, 0.0D)
                        : normal.cross(new Vec3(0.0D, 1.0D, 0.0D));
            }
            this.wanderDirection = randomDirection.normalize();
            this.nextWanderTurn = this.tickCount + 45 + this.random.nextInt(76);
        }

        Vec3 surfaceWander = surfaceDirection(this.wanderDirection, normal);
        Direction oldSupport = getSupportDirection();
        Vec3 desired = transitionOntoObstacle(level,
                surfaceWander.scale(WANDER_SPEED), surfaceWander, WANDER_SPEED);
        if (getSupportDirection() != oldSupport) {
            this.nextWanderTurn = 0;
        }
        desired = desired.add(supportNormal().scale(-0.05D));
        moveChainPart(desired, 0.38D);
        faceDriverFromDistances(level, desired);
    }

    private boolean tickReconnect(ServerLevel level, List<GiantCentipedeEntity> ownChain) {
        if (blocksRecombination(level, ownChain)) {
            clearReconnectNavigation();
            return false;
        }
        GiantCentipedeEntity nearestHead = null;
        double best = RECONNECT_RANGE * RECONNECT_RANGE;
        for (GiantCentipedeEntity candidate : level.getEntitiesOfClass(GiantCentipedeEntity.class,
                this.getBoundingBox().inflate(RECONNECT_RANGE),
                p -> p.isHead() && !p.chainId.equals(this.chainId) && p.isAlive())) {
            if (blocksRecombination(level, loadedChain(level, candidate.chainId))) continue;
            double distance = this.distanceToSqr(candidate);
            if (distance < best) {
                best = distance;
                nearestHead = candidate;
            }
        }
        if (nearestHead == null) {
            clearReconnectNavigation();
            return false;
        }
        Vec3 towardHead = nearestHead.position().subtract(this.position());
        if (best <= 2.4D * 2.4D) {
            clearReconnectNavigation();
            return mergeAtHeads(level, this, nearestHead);
        }

        boolean newHead = !nearestHead.getUUID().equals(this.reconnectHead);
        if (newHead || !Double.isFinite(this.reconnectDistance)) {
            this.reconnectHead = nearestHead.getUUID();
            this.reconnectDistance = Math.sqrt(best);
            this.nextReconnectProgressCheck = this.tickCount + 20;
            this.reconnectNavigationUntil = 0;
        } else if (this.tickCount >= this.nextReconnectProgressCheck) {
            double distance = Math.sqrt(best);
            double progress = this.reconnectDistance - distance;
            this.reconnectDistance = distance;
            this.nextReconnectProgressCheck = this.tickCount + 20;
            if (progress < RECONNECT_MIN_PROGRESS) {
                // Direct crawling cannot route around dungeon corners. Give
                // the inherited ground navigator three seconds to find a
                // route, then reassess direct surface crawling.
                this.reconnectNavigationUntil = this.tickCount + 60;
                this.getNavigation().stop();
            }
        }

        if (!this.hasLineOfSight(nearestHead)) {
            this.reconnectNavigationUntil = Math.max(this.reconnectNavigationUntil, this.tickCount + 40);
        }
        if (this.tickCount < this.reconnectNavigationUntil) {
            this.setNoGravity(false);
            if (this.tickCount % 10 == 0 || this.getNavigation().isDone()) {
                this.getNavigation().moveTo(nearestHead, 1.45D);
            }
            this.getLookControl().setLookAt(nearestHead, 30.0F, 30.0F);
            return true;
        }

        this.getNavigation().stop();
        this.setNoGravity(true);
        Vec3 desired = towardHead;
        desired = surfaceDirection(desired, supportNormal()).scale(0.34D);
        desired = transitionOntoObstacle(level, desired, towardHead, 0.34D);
        moveChainPart(desired.add(supportNormal().scale(-0.05D)), 0.46D);
        faceDriverFromDistances(level, desired);
        return true;
    }

    private void clearReconnectNavigation() {
        this.reconnectHead = null;
        this.reconnectDistance = Double.NaN;
        this.nextReconnectProgressCheck = 0;
        this.reconnectNavigationUntil = 0;
        this.getNavigation().stop();
    }

    private void fleeNearestPlayer(ServerLevel level) {
        Player player = level.getNearestPlayer(this, 40.0D);
        if (player == null || player.isCreative() || player.isSpectator()) {
            tickWander(level);
            return;
        }
        Vec3 away = this.position().subtract(player.position());
        away = surfaceDirection(away, supportNormal()).scale(0.46D);
        away = transitionOntoObstacle(level, away, away, 0.46D);
        moveChainPart(away.add(supportNormal().scale(-0.05D)), 0.50D);
        faceDriverFromDistances(level, away);
    }

    private static void solveFollowerJoints(ServerLevel level, List<GiantCentipedeEntity> chain) {
        for (int index = 1; index < chain.size(); index++) {
            chain.get(index).tickFollower(level, chain);
        }
    }

    private void tickFollower(ServerLevel level, List<GiantCentipedeEntity> chain) {
        GiantCentipedeEntity previous = null;
        GiantCentipedeEntity next = null;
        for (GiantCentipedeEntity part : chain) {
            if (part.getPartIndex() == this.getPartIndex() - 1) {
                previous = part;
            }
            if (part.getPartIndex() == this.getPartIndex() + 1) next = part;
        }
        if (previous == null) return;
        boolean rearHead = this.isHead() && this.getPartIndex() == chain.size() - 1;
        // A follower is a hard joint, not a pursuing mob. Reposition it to the
        // exact exported section length every tick so acceleration and corners
        // can never pull visible gaps into the chain.
        Vec3 outward = this.position().subtract(previous.position());
        if (outward.lengthSqr() < 1.0E-5D) {
            outward = previous.getLookAngle().multiply(-1.0D, 0.0D, -1.0D);
        }
        if (outward.lengthSqr() < 1.0E-5D) outward = new Vec3(0.0D, 0.0D, -1.0D);
        outward = outward.normalize();
        Vec3 attached = previous.position().add(outward.scale(PART_SPACING));
        this.setPos(attached.x, attached.y, attached.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.entityData.set(MOVING, previous.isChainMoving());

        Direction nearestSurface = nearestSupportDirection(level);
        if (nearestSurface != null) this.entityData.set(SUPPORT, nearestSurface);

        Vec3 facing;
        if (rearHead) {
            facing = outward;
        } else {
            Vec3 towardFront = previous.position().subtract(attached);
            Vec3 awayFromRear = next == null ? towardFront : attached.subtract(next.position());
            Vec3 positionalFacing = safeNormal(towardFront).add(safeNormal(awayFromRear));
            Vec3 neighborRotation = previous.getLookAngle();
            if (next != null) neighborRotation = neighborRotation.add(next.getLookAngle());
            facing = safeNormal(positionalFacing).scale(0.62D)
                    .add(safeNormal(neighborRotation).scale(0.38D));
        }

        // Neighbor positions and rotations define the main chain tangent.
        // The nearest surface in every cardinal direction then contributes a
        // wall-parallel tangent with weight approximately proportional to 1/d.
        // No discrete SUPPORT direction participates in visual orientation.
        facing = orientFromNeighborsAndSurfaces(level, facing);
        faceMovement(facing);
        updateVisualRoll(level, previous, next);
    }

    private static Vec3 safeNormal(Vec3 vector) {
        return vector.lengthSqr() < 1.0E-5D ? Vec3.ZERO : vector.normalize();
    }

    private void faceDriverFromDistances(ServerLevel level, Vec3 movementFacing) {
        Vec3 facing = safeNormal(movementFacing);
        GiantCentipedeEntity firstFollower = null;
        for (GiantCentipedeEntity part : loadedChain(level, this.chainId)) {
            if (part.getPartIndex() != 1) continue;
            firstFollower = part;
            Vec3 awayFromFirstFollower = safeNormal(this.position().subtract(part.position()));
            if (awayFromFirstFollower.lengthSqr() >= 1.0E-5D) {
                facing = safeNormal(facing.scale(0.72D)
                        .add(awayFromFirstFollower.scale(0.28D)));
            }
            break;
        }
        faceMovement(orientFromNeighborsAndSurfaces(level, facing));
        updateVisualRoll(level, firstFollower, null);
    }

    private Vec3 orientFromNeighborsAndSurfaces(ServerLevel level, Vec3 neighborFacing) {
        Vec3 base = safeNormal(neighborFacing);
        if (base.lengthSqr() < 1.0E-5D) return base;

        Vec3 center = this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
        Vec3 combined = base.scale(1.65D);
        Vec3 previousFacing = safeNormal(this.getLookAngle());
        final int searchBlocks = 4;
        final double distanceOffset = 0.18D;
        final double maximumSurfaceWeight = 4.5D;

        for (Direction direction : Direction.values()) {
            Vec3 towardSurface = Vec3.atLowerCornerOf(direction.getUnitVec3i());
            double gap = nearestSurfaceGap(level, center, direction, searchBlocks);
            if (!Double.isFinite(gap)) continue;
            double weight = Math.min(maximumSurfaceWeight, 1.0D / (gap + distanceOffset));

            Vec3 surfaceNormal = towardSurface.scale(-1.0D);
            Vec3 tangent = base.subtract(surfaceNormal.scale(base.dot(surfaceNormal)));
            if (tangent.lengthSqr() < 1.0E-5D) {
                tangent = previousFacing.subtract(surfaceNormal.scale(previousFacing.dot(surfaceNormal)));
            }
            if (tangent.lengthSqr() < 1.0E-5D) {
                tangent = surfaceDirection(base, surfaceNormal);
            }
            if (tangent.lengthSqr() >= 1.0E-5D) {
                combined = combined.add(tangent.normalize().scale(weight));
            }
        }

        return safeNormal(combined);
    }

    private void updateVisualRoll(ServerLevel level, @Nullable GiantCentipedeEntity firstNeighbor,
                                  @Nullable GiantCentipedeEntity secondNeighbor) {
        Vec3 forward = safeNormal(this.getLookAngle());
        if (forward.lengthSqr() < 1.0E-5D) return;

        Vec3 center = this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
        Vec3 weightedOutwardNormal = Vec3.ZERO;
        double totalSurfaceWeight = 0.0D;
        final int searchBlocks = 4;
        final double distanceOffset = 0.18D;
        final double maximumSurfaceWeight = 4.5D;

        for (Direction direction : Direction.values()) {
            double gap = nearestSurfaceGap(level, center, direction, searchBlocks);
            if (!Double.isFinite(gap)) continue;
            double weight = Math.min(maximumSurfaceWeight, 1.0D / (gap + distanceOffset));
            Vec3 outwardNormal = Vec3.atLowerCornerOf(
                    direction.getOpposite().getUnitVec3i());
            weightedOutwardNormal = weightedOutwardNormal.add(outwardNormal.scale(weight));
            totalSurfaceWeight += weight;
        }

        float current = this.entityData.get(VISUAL_ROLL);
        float neighborRoll = current;
        if (firstNeighbor != null && secondNeighbor != null) {
            neighborRoll = Mth.rotLerp(0.5F, firstNeighbor.entityData.get(VISUAL_ROLL),
                    secondNeighbor.entityData.get(VISUAL_ROLL));
        } else if (firstNeighbor != null) {
            neighborRoll = firstNeighbor.entityData.get(VISUAL_ROLL);
        } else if (secondNeighbor != null) {
            neighborRoll = secondNeighbor.entityData.get(VISUAL_ROLL);
        }

        float target = neighborRoll;
        if (totalSurfaceWeight > 1.0E-5D) {
            Vec3 desiredUp = weightedOutwardNormal
                    .subtract(forward.scale(weightedOutwardNormal.dot(forward)));
            if (desiredUp.lengthSqr() >= 1.0E-5D) {
                desiredUp = desiredUp.normalize();
                Vec3 referenceUp = new Vec3(0.0D, 1.0D, 0.0D)
                        .subtract(forward.scale(forward.y));
                if (referenceUp.lengthSqr() < 0.04D) {
                    referenceUp = Vec3.directionFromRotation(0.0F, this.getYRot());
                    referenceUp = referenceUp.subtract(forward.scale(referenceUp.dot(forward)));
                }
                if (referenceUp.lengthSqr() >= 1.0E-5D) {
                    referenceUp = referenceUp.normalize();
                    double sine = forward.dot(referenceUp.cross(desiredUp));
                    double cosine = referenceUp.dot(desiredUp);
                    float surfaceRoll = (float)(Mth.atan2(sine, cosine) * Mth.RAD_TO_DEG);
                    target = Mth.rotLerp(firstNeighbor == null && secondNeighbor == null ? 1.0F : 0.72F,
                            neighborRoll, surfaceRoll);
                }
            }
        }

        this.entityData.set(VISUAL_ROLL, Mth.rotLerp(0.24F, current, target));
    }

    private double nearestSurfaceGap(ServerLevel level, Vec3 center, Direction direction,
                                     int searchBlocks) {
        for (int step = 1; step <= searchBlocks; step++) {
            BlockPos candidate = this.blockPosition().relative(direction, step);
            if (!validSupportSurface(level, candidate, direction)) continue;
            Vec3 towardSurface = Vec3.atLowerCornerOf(direction.getUnitVec3i());
            double centerSeparation = Math.abs(Vec3.atCenterOf(candidate)
                    .subtract(center).dot(towardSurface));
            double sectionHalfExtent = direction.getAxis().isVertical()
                    ? this.getBbHeight() * 0.5D
                    : this.getBbWidth() * 0.5D;
            return Math.max(0.0D, centerSeparation - 0.5D - sectionHalfExtent);
        }
        return Double.POSITIVE_INFINITY;
    }

    private void moveChainPart(Vec3 desired, double cap) {
        Vec3 current = this.getDeltaMovement();
        Vec3 movement = new Vec3(Mth.lerp(0.35D, current.x, desired.x),
                Mth.lerp(0.35D, current.y, desired.y), Mth.lerp(0.35D, current.z, desired.z));
        if (movement.length() > cap) movement = movement.normalize().scale(cap);
        this.setDeltaMovement(movement);
    }

    private void updateSupport(ServerLevel level) {
        Direction best = nearestSupportDirection(level);
        if (best != null) {
            this.entityData.set(SUPPORT, best);
            this.unsupportedTicks = 0;
        } else if (this.isDriver()) {
            this.unsupportedTicks++;
            // Apply Minecraft-like accelerating gravity ourselves because the
            // wall crawler normally keeps noGravity enabled. The former fixed
            // -0.08 velocity made unsupported chains glide at constant speed.
            Vec3 movement = this.getDeltaMovement();
            double fallingY = Math.max(-1.50D, (movement.y - 0.08D) * 0.98D);
            double horizontalDrag = this.unsupportedTicks > 2 ? 0.91D : 0.98D;
            this.setDeltaMovement(movement.x * horizontalDrag, fallingY, movement.z * horizontalDrag);
        }
    }

    private @Nullable Direction nearestSupportDirection(ServerLevel level) {
        Direction best = null;
        double bestDistance = Double.MAX_VALUE;
        Vec3 center = this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
        for (Direction direction : Direction.values()) {
            BlockPos candidate = this.blockPosition().relative(direction);
            if (!validSupportSurface(level, candidate, direction)) continue;
            double distance = center.distanceToSqr(Vec3.atCenterOf(candidate));
            if (distance < bestDistance) {
                best = direction;
                bestDistance = distance;
            }
        }
        return best;
    }

    /**
     * Transfers movement onto an encountered wall, floor, or ceiling in the
     * same tick. Projecting the old direction onto the new surface prevents a
     * section from spending a tick driving directly into the obstacle.
     */
    private Vec3 transitionOntoObstacle(ServerLevel level, Vec3 movement,
                                        Vec3 preferredDirection, double speed) {
        Direction obstacle = solidDirectionAhead(level, movement);
        if (obstacle == null) return movement;

        // WallBlock collision is narrower and taller than a full cube. Treating
        // its post or arms as a full climbable face makes the support direction
        // alternate every tick. Keep the current surface and route around the
        // actual wall collision instead.
        BlockPos obstaclePos = this.blockPosition().relative(obstacle);
        BlockState obstacleState = level.getBlockState(obstaclePos);
        BlockState occupiedState = level.getBlockState(this.blockPosition());
        if (obstacleState.getBlock() instanceof WallBlock
                || occupiedState.getBlock() instanceof WallBlock) {
            this.nextWanderTurn = 0;
            return steerAroundWallBlock(level, obstacle, preferredDirection, speed);
        }

        // A stair's side is not a wall-climbing face. Preserve floor support,
        // enable ordinary step-up physics, and continue across the stair.
        if (obstacle.getAxis().isHorizontal()
                && (obstacleState.getBlock() instanceof StairBlock
                || occupiedState.getBlock() instanceof StairBlock)) {
            this.setNoGravity(false);
            return movement.add(0.0D, 0.10D, 0.0D);
        }

        if (obstacle == getSupportDirection()) return movement;

        this.entityData.set(SUPPORT, obstacle);
        return surfaceDirection(preferredDirection, supportNormal()).scale(speed);
    }

    private Vec3 steerAroundWallBlock(ServerLevel level, Direction obstacle,
                                      Vec3 preferredDirection, double speed) {
        if (!obstacle.getAxis().isHorizontal()) return movementAwayFrom(obstacle).scale(speed);

        Vec3 intoWall = Vec3.atLowerCornerOf(obstacle.getUnitVec3i());
        Vec3 left = new Vec3(-intoWall.z, 0.0D, intoWall.x);
        Vec3 right = left.scale(-1.0D);
        boolean leftOpen = level.noBlockCollision(this,
                this.getBoundingBox().move(left.scale(0.55D)));
        boolean rightOpen = level.noBlockCollision(this,
                this.getBoundingBox().move(right.scale(0.55D)));

        Vec3 side;
        if (leftOpen && rightOpen) {
            double leftScore = preferredDirection.dot(left);
            double rightScore = preferredDirection.dot(right);
            if (Math.abs(leftScore - rightScore) > 0.05D) side = leftScore > rightScore ? left : right;
            else side = (this.chainId.hashCode() & 1) == 0 ? left : right;
        } else if (leftOpen) {
            side = left;
        } else if (rightOpen) {
            side = right;
        } else {
            side = intoWall.scale(-1.0D);
        }

        return side.normalize().scale(speed);
    }

    private static Vec3 movementAwayFrom(Direction direction) {
        return Vec3.atLowerCornerOf(direction.getOpposite().getUnitVec3i());
    }

    private @Nullable Direction solidDirectionAhead(ServerLevel level, Vec3 movement) {
        if (movement.lengthSqr() < 1.0E-5D) return null;
        Direction direction = Direction.getApproximateNearest(movement.x, movement.y, movement.z);
        return solid(level, this.blockPosition().relative(direction)) ? direction : null;
    }

    private static boolean solid(ServerLevel level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private static boolean validSupportSurface(ServerLevel level, BlockPos pos, Direction direction) {
        if (!solid(level, pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof WallBlock) return false;
        // Stairs are valid beneath or above a section, but their partial side
        // faces must never become a horizontal wall-crawling support.
        return !(state.getBlock() instanceof StairBlock) || direction.getAxis().isVertical();
    }

    private static boolean isFullClimbableSurface(BlockState state) {
        return !(state.getBlock() instanceof WallBlock)
                && !(state.getBlock() instanceof StairBlock);
    }

    private Vec3 supportNormal() {
        return Vec3.atLowerCornerOf(getSupportDirection().getOpposite().getUnitVec3i());
    }

    /** Returns a non-zero direction tangent to the current supporting surface. */
    private Vec3 surfaceDirection(Vec3 preferred, Vec3 normal) {
        Vec3 projected = preferred.subtract(normal.scale(preferred.dot(normal)));
        if (projected.lengthSqr() >= 0.01D) return projected.normalize();

        // If the target is directly through a wall, climb instead of pressing
        // into it. On floors and ceilings, retain the current horizontal
        // heading so the chain can route around the obstruction.
        Vec3 vertical = new Vec3(0.0D, preferred.y < -0.05D ? -1.0D : 1.0D, 0.0D);
        projected = vertical.subtract(normal.scale(vertical.dot(normal)));
        if (projected.lengthSqr() >= 0.01D) return projected.normalize();

        Vec3 facing = Vec3.directionFromRotation(0.0F, this.getYRot());
        projected = facing.subtract(normal.scale(facing.dot(normal)));
        if (projected.lengthSqr() >= 0.01D) return projected.normalize();

        Vec3 axis = Math.abs(normal.x) < 0.8D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(0.0D, 0.0D, 1.0D);
        return axis.subtract(normal.scale(axis.dot(normal))).normalize();
    }

    private void faceMovement(Vec3 movement) {
        if (movement.lengthSqr() < 1.0E-5D) return;
        double horizontal = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        if (horizontal >= 1.0E-5D) {
            float yaw = (float)(Mth.atan2(movement.z, movement.x) * Mth.RAD_TO_DEG) - 90.0F;
            this.setYRot(yaw);
            this.setYHeadRot(yaw);
            this.yBodyRot = yaw;
        }
        // Negative pitch points Minecraft's local forward axis upward.
        this.setXRot((float)(-Mth.atan2(movement.y, horizontal) * Mth.RAD_TO_DEG));
    }

    private @Nullable Player findTrackablePlayer(ServerLevel level) {
        Player nearest = null;
        double best = TRACK_RANGE_SQR;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.isCreative()) continue;
            double distance = this.distanceToSqr(player);
            boolean personallyAggroed = player.getUUID().equals(this.aggroPlayer);
            if (player.isCrouching() && distance > CROUCH_DETECTION_SQR && !personallyAggroed) continue;
            if (distance < best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private void tryContactAttack(ServerLevel level, Player target) {
        if (this.attackCooldown > 0 || this.distanceToSqr(target) > 2.35D * 2.35D) return;
        if (target.hurtServer(level, this.damageSources().mobAttack(this), 6.0F)) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), this);
            if (target instanceof net.minecraft.server.level.ServerPlayer player)
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new net.beamex.shamaschizm.centipede.BiteVisuals.Bite(player.getId()));
        }
        this.attackCooldown = 20;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && !player.isCreative()) {
            for (GiantCentipedeEntity part : loadedChain(level, this.chainId)) part.aggroPlayer = player.getUUID();
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel level && !this.suppressChainDeath) {
            if (this.isHead()) {
                spawnDeathPoisonCloud(level);
                killWholeChain(level);
            }
            else splitAtSection(level);
        }
        super.die(source);
    }

    private void spawnDeathPoisonCloud(ServerLevel level) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, this.getX(), this.getY(), this.getZ());
        cloud.setOwner(this);
        cloud.setRadius(3.0F);
        cloud.setWaitTime(0);
        cloud.setDuration(DEATH_CLOUD_DURATION);
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
        level.addFreshEntity(cloud);
    }

    private void killWholeChain(ServerLevel level) {
        for (GiantCentipedeEntity part : loadedChain(level, this.chainId)) {
            if (part == this) continue;
            part.suppressChainDeath = true;
            part.discard();
        }
    }

    private void splitAtSection(ServerLevel level) {
        List<GiantCentipedeEntity> original = loadedChain(level, this.chainId);
        int brokenIndex = this.getPartIndex();
        List<GiantCentipedeEntity> left = new ArrayList<>();
        List<GiantCentipedeEntity> right = new ArrayList<>();
        for (GiantCentipedeEntity part : original) {
            if (part == this) continue;
            if (part.getPartIndex() < brokenIndex) left.add(part);
            else if (part.getPartIndex() > brokenIndex) right.add(part);
        }
        if (!left.isEmpty()) {
            GiantCentipedeEntity newRear = createPart(level, HEAD, false, left.size(), this.position(), this.getYRot() + 180.0F);
            if (newRear != null) left.add(newRear);
            assignChain(left, UUID.randomUUID());
        }
        if (!right.isEmpty()) {
            right.sort(Comparator.comparingInt(GiantCentipedeEntity::getPartIndex).reversed());
            GiantCentipedeEntity newRear = createPart(level, HEAD, false, right.size(), this.position(), this.getYRot());
            if (newRear != null) right.add(newRear);
            assignChain(right, UUID.randomUUID());
        }
    }

    private static boolean mergeAtHeads(ServerLevel level, GiantCentipedeEntity a, GiantCentipedeEntity b) {
        List<GiantCentipedeEntity> aChain = loadedChain(level, a.chainId);
        List<GiantCentipedeEntity> bChain = loadedChain(level, b.chainId);
        if (blocksRecombination(level, aChain) || blocksRecombination(level, bChain)) return false;
        List<GiantCentipedeEntity> first = orientedAwayFromContact(aChain, a);
        List<GiantCentipedeEntity> second = orientedTowardFarHead(bChain, b);
        if (first.isEmpty() || second.isEmpty()) return false;
        a.suppressChainDeath = true;
        b.suppressChainDeath = true;
        a.discard();
        b.discard();
        first.addAll(second);
        assignChain(first, UUID.randomUUID());
        return true;
    }

    private static List<GiantCentipedeEntity> orientedAwayFromContact(List<GiantCentipedeEntity> chain,
                                                                       GiantCentipedeEntity contact) {
        List<GiantCentipedeEntity> result = new ArrayList<>(chain);
        if (contact.getPartIndex() == 0) result.sort(Comparator.comparingInt(GiantCentipedeEntity::getPartIndex).reversed());
        result.remove(contact);
        return result;
    }

    private static List<GiantCentipedeEntity> orientedTowardFarHead(List<GiantCentipedeEntity> chain,
                                                                     GiantCentipedeEntity contact) {
        List<GiantCentipedeEntity> result = new ArrayList<>(chain);
        if (contact.getPartIndex() != 0) result.sort(Comparator.comparingInt(GiantCentipedeEntity::getPartIndex).reversed());
        result.remove(contact);
        return result;
    }

    private static void assignChain(List<GiantCentipedeEntity> ordered, UUID id) {
        for (int i = 0; i < ordered.size(); i++) {
            GiantCentipedeEntity part = ordered.get(i);
            part.chainId = id;
            part.initialized = true;
            part.setPartIndex(i);
            part.setPartKind(i == 0 || i == ordered.size() - 1 ? HEAD : BODY);
            part.setDriver(i == 0);
            part.setPersistenceRequired();
        }
        updateChainHealth(ordered);
    }

    private static void updateChainHealth(List<GiantCentipedeEntity> chain) {
        int bodies = (int) chain.stream().filter(part -> !part.isHead()).count();
        for (GiantCentipedeEntity part : chain) {
            double maximum = 36.0D + (part.isHead() ? 2.0D * bodies : 0.0D);
            var attribute = part.getAttribute(Attributes.MAX_HEALTH);
            if (attribute != null && (part.healthBodySections != bodies
                    || attribute.getBaseValue() != maximum)) {
                float fraction = part.getHealth() / part.getMaxHealth();
                attribute.setBaseValue(maximum);
                // Preserve damage proportion when a chain splits, merges or reverses.
                part.setHealth(Math.min(part.getMaxHealth(), fraction * part.getMaxHealth()));
            }
            part.healthBodySections = bodies;
        }
    }

    private static List<GiantCentipedeEntity> loadedChain(ServerLevel level, UUID id) {
        List<GiantCentipedeEntity> result = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof GiantCentipedeEntity part && part.isAlive() && id.equals(part.chainId)) result.add(part);
        }
        result.sort(Comparator.comparingInt(GiantCentipedeEntity::getPartIndex));
        return result;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playCrawlSound();
    }

    private void playCrawlSound() {
        if (this.tickCount < this.nextCrawlSoundTick) return;
        // 2^(-3/12) = 0.840896: exactly three semitones below the roach playback pitch.
        this.playSound(ModSounds.ROACH_CRAWL.get(), 0.38F, 0.840896F);
        this.nextCrawlSoundTick = this.tickCount + 30 + this.random.nextInt(21);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0.0F;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
        // Chain sections must not shove one another apart.
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    /** Wall-crawling joints may overlap a corner briefly; that must not kill a chain. */
    @Override
    public boolean isInWall() {
        return false;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        this.fallDistance = 0.0D;
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("CentipedeChain", this.chainId.toString());
        output.putInt("CentipedeKind", getPartKind());
        output.putInt("CentipedeIndex", getPartIndex());
        output.putBoolean("CentipedeDriver", isDriver());
        output.putInt("CentipedeSupport", getSupportDirection().get3DDataValue());
        output.putBoolean("CentipedeInitialized", this.initialized);
        output.putInt("CentipedeHealthBodySections", this.healthBodySections);
        if (this.aggroPlayer != null) output.putString("CentipedeAggro", this.aggroPlayer.toString());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.chainId = parseUuid(input.getString("CentipedeChain").orElse(null), UUID.randomUUID());
        setPartKind(Mth.clamp(input.getIntOr("CentipedeKind", HEAD), BODY, HEAD));
        setPartIndex(Math.max(0, input.getIntOr("CentipedeIndex", 0)));
        setDriver(input.getBooleanOr("CentipedeDriver", false));
        this.entityData.set(SUPPORT, Direction.from3DDataValue(input.getIntOr("CentipedeSupport", Direction.DOWN.get3DDataValue())));
        this.initialized = input.getBooleanOr("CentipedeInitialized", true);
        this.healthBodySections = input.getIntOr("CentipedeHealthBodySections", -1);
        this.aggroPlayer = parseUuid(input.getString("CentipedeAggro").orElse(null), null);
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    private static @Nullable UUID parseUuid(@Nullable String value, @Nullable UUID fallback) {
        if (value == null) return fallback;
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }

    public UUID getChainId() { return this.chainId; }
    public int getPartKind() { return this.entityData.get(PART_KIND); }
    public void setPartKind(int kind) { this.entityData.set(PART_KIND, kind); }
    public boolean isHead() { return getPartKind() == HEAD; }
    public int getPartIndex() { return this.entityData.get(PART_INDEX); }
    public void setPartIndex(int index) { this.entityData.set(PART_INDEX, index); }
    public boolean isDriver() { return this.entityData.get(DRIVER); }
    public void setDriver(boolean driver) { this.entityData.set(DRIVER, driver); }
    public boolean isChainMoving() { return this.entityData.get(MOVING); }
    public float getVisualRoll(float partialTick) {
        return Mth.rotLerp(partialTick, this.visualRollO, this.entityData.get(VISUAL_ROLL));
    }
    public Direction getSupportDirection() { return this.entityData.get(SUPPORT); }
}
