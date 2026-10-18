package net.beamex.shamaschizm.entity.custom;

import java.util.UUID;
import net.beamex.shamaschizm.entity.FrogGodRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Persistent tribute guardian which wakes, accepts an emerald, or hunts the player who leaves. */
public final class FrogGodEntity extends PathfinderMob {
    public static final int DORMANT = 0;
    public static final int WAKING = 1;
    public static final int WAITING_FOR_TRIBUTE = 2;
    public static final int PACIFYING = 3;
    public static final int PACIFIED = 4;
    public static final int HOSTILE = 5;
    public static final int JUMPING = 6;
    public static final int CROAKING = 7;
    public static final int FIRE_ATTACK = 8;

    private static final int WAKEUP_TICKS = 100;
    private static final int CROAK_TICKS = 60;
    private static final int FIRE_ATTACK_TICKS = 80;
    private static final int PACIFIED_TICKS = 10 * 60 * 20;
    private static final double WAKE_RANGE = 10.0D;
    private static final double BETRAYAL_RANGE_SQR = 12.0D * 12.0D;
    private static final double FIRE_RANGE_SQR = 11.0D * 11.0D;

    private static final EntityDataAccessor<Integer> ACTION =
            SynchedEntityData.defineId(FrogGodEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PACIFIED_REMAINING =
            SynchedEntityData.defineId(FrogGodEntity.class, EntityDataSerializers.INT);

    private @Nullable UUID tributePlayer;
    private int actionTicks;
    private int clientAnimationAction = Integer.MIN_VALUE;
    private int clientAnimationTicks;
    private int leapCooldown;
    private int fireCooldown = 60;
    private boolean jumpLeftGround;

    public FrogGodEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 25;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.16D);
    }

    /** Players and other entities cannot shove or collision-push the frog god. */
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {}

    @Override
    public void push(double x, double y, double z) {}

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new FrogMeleeGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                this, Player.class, 10, true, false,
                (candidate, level) -> this.isHostileState()));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        super.defineSynchedData(data);
        data.define(ACTION, DORMANT);
        data.define(PACIFIED_REMAINING, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            tickClientAnimationClock();
            return;
        }
        ServerLevel level = (ServerLevel)this.level();

        if (this.leapCooldown > 0) this.leapCooldown--;
        this.actionTicks++;

        switch (getAction()) {
            case DORMANT -> tickDormant(level);
            case WAKING -> tickWaking(level);
            case WAITING_FOR_TRIBUTE -> tickWaiting(level);
            case PACIFYING -> tickPacifying();
            case PACIFIED -> tickPacified();
            case HOSTILE -> tickHostile(level);
            case JUMPING -> tickJumping(level);
            case CROAKING -> tickCroaking(level);
            case FIRE_ATTACK -> tickFireAttack(level);
            default -> beginAction(DORMANT);
        }
    }

    private void tickDormant(ServerLevel level) {
        this.getNavigation().stop();
        this.setTarget(null);
        if (this.tickCount % 5 != 0) return;
        Player nearby = null;
        double nearestDistance = WAKE_RANGE * WAKE_RANGE;
        for (Player candidate : level.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(WAKE_RANGE), FrogGodEntity::isPresentPlayer)) {
            double distance = this.distanceToSqr(candidate);
            if (distance <= nearestDistance) {
                nearby = candidate;
                nearestDistance = distance;
            }
        }
        if (nearby != null) {
            this.tributePlayer = nearby.getUUID();
            beginAction(WAKING);
        }
    }

    private void tickWaking(ServerLevel level) {
        lockHorizontalMovement();
        Player player = tributePlayer(level);
        if (player == null) {
            this.tributePlayer = null;
            beginAction(DORMANT);
            return;
        }
        face(player, 8.0F);
        if (getActionTicks() >= WAKEUP_TICKS) beginAction(WAITING_FOR_TRIBUTE);
    }

    private void tickWaiting(ServerLevel level) {
        lockHorizontalMovement();
        Player player = tributePlayer(level);
        if (player == null) {
            this.tributePlayer = null;
            beginAction(DORMANT);
            return;
        }
        face(player, 8.0F);
        if (this.distanceToSqr(player) > BETRAYAL_RANGE_SQR) becomeHostile(level, player);
    }

    private void tickPacifying() {
        lockHorizontalMovement();
        tickPacifiedClock();
        if (getActionTicks() >= WAKEUP_TICKS) beginAction(PACIFIED);
    }

    private void tickPacified() {
        lockHorizontalMovement();
        tickPacifiedClock();
    }

    private void tickPacifiedClock() {
        int remaining = Math.max(0, getPacifiedRemaining() - 1);
        this.entityData.set(PACIFIED_REMAINING, remaining);
        if (remaining == 0) {
            this.tributePlayer = null;
            beginAction(DORMANT);
        }
    }

    private void tickHostile(ServerLevel level) {
        Player target = validTarget();
        if (target == null) return;
        if (this.fireCooldown > 0) this.fireCooldown--;

        double distance = this.distanceToSqr(target);
        if (this.fireCooldown <= 0 && distance <= FIRE_RANGE_SQR && this.hasLineOfSight(target)) {
            beginAction(CROAKING);
            this.playSound(FrogGodRegistration.CROAK, 2.0F, 1.0F);
            return;
        }
        if (this.leapCooldown <= 0 && this.onGround() && distance >= 1.0D && distance <= 144.0D
                && this.hasLineOfSight(target)) {
            beginLeap(target);
        }
    }

    private void beginLeap(Player target) {
        // Aim at where the player is likely to be near the end of the leap,
        // rather than launching at a fixed speed toward their old position.
        Vec3 prediction = target.getDeltaMovement().multiply(12.0D, 0.0D, 12.0D);
        Vec3 horizontal = target.position().add(prediction).subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        double speed = Mth.clamp(horizontal.length() / 22.0D, 0.34D, 0.92D);
        if (horizontal.lengthSqr() > 1.0E-5D) horizontal = horizontal.normalize().scale(speed);
        this.setDeltaMovement(horizontal.x, 1.05D, horizontal.z);
        this.jumpLeftGround = false;
        this.leapCooldown = 35;
        beginAction(JUMPING);
        this.playSound(SoundEvents.FROG_LONG_JUMP, 1.2F, 0.82F);
    }

    private void tickJumping(ServerLevel level) {
        if (!this.onGround()) this.jumpLeftGround = true;
        Player target = validTarget();
        if (this.jumpLeftGround && target != null && !this.onGround()) {
            // Mild in-air correction makes moving targets hittable without
            // turning the leap into perfect homing movement.
            Vec3 movement = this.getDeltaMovement();
            Vec3 desired = target.position().add(target.getDeltaMovement().scale(6.0D))
                    .subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            if (desired.lengthSqr() > 1.0E-5D) {
                double speed = Mth.clamp(desired.length() / 14.0D, 0.32D, 0.90D);
                desired = desired.normalize().scale(speed);
                this.setDeltaMovement(Mth.lerp(0.14D, movement.x, desired.x), movement.y,
                        Mth.lerp(0.14D, movement.z, desired.z));
            }
        }
        if (this.jumpLeftGround && this.onGround()) {
            land(level);
            beginAction(HOSTILE);
        }
    }

    private void land(ServerLevel level) {
        BlockPos floor = this.blockPosition().below();
        BlockState state = level.getBlockState(floor);
        BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, state, floor);
        level.sendParticles(debris, this.getX(), this.getY() + 0.1D, this.getZ(),
                70, 0.8D, 0.12D, 0.8D, 0.32D);
        level.playSound(null, this.blockPosition(), state.getSoundType(level, floor, this).getFallSound(),
                SoundSource.HOSTILE, 1.6F, 0.7F);

        AABB area = this.getBoundingBox().inflate(4.0D, 1.25D, 4.0D);
        for (Player player : level.getEntitiesOfClass(Player.class, area, FrogGodEntity::isAttackablePlayer)) {
            double horizontalDistance = Math.sqrt(this.distanceToSqr(player.getX(), this.getY(), player.getZ()));
            if (horizontalDistance > 4.0D) continue;
            float damage = (float)Mth.clamp(14.0D - horizontalDistance * 2.75D, 3.0D, 14.0D);
            player.hurtServer(level, this.damageSources().mobAttack(this), damage);
        }
    }

    private void tickCroaking(ServerLevel level) {
        lockHorizontalMovement();
        Player target = validTarget();
        if (target != null) face(target, 6.0F);
        if (getActionTicks() >= CROAK_TICKS) {
            beginAction(FIRE_ATTACK);
            this.playSound(FrogGodRegistration.FROG_FIRE, 2.0F, 1.0F);
        }
    }

    private void tickFireAttack(ServerLevel level) {
        lockHorizontalMovement();
        Player target = validTarget();
        if (target != null) face(target, 12.0F);
        maintainAttackLight(level);
        int ticks = getActionTicks();
        if (ticks >= 34 && ticks <= 58) {
            emitFireCone(level, ticks == 42 || ticks == 52);
        }
        if (ticks >= FIRE_ATTACK_TICKS) {
            this.fireCooldown = 80 + this.random.nextInt(61);
            beginAction(HOSTILE);
        }
    }

    private void emitFireCone(ServerLevel level, boolean damagingPulse) {
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        Vec3 side = new Vec3(forward.z, 0.0D, -forward.x);
        Vec3 mouth = this.position().add(0.0D, 1.15D, 0.0D).add(forward.scale(0.65D));

        for (int i = 0; i < 34; i++) {
            double distance = 0.35D + this.random.nextDouble() * 5.2D;
            double width = (this.random.nextDouble() - 0.5D) * distance * 0.78D;
            double rise = (this.random.nextDouble() - 0.45D) * distance * 0.28D;
            Vec3 point = mouth.add(forward.scale(distance)).add(side.scale(width)).add(0.0D, rise, 0.0D);
            level.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z,
                    1, 0.035D, 0.035D, 0.035D, 0.015D);
            tryIgnite(level, BlockPos.containing(point));
        }

        if (!damagingPulse) return;
        AABB reach = this.getBoundingBox().expandTowards(forward.scale(8.0D)).inflate(3.2D, 1.8D, 3.2D);
        for (Player player : level.getEntitiesOfClass(Player.class, reach, FrogGodEntity::isAttackablePlayer)) {
            Vec3 toPlayer = player.getEyePosition().subtract(mouth);
            double distance = toPlayer.length();
            if (distance > 8.0D || distance < 0.01D
                    || toPlayer.normalize().dot(forward) < 0.68D
                    || !this.hasLineOfSight(player)) continue;
            player.igniteForSeconds(8.0F);
            player.hurtServer(level, this.damageSources().mobAttack(this), 4.0F);
        }
    }

    private static void tryIgnite(ServerLevel level, BlockPos airPos) {
        if (!level.getBlockState(airPos).isAir()) return;
        boolean touchesFlammable = false;
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = airPos.relative(direction);
            if (level.getBlockState(neighbour).isFlammable(level, neighbour, direction.getOpposite())) {
                touchesFlammable = true;
                break;
            }
        }
        if (touchesFlammable) {
            BlockState fire = BaseFireBlock.getState(level, airPos);
            if (fire.canSurvive(level, airPos)) level.setBlock(airPos, fire, Block.UPDATE_ALL);
        }
    }

    private void maintainAttackLight(ServerLevel level) {
        BlockPos pos = this.blockPosition().above();
        BlockState existing = level.getBlockState(pos);
        if (existing.isAir()) {
            level.setBlock(pos, FrogGodRegistration.FIRE_LIGHT.defaultBlockState()
                    .setValue(LightBlock.LEVEL, 6), Block.UPDATE_CLIENTS);
        }
        if (level.getBlockState(pos).is(FrogGodRegistration.FIRE_LIGHT)) {
            level.scheduleTick(pos, FrogGodRegistration.FIRE_LIGHT, 5);
        }
    }

    private void becomeHostile(ServerLevel level, Player offender) {
        this.tributePlayer = null;
        this.setTarget(offender);
        beginAction(HOSTILE);
        this.playSound(FrogGodRegistration.FROG_1, 2.0F, 1.0F);
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                this.getX(), this.getY() + this.getBbHeight(), this.getZ(),
                18, 0.65D, 0.65D, 0.65D, 0.04D);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isCreative()) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        // Waiting until WAKEUP finishes prevents the reverse animation from
        // snapping from a partially awake pose to its fully awake endpoint.
        if (!held.is(Items.EMERALD) || getAction() != WAITING_FOR_TRIBUTE) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide()) return InteractionResult.SUCCESS;

        this.usePlayerItem(player, hand, held);
        this.tributePlayer = null;
        this.setTarget(null);
        this.entityData.set(PACIFIED_REMAINING, PACIFIED_TICKS);
        beginAction(PACIFYING);
        this.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.4F, 1.25F);
        ServerLevel level = (ServerLevel)this.level();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                this.getX(), this.getY() + this.getBbHeight() * 0.75D, this.getZ(),
                34, 0.7D, 0.75D, 0.7D, 0.08D);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && player.isCreative()) return false;
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) return false;
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.POISON) && super.canBeAffected(effect);
    }

    @Override
    public ProjectileDeflection deflection(Projectile projectile) {
        return projectile instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow
                ? ProjectileDeflection.REVERSE : super.deflection(projectile);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        int emeralds = this.random.nextInt(9);
        if (emeralds > 0) this.spawnAtLocation(level, new ItemStack(Items.EMERALD, emeralds));
        this.spawnAtLocation(level, new ItemStack(
                this.random.nextBoolean() ? Items.COBBLESTONE : Items.MAGMA_BLOCK));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FROG_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FROG_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FrogGodRegistration.FROG_1;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.FROG_STEP, 0.8F, 0.75F);
    }

    @Override
    public SoundSource getSoundSource() {
        return isHostileState() ? SoundSource.HOSTILE : SoundSource.NEUTRAL;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("FrogGodAction", getAction());
        output.putInt("FrogGodActionTicks", getActionTicks());
        output.putInt("FrogGodPacifiedRemaining", getPacifiedRemaining());
        output.putInt("FrogGodLeapCooldown", this.leapCooldown);
        output.putInt("FrogGodFireCooldown", this.fireCooldown);
        if (this.tributePlayer != null) output.putString("FrogGodTributePlayer", this.tributePlayer.toString());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(ACTION, Mth.clamp(input.getIntOr("FrogGodAction", DORMANT), DORMANT, FIRE_ATTACK));
        this.actionTicks = Math.max(0, input.getIntOr("FrogGodActionTicks", 0));
        this.entityData.set(PACIFIED_REMAINING, Math.max(0,
                input.getIntOr("FrogGodPacifiedRemaining", 0)));
        this.leapCooldown = Math.max(0, input.getIntOr("FrogGodLeapCooldown", 0));
        this.fireCooldown = Math.max(0, input.getIntOr("FrogGodFireCooldown", 60));
        this.tributePlayer = input.getString("FrogGodTributePlayer").flatMap(value -> {
            try {
                return java.util.Optional.of(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
                return java.util.Optional.empty();
            }
        }).orElse(null);
        this.setPersistenceRequired();
        if (getAction() == JUMPING) beginAction(HOSTILE);
    }

    private void beginAction(int action) {
        this.entityData.set(ACTION, action);
        this.actionTicks = 0;
    }

    private void tickClientAnimationClock() {
        int action = getAction();
        if (action != this.clientAnimationAction) {
            this.clientAnimationAction = action;
            this.clientAnimationTicks = 0;
        } else {
            this.clientAnimationTicks++;
        }
    }

    private void lockHorizontalMovement() {
        this.getNavigation().stop();
        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(0.0D, movement.y, 0.0D);
    }

    private void face(Entity target, float maximumStep) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        float wanted = (float)(Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float facing = Mth.approachDegrees(this.getYRot(), wanted, maximumStep);
        this.setYRot(facing);
        this.yBodyRot = facing;
        this.yHeadRot = facing;
    }

    private @Nullable Player tributePlayer(ServerLevel level) {
        if (this.tributePlayer == null) return null;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.tributePlayer);
        return isPresentPlayer(player) && player.level() == level ? player : null;
    }

    private @Nullable Player validTarget() {
        return this.getTarget() instanceof Player player && isAttackablePlayer(player) ? player : null;
    }

    private static boolean isPresentPlayer(@Nullable Player player) {
        return player != null && player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    private static boolean isAttackablePlayer(@Nullable Player player) {
        return isPresentPlayer(player);
    }

    public int getAction() {
        return this.entityData.get(ACTION);
    }

    public int getActionTicks() {
        return this.actionTicks;
    }

    public float getClientAnimationTicks(float partialTick) {
        return this.clientAnimationTicks + partialTick;
    }

    public int getPacifiedRemaining() {
        return this.entityData.get(PACIFIED_REMAINING);
    }

    public boolean isHostileState() {
        return getAction() >= HOSTILE;
    }

    public boolean isFireAttacking() {
        return getAction() == FIRE_ATTACK;
    }

    public boolean usesBlessedTexture() {
        return getAction() == PACIFYING || getAction() == PACIFIED;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    private static final class FrogMeleeGoal extends MeleeAttackGoal {
        private final FrogGodEntity frog;

        private FrogMeleeGoal(FrogGodEntity frog) {
            super(frog, 1.0D, true);
            this.frog = frog;
        }

        @Override
        public boolean canUse() {
            return this.frog.getAction() == HOSTILE && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return this.frog.getAction() == HOSTILE && super.canContinueToUse();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            // Chasing is handled here, but the frog's actual attacks are the
            // landing shockwave and the separately timed fire cone.
        }
    }
}
