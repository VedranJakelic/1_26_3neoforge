package net.beamex.shamaschizm.livingsmoke;

import java.util.UUID;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Silent stalker: any player attack provokes it, but only enchanted weapons damage it. */
public final class LivingSmoke extends Monster {
    public static final int REACTION_TICKS = 52;
    public static final int COOLDOWN_TICKS = 300;
    private static final EntityDataAccessor<Long> REACTION_AT =
            SynchedEntityData.defineId(LivingSmoke.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> AGGRESSIVE =
            SynchedEntityData.defineId(LivingSmoke.class, EntityDataSerializers.BOOLEAN);
    // Dedicated visual pitch: do not depend on vanilla mob look control or rotation packets
    // to carry the downward loom to the client renderer and remote portal replicas.
    private static final EntityDataAccessor<Float> VISUAL_PITCH =
            SynchedEntityData.defineId(LivingSmoke.class, EntityDataSerializers.FLOAT);
    private UUID attacker;
    private int angerTicks, attackDelay, shelterDelay, unseenTicks, routeAge, routeIndex, stalledTicks;
    private long nextReactionAt;
    private Vec3 hiddenDestination, previousPosition, routeTarget, lastProgressPosition;
    private List<Vec3> route = List.of();
    private boolean orbReleased, deathPearlsReleased;

    public LivingSmoke(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 5;
    }
    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 18)
                .add(Attributes.MOVEMENT_SPEED, .3)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }
    @Override protected void registerGoals() { /* Motion and gaze are handled explicitly. */ }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(REACTION_AT, -100000L);
        builder.define(AGGRESSIVE, false);
        builder.define(VISUAL_PITCH, 0F);
    }
    @Override public boolean isSilent() { return true; }
    @Override public boolean causeFallDamage(double distance, float multiplier, DamageSource source) { return false; }
    @Override public int getMaxSpawnClusterSize() { return 1; }
    @Override public void travel(Vec3 input) { /* Collision-aware floating movement happens in tick(). */ }

    /** Synchronized timestamp allows both normal and immersive-portal renders to animate identically. */
    public float pearlReaction(float partialTicks) {
        long elapsed = level().getGameTime() - entityData.get(REACTION_AT);
        if (elapsed < 0 || elapsed >= REACTION_TICKS) return -1;
        return Mth.clamp((elapsed + partialTicks) / (float) REACTION_TICKS, 0, 1);
    }
    public boolean aggressive() { return entityData.get(AGGRESSIVE); }
    /** Explicitly synchronized downward pose of the entire mouth/pearl model. */
    public float visualPitch() { return entityData.get(VISUAL_PITCH); }

    @Override public void tick() {
        super.tick();
        setNoGravity(true);
        if (level().isClientSide()) {
            clientSmoke();
            return;
        }
        if (!(level() instanceof ServerLevel server) || !isAlive() || isNoAi()) return;
        getNavigation().stop(); setTarget(null);
        if (previousPosition != null && previousPosition.distanceToSqr(position()) > 25) {
            // Discard targets and velocity from the old room after portal traversal.
            hiddenDestination = null; previousPosition = position();
            resetRoute();
            setDeltaMovement(Vec3.ZERO);
        }
        if (pearlReaction(0) >= 0 && !orbReleased && pearlReaction(0) >= .55F) {
            orbReleased = true;
            Vec3 forward = forward();
            Vec3 from = position().add(0, .06, 0).add(forward.scale(.78));
            Kugelblitz orb = LivingSmokeRegistration.KUGELBLITZ.create(server, EntitySpawnReason.TRIGGERED);
            if (orb != null) {
                orb.snapTo(from.x, from.y, from.z, getYRot(), 0);
                server.addFreshEntity(orb);
            }
        }
        if (aggressive()) {
            if (--angerTicks <= 0) { entityData.set(AGGRESSIVE, false); attacker = null; }
            else hunt(server);
        } else observe(server);
        previousPosition = position();
    }

    private void clientSmoke() {
        if (!isAlive()) return;
        // Smoke occupies the invisible body: wide at the mask, tapering toward the tail.
        Vec3 facing = forward();
        Vec3 side = new Vec3(Math.cos(Math.toRadians(getYRot())), 0,
                Math.sin(Math.toRadians(getYRot())));
        Vec3 up = facing.cross(side).normalize();
        for (int i = 0; i < 7; i++) {
            double t = random.nextDouble();
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = (.12 + .58 * (1 - t)) * Math.sqrt(random.nextDouble());
            Vec3 point = position().subtract(facing.scale(.2 + 1.6 * t))
                    .add(side.scale(Math.cos(angle) * radius))
                    .add(up.scale(Math.sin(angle) * radius * .8))
                    .add(0, .08, 0);
            double dx = (random.nextDouble() - .5) * .005;
            double dy = .002 + random.nextDouble() * .007;
            double dz = (random.nextDouble() - .5) * .005;
            level().addParticle(i % 3 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                    point.x, point.y, point.z, dx, dy, dz);
        }
    }

    /** Face points along the standard Minecraft yaw heading. */
    private Vec3 forward() {
        double yaw = Math.toRadians(getYRot());
        double pitch = Math.toRadians(visualPitch());
        return new Vec3(-Math.sin(yaw) * Math.cos(pitch),
                -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
    }
    private static boolean canStalk(Player player) {
        // Creative mode is valid for passive observation and spawn-egg testing.
        return player.isAlive() && !player.isSpectator();
    }
    private ServerPlayer closest(ServerLevel level, double radius) {
        ServerPlayer found = null;
        double best = radius * radius;
        for (ServerPlayer p : level.players()) {
            double d = distanceToSqr(p);
            if (canStalk(p) && d < best) { best = d; found = p; }
        }
        return found;
    }
    private boolean visibleTo(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle().normalize();
        Vec3 back = forward().scale(-1);
        // Check the whole silhouette, including the mouth, smoke tail, and pearls.
        Vec3[] samples = {
                position(), position().add(0, .75, 0), position().add(0, -.65, 0),
                position().add(.65, 0, 0), position().add(-.65, 0, 0),
                position().add(back.scale(.9)), position().add(back.scale(1.65))
        };
        for (Vec3 sample : samples) {
            Vec3 delta = sample.subtract(eye);
            double distance = delta.length();
            if (distance < .01 || distance > 27) continue;
            // Conservative field-of-view check: seeing an edge also freezes it.
            if (look.dot(delta.scale(1 / distance)) < .56) continue;
            var hit = level.clip(new ClipContext(eye, sample,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) return true;
        }
        return false;
    }
    private boolean watchedByAnyone(ServerLevel server) {
        for (ServerPlayer observer : server.players()) {
            if (canStalk(observer) && distanceToSqr(observer) < 28 * 28 && visibleTo(server, observer))
                return true;
        }
        return false;
    }

    private void observe(ServerLevel server) {
        ServerPlayer player = closest(server, 30);
        // Freeze only while actually watched, with a short hesitation once the gaze breaks.
        // Previously this remained frozen whenever a player was within 12 blocks, forever.
        if (watchedByAnyone(server)) {
            unseenTicks = 0;
            resetRoute();
            halt();
            if (player != null) face(player.getEyePosition());
            return;
        }
        if (unseenTicks < 16) {
            unseenTicks++;
            halt();
            // Keep the mouth aimed at the player's moving head even during the
            // short pause before the smoke starts stalking again.
            if (player != null) face(player.getEyePosition());
            return;
        }
        if (player != null && distanceToSqr(player) < 28 * 28) {
            hiddenDestination = null;
            Vec3 goal = behindHead(server, player);
            // It follows the head from behind, rather than trying to occupy the player's hitbox.
            approach(server, goal, .23);
            face(player.getEyePosition());
            return;
        }
        if (--shelterDelay <= 0 || hiddenDestination == null
                || position().distanceToSqr(hiddenDestination) < .6 * .6
                || stalledTicks > 35) {
            shelterDelay = 55 + random.nextInt(80);
            findDarkCorner(server);
            resetRoute();
        }
        if (hiddenDestination != null) approach(server, hiddenDestination, .075);
        else { // Never remain inert if no dark candidate fits the collision box.
            Vec3 drift = position().add((random.nextDouble() - .5) * 4,
                    (random.nextDouble() - .5) * 2, (random.nextDouble() - .5) * 4);
            if (SmokeFlightPath.free(this, server, drift)) hiddenDestination = drift;
        }
    }

    private Vec3 behindHead(ServerLevel server, Player player) {
        Vec3 gaze = player.getLookAngle().multiply(1, 0, 1);
        if (gaze.lengthSqr() < .001) gaze = new Vec3(0, 0, 1);
        gaze = gaze.normalize();
        Vec3 sideways = new Vec3(gaze.z, 0, -gaze.x);
        Vec3 eye = player.getEyePosition();
        // Hover slightly above and directly behind the player's eyes; close enough
        // for the head to remain in front of the mouth without hovering over the
        // ceiling. Lower/further positions are collision-safe fallbacks only.
        for (double radius : new double[]{1.05, 1.35, 1.7, 2.15, 2.75, 3.35}) {
            for (double lift : new double[]{.70, .55, .4, .25, .1, -.2, -.4, -.55}) {
                Vec3 back = eye.subtract(gaze.scale(radius)).add(0, lift, 0);
                for (double side : new double[]{0, .5, -.5, 1.0, -1.0}) {
                    Vec3 candidate = back.add(sideways.scale(side));
                    if (SmokeFlightPath.free(this, server, candidate)) return candidate;
                }
            }
        }
        // The direct position may be obstructed; the flight planner can still get closer.
        return eye.subtract(gaze.scale(2.1)).add(0, .4, 0);
    }

    private void findDarkCorner(ServerLevel server) {
        Vec3 chosen = null;
        // Rank cheap candidates first; test route reachability only for promising corners.
        java.util.List<Vec3> choices = new java.util.ArrayList<>();
        java.util.List<Integer> scores = new java.util.ArrayList<>();
        for (int i = 0; i < 36; i++) {
            Vec3 test = position().add((random.nextDouble() - .5) * 13,
                    (random.nextDouble() - .5) * 4, (random.nextDouble() - .5) * 13);
            if (!SmokeFlightPath.free(this, server, test)) continue;
            BlockPos p = BlockPos.containing(test);
            int light = server.getMaxLocalRawBrightness(p);
            int open = 0;
            for (Vec3 dir : new Vec3[] {new Vec3(2, 0, 0), new Vec3(-2, 0, 0),
                    new Vec3(0, 0, 2), new Vec3(0, 0, -2), new Vec3(0, 2, 0)}) {
                if (server.clip(new ClipContext(test, test.add(dir), ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS) open++;
            }
            int score = light * 5 + open * 3 + random.nextInt(6);
            choices.add(test); scores.add(score);
        }
        for (int trial = 0; trial < 6 && !choices.isEmpty(); trial++) {
            int index = 0;
            for (int i = 1; i < scores.size(); i++) if (scores.get(i) < scores.get(index)) index = i;
            Vec3 test = choices.remove(index);
            scores.remove(index);
            if (SmokeFlightPath.clearLine(this, server, test)
                    || !SmokeFlightPath.plan(this, server, test).isEmpty()) {
                chosen = test;
                break;
            }
        }
        hiddenDestination = chosen;
    }

    private void hunt(ServerLevel server) {
        Player player = attacker == null ? null : server.getPlayerByUUID(attacker);
        if (player == null || !canStalk(player) || distanceToSqr(player) > 34 * 34) {
            entityData.set(AGGRESSIVE, false); attacker = null; resetRoute(); return;
        }
        Vec3 mouth = player.getEyePosition().add(0, -.25, 0);
        // Approach from an offset when needed; collision with the player is not a flying route.
        Vec3 delta = mouth.subtract(position());
        Vec3 goal = delta.lengthSqr() > .01 ? mouth.subtract(delta.normalize().scale(.75)) : mouth;
        approach(server, goal, .29);
        face(mouth);
        if (pearlReaction(0) >= 0) return;
        if (--attackDelay <= 0 && getBoundingBox().inflate(.65).intersects(player.getBoundingBox())) {
            attackDelay = 6;
            if (server.clip(new ClipContext(position(), mouth, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS) {
                player.hurtServer(server, damageSources().mobAttack(this),
                        (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
                server.playSound(null, player.getX(), player.getY() + 1, player.getZ(),
                        SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, .85F, .9F + random.nextFloat() * .2F);
            }
        }
    }

    private void resetRoute() {
        route = List.of(); routeIndex = 0; routeAge = 0; routeTarget = null;
        stalledTicks = 0; lastProgressPosition = null;
    }
    private void approach(ServerLevel server, Vec3 destination, double speed) {
        if (lastProgressPosition == null) lastProgressPosition = position();
        if (position().distanceToSqr(lastProgressPosition) > .2 * .2) {
            lastProgressPosition = position();
            stalledTicks = 0;
        } else stalledTicks++;
        if (position().distanceToSqr(destination) < .27 * .27) {
            halt(); resetRoute(); return;
        }
        boolean blocked = !SmokeFlightPath.clearLine(this, server, destination) || stalledTicks >= 9;
        Vec3 waypoint = destination;
        if (blocked) {
            if (routeTarget == null || routeTarget.distanceToSqr(destination) > 2.5 * 2.5
                    || --routeAge <= 0) {
                route = SmokeFlightPath.plan(this, server, destination);
                routeIndex = 0;
                routeAge = 14;
                routeTarget = destination;
            }
            while (routeIndex < route.size() && position().distanceToSqr(route.get(routeIndex)) < .38 * .38)
                routeIndex++;
            if (routeIndex < route.size()) waypoint = route.get(routeIndex);
        } else if (!route.isEmpty()) {
            resetRoute();
        }
        Vec3 difference = waypoint.subtract(position());
        // Proportional speed near the destination avoids vibration around the player's head.
        Vec3 wish = difference.lengthSqr() < .0001 ? Vec3.ZERO
                : difference.normalize().scale(Math.min(speed, difference.length() * .25));
        Vec3 velocity = getDeltaMovement().scale(.58).add(wish.scale(.42));
        if (velocity.lengthSqr() < 1.0E-7) return;
        if (!server.noCollision(this, getBoundingBox().expandTowards(velocity))) {
            // Cancel only the blocked components; try the remaining axis movement.
            Vec3 x = new Vec3(velocity.x, 0, 0);
            Vec3 y = new Vec3(0, velocity.y, 0);
            Vec3 z = new Vec3(0, 0, velocity.z);
            if (!server.noCollision(this, getBoundingBox().expandTowards(x))) x = Vec3.ZERO;
            if (!server.noCollision(this, getBoundingBox().expandTowards(y))) y = Vec3.ZERO;
            if (!server.noCollision(this, getBoundingBox().expandTowards(z))) z = Vec3.ZERO;
            velocity = x.add(y).add(z);
            routeAge = 0; // Recalculate the route after a blocked step.
        }
        setDeltaMovement(velocity);
        move(MoverType.SELF, velocity);
        if (horizontalCollision || verticalCollision) { setDeltaMovement(Vec3.ZERO); routeAge = 0; }
    }
    private void halt() { setDeltaMovement(Vec3.ZERO); }
    private void face(Vec3 target) {
        Vec3 direction = target.subtract(position());
        double horizontal = Math.sqrt(direction.horizontalDistanceSqr());
        if (direction.lengthSqr() < 1.0E-6) return;
        // Track the actual line from the mouth's local origin to the target eye
        // position. Positive pitch is downward. No minimum/forced looming angle:
        // the pitch gets steeper naturally as the smoke moves directly overhead.
        float wantedPitch = (float)Math.toDegrees(Math.atan2(-direction.y, horizontal));
        float rendered = Mth.lerp(.38F, visualPitch(), wantedPitch);
        entityData.set(VISUAL_PITCH, rendered);
        setXRot(rendered); // Retain standard facing for any vanilla behaviors.
        if (horizontal > .001) {
            float targetYaw = (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
            setYRot(Mth.rotLerp(.18F, getYRot(), targetYaw));
        }
        yBodyRot = getYRot(); yHeadRot = getYRot();
    }

    /** Starting retaliation is independent of whether the blow can damage the smoke. */
    private void provoke(ServerLevel server, Player player) {
        if (!isAlive() || !canStalk(player)) return;
        attacker = player.getUUID();
        entityData.set(AGGRESSIVE, true);
        angerTicks = 240;
        resetRoute();
        if (server.getGameTime() >= nextReactionAt) {
            nextReactionAt = server.getGameTime() + COOLDOWN_TICKS;
            entityData.set(REACTION_AT, server.getGameTime());
            orbReleased = false;
        }
    }

    @Override public boolean hurtServer(ServerLevel server, DamageSource source, float amount) {
        // Player-caused blows, including bare fists and completely unenchanted items,
        // must anger the smoke BEFORE the enchantment-based damage immunity check.
        if (source.getEntity() instanceof Player player) provoke(server, player);
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurtServer(server, source, amount);
        // Keep the existing weapon damage restriction: melee requires an enchanted
        // main hand; ranged attacks require an enchanted held weapon. Unarmed and
        // unenchanted blows still return false (no health loss), but trigger hostility.
        if (!(source.getEntity() instanceof LivingEntity attackerEntity)) return false;
        ItemStack main = attackerEntity.getMainHandItem(), off = attackerEntity.getOffhandItem();
        boolean melee = source.getDirectEntity() == attackerEntity;
        if (!main.isEnchanted() && (melee || !off.isEnchanted())) return false;
        boolean hurt = super.hurtServer(server, source, amount);
        if (hurt && isAlive() && !(attackerEntity instanceof Player)) {
            // Preserve the previous reaction to enchanted non-player attacks.
            ServerPlayer nearest = closest(server, 32);
            if (nearest != null) provoke(server, nearest);
        }
        return hurt;
    }
    /** Drop each visible pearl as its own small, gravity-driven rigid body. */
    @Override public void die(DamageSource source) {
        if (!deathPearlsReleased && level() instanceof ServerLevel server) {
            deathPearlsReleased = true;
            float age = tickCount;
            float reaction = pearlReaction(0);
            for (int i = 0; i < 16; i++) {
                Vec3 local = LivingSmokePearls.position(i, age, reaction);
                double angle = Math.PI - Math.toRadians(getYRot());
                // Match the yaw * downward-pitch transform used by the renderer.
                double pitch = -Math.toRadians(visualPitch());
                double pitchedY = Math.cos(pitch) * local.y - Math.sin(pitch) * local.z;
                double pitchedZ = Math.sin(pitch) * local.y + Math.cos(pitch) * local.z;
                double x = Math.cos(angle) * local.x + Math.sin(angle) * pitchedZ;
                double z = -Math.sin(angle) * local.x + Math.cos(angle) * pitchedZ;
                Vec3 at = position().add(x, pitchedY, z);
                FallingPearl pearl = LivingSmokeRegistration.FALLING_PEARL.create(server, EntitySpawnReason.TRIGGERED);
                if (pearl == null) continue;
                pearl.snapTo(at.x, at.y, at.z, random.nextFloat() * 360F, random.nextFloat() * 360F);
                Vec3 outward = new Vec3(x, local.y * .65, z);
                if (outward.lengthSqr() < .01) outward = new Vec3(random.nextDouble() - .5, 0, random.nextDouble() - .5);
                outward = outward.normalize();
                pearl.setDeltaMovement(outward.scale(.09 + random.nextDouble() * .12)
                        .add((random.nextDouble() - .5) * .06,
                                .075 + random.nextDouble() * .10,
                                (random.nextDouble() - .5) * .06));
                server.addFreshEntity(pearl);
            }
        }
        super.die(source);
    }

    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("LivingSmokeAnger", angerTicks);
        out.putInt("LivingSmokeReactionCooldown", (int)Math.max(0, nextReactionAt - level().getGameTime()));
        out.putInt("LivingSmokeReactionAge", (int)Math.max(0, level().getGameTime() - entityData.get(REACTION_AT)));
        out.putBoolean("LivingSmokeAggressive", aggressive());
        out.putBoolean("LivingSmokeOrbReleased", orbReleased);
        if (attacker != null) out.putString("LivingSmokeAttacker", attacker.toString());
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        angerTicks = Math.max(0, in.getIntOr("LivingSmokeAnger", 0));
        nextReactionAt = level().getGameTime() + Math.clamp(in.getIntOr("LivingSmokeReactionCooldown", 0), 0, COOLDOWN_TICKS);
        entityData.set(REACTION_AT, level().getGameTime() - Math.max(0,
                in.getIntOr("LivingSmokeReactionAge", REACTION_TICKS)));
        entityData.set(AGGRESSIVE, in.getBooleanOr("LivingSmokeAggressive", false));
        orbReleased = in.getBooleanOr("LivingSmokeOrbReleased", true);
        try { attacker = UUID.fromString(in.getStringOr("LivingSmokeAttacker", "")); }
        catch (IllegalArgumentException ignored) { attacker = null; }
        setNoGravity(true);
    }
}
