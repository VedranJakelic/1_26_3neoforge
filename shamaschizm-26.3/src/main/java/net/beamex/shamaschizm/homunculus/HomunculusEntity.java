package net.beamex.shamaschizm.homunculus;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** All decisions, feeding and inventory mutations are server-authoritative. */
public final class HomunculusEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(HomunculusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> SKIN_ID = SynchedEntityData.defineId(HomunculusEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_NAME = SynchedEntityData.defineId(HomunculusEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<net.minecraft.world.item.component.ResolvableProfile> PROFILE = SynchedEntityData.defineId(HomunculusEntity.class, EntityDataSerializers.RESOLVABLE_PROFILE);
    private UUID owner;
    private int fed, lastAttackStamp = -1, lastHurtStamp = -1, attackCooldown, stuckTicks;
    private HomunculusCommand command = HomunculusCommand.STAY;
    private BlockPos anchor;
    private Vec3 marchStart = Vec3.ZERO, marchDirection = Vec3.ZERO;
    private Vec3 previousPosition = Vec3.ZERO;
    private double marchProgress;
    private UUID pursued;
    private int pursuitTicks;
    private final Map<UUID,Integer> unreachableTargets = new HashMap<>();
    final HomunculusWork work = new HomunculusWork(this);

    public HomunculusEntity(EntityType<? extends HomunculusEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }
    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10).add(Attributes.MOVEMENT_SPEED, .18)
                .add(Attributes.ATTACK_DAMAGE, 2).add(Attributes.FOLLOW_RANGE, 32).add(Attributes.STEP_HEIGHT, 1);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b); b.define(STAGE, 0); b.define(SKIN_ID, ""); b.define(SKIN_NAME, ""); b.define(PROFILE, net.minecraft.world.item.component.ResolvableProfile.createUnresolved(new UUID(0,0)));
    }
    public int stage() { return entityData.get(STAGE); }
    public String skinId() { return entityData.get(SKIN_ID); }
    public String skinName() { return entityData.get(SKIN_NAME); }
    public net.minecraft.world.item.component.ResolvableProfile skinProfile() { return entityData.get(PROFILE); }
    public UUID ownerId() { return owner; }
    public BlockPos anchor() { return anchor == null ? blockPosition() : anchor; }
    public HomunculusCommand command() { return command; }
    public ServerPlayer ownerPlayer() {
        return level() instanceof ServerLevel s && owner != null ? s.getServer().getPlayerList().getPlayer(owner) : null;
    }
    public boolean accepts(Player p) { return stage() == 0 || p.getUUID().equals(owner); }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected void registerGoals() { goalSelector.addGoal(0, new FloatGoal(this)); }

    public boolean command(ServerPlayer p, HomunculusCommand next) {
        if (!accepts(p) || distanceToSqr(p) > 100) return false;
        if (stage() < next.requiredStage) {
            p.sendSystemMessage(Component.translatableWithFallback("message.shamaschizm.homunculus.too_young", "The homunculus has not learned that command yet."));
            return false;
        }
        work.releaseTickets(); work.resetJob();
        owner = p.getUUID(); command = next; anchor = blockPosition();
        getNavigation().stop(); setTarget(null); stuckTicks = 0;
        lastAttackStamp = p.getLastHurtMobTimestamp(); lastHurtStamp = p.getLastHurtByMobTimestamp();
        if (next == HomunculusCommand.DESTROY) {
            marchStart = position(); marchDirection = p.getLookAngle().multiply(1,0,1).normalize();
            if(marchDirection.lengthSqr()<.001){double yaw=Math.toRadians(p.getYRot());marchDirection=new Vec3(-Math.sin(yaw),0,Math.cos(yaw));}
            marchProgress = 0;
        }
        p.sendSystemMessage(Component.translatableWithFallback("message.shamaschizm.homunculus.command", "Homunculus: %s", next.name().toLowerCase(Locale.ROOT)));
        return true;
    }
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!accepts(player)) return InteractionResult.PASS;
        var food = stack.get(DataComponents.FOOD);
        if (food != null && food.nutrition() > 0) {
            if (!level().isClientSide()) {
                int value = food.nutrition();
                var useRemainder = stack.get(DataComponents.USE_REMAINDER);
                ItemStack remainder = useRemainder == null ? ItemStack.EMPTY : useRemainder.convertInto().create();
                stack.consume(1, player);
                if (!player.getAbilities().instabuild && !remainder.isEmpty()) {
                    if (!player.getInventory().add(remainder)) spawnAtLocation((ServerLevel)level(), remainder);
                }
                heal(value); playSound(SoundEvents.GENERIC_EAT.value(), .5F, 1.2F);
                if (stage() < 2) {
                    fed += value;
                    int required = HomunculusRules.foodRequired(stage());
                    if (fed >= required) {
                        fed = 0;
                        if (stage() == 0) {
                            owner = player.getUUID();
                            entityData.set(PROFILE, net.minecraft.world.item.component.ResolvableProfile.createResolved(player.getGameProfile()));
                            entityData.set(SKIN_ID, owner.toString()); entityData.set(SKIN_NAME, player.getGameProfile().name());
                        }
                        entityData.set(STAGE, stage() + 1); applyStage(); setHealth(getMaxHealth());
                        player.sendSystemMessage(Component.translatableWithFallback("message.shamaschizm.homunculus.grown", "The homunculus has grown."));
                    } else player.sendSystemMessage(Component.translatableWithFallback("message.shamaschizm.homunculus.fed", "Homunculus feeding: %s / %s", fed, required));
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (stage() >= 1 && !stack.isEmpty()) {
            EquipmentSlot slot = getEquipmentSlotForItem(stack);
            if (slot != EquipmentSlot.MAINHAND && (stage() < 2 || !Set.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET).contains(slot))) return InteractionResult.PASS;
            if (!level().isClientSide()) {
                ItemStack old = getItemBySlot(slot);
                setItemSlot(slot, stack.copyWithCount(1)); setGuaranteedDrop(slot); stack.consume(1, player);
                if (!old.isEmpty() && !player.getInventory().add(old)) spawnAtLocation((ServerLevel)level(), old);
            }
            return InteractionResult.SUCCESS;
        }
        if (stage() >= 1 && stack.isEmpty() && player.isShiftKeyDown()) {
            if (!level().isClientSide()) for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack old = getItemBySlot(slot);
                if (!old.isEmpty()) {
                    setItemSlot(slot, ItemStack.EMPTY);
                    if (!player.getInventory().add(old)) spawnAtLocation((ServerLevel)level(), old);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }
    private void applyStage() {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(HomunculusRules.maximumHealth(stage()));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(stage() == 2 ? 6 : 2);
        refreshDimensions();
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key); if (STAGE.equals(key)) refreshDimensions();
    }
    @Override protected EntityDimensions getDefaultDimensions(Pose pose) {
        if (stage() == 0) return EntityDimensions.scalable(.65F, .5F);
        if (stage() == 1) return EntityDimensions.scalable(.4F, .95F);
        return EntityDimensions.scalable(.66F, pose == Pose.SWIMMING ? .66F : pose == Pose.CROUCHING ? 1.5F : 1.98F);
    }
    private boolean fits(Pose pose, Vec3 at) {
        return level().noCollision(this, getDefaultDimensions(pose).makeBoundingBox(at));
    }
    private void posture() {
        if (stage() != 2) return;
        Vec3 ahead = position();
        var path = getNavigation().getPath();
        if (path != null && !path.isDone()) {
            Vec3 delta = path.getNextEntityPos(this).subtract(position());
            ahead = position().add(delta.normalize().scale(Math.min(.75, delta.length())));
        }
        Pose wanted = Pose.SWIMMING;
        for (Pose pose : new Pose[]{Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING})
            if (fits(pose, position()) && fits(pose, ahead)) { wanted = pose; break; }
        if (getPose() != wanted) { setPose(wanted); refreshDimensions(); }
    }
    /** Plan at crawling height; physical pose expands only when the next segment has room. */
    void walkTo(Vec3 at) {
        if (stage() == 2) { setPose(Pose.SWIMMING); refreshDimensions(); }
        getNavigation().moveTo(at.x, at.y, at.z, 1);
        posture();
    }
    net.minecraft.world.level.pathfinder.Path workPath(BlockPos at) {
        Pose old=getPose();
        if(stage()==2){setPose(Pose.SWIMMING);refreshDimensions();}
        var path=getNavigation().createPath(at,0);
        if(stage()==2){setPose(old);refreshDimensions();}
        return path;
    }
    private boolean eligible(LivingEntity e) {
        return e != this && e.isAlive() && unreachableTargets.getOrDefault(e.getUUID(),0)<=tickCount && !e.getUUID().equals(owner) && !isAlliedTo(e)
                && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
                && !(e instanceof HomunculusEntity h && owner != null && owner.equals(h.ownerId()));
    }
    private boolean inMarch(LivingEntity e) {
        Vec3 d = e.position().subtract(marchStart);
        double along = d.dot(marchDirection);
        return along >= -2 && along <= 200 && d.subtract(marchDirection.scale(along)).lengthSqr() <= 36;
    }
    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || !isAlive()) return;
        if (anchor == null) anchor = blockPosition();
        posture();
        if (attackCooldown > 0) attackCooldown--;
        if (tickCount % 10 != 0) return;
        work.updateTickets();
        unreachableTargets.entrySet().removeIf(e -> e.getValue()<=tickCount);
        ServerPlayer p = ownerPlayer();
        boolean sameWorld = p != null && p.level() == level();
        if (stage() >= 1 && sameWorld && command == HomunculusCommand.FIGHT && p.getLastHurtMobTimestamp() != lastAttackStamp) {
            lastAttackStamp = p.getLastHurtMobTimestamp();
            if (p.getLastHurtMob() != null && eligible(p.getLastHurtMob())) setTarget(p.getLastHurtMob());
        }
        if (stage() == 2 && sameWorld && p.getLastHurtByMobTimestamp() != lastHurtStamp) {
            lastHurtStamp = p.getLastHurtByMobTimestamp();
            if (p.getLastHurtByMob() != null && eligible(p.getLastHurtByMob()) && distanceToSqr(p) < 64*64) setTarget(p.getLastHurtByMob());
        }
        LivingEntity target = getTarget();
        if (target != null && (!eligible(target) || distanceToSqr(target) > 48*48
                || command == HomunculusCommand.GUARD && target.distanceToSqr(Vec3.atCenterOf(anchor())) > 400
                || command == HomunculusCommand.DESTROY && !inMarch(target))) { setTarget(null); target = null; }
        if (target == null && (command == HomunculusCommand.GUARD || command == HomunculusCommand.DESTROY)) {
            final boolean guard = command == HomunculusCommand.GUARD;
            AABB box = guard ? new AABB(anchor()).inflate(20) : getBoundingBox().inflate(20);
            target = level().getEntitiesOfClass(LivingEntity.class, box, e -> eligible(e)
                    && (guard ? e instanceof Enemy && e.distanceToSqr(Vec3.atCenterOf(anchor())) <= 400 : inMarch(e)))
                    .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            setTarget(target);
        }
        if (target != null) {
            if(!target.getUUID().equals(pursued)){pursued=target.getUUID();pursuitTicks=0;}
            if(++pursuitTicks>60 && getSensing().hasLineOfSight(target)==false || pursuitTicks>120){
                unreachableTargets.put(target.getUUID(),tickCount+600);setTarget(null);pursuitTicks=0;return;
            }
            walkTo(target.position()); getLookControl().setLookAt(target, 30, 30);
            if (distanceToSqr(target) < Math.pow(getBbWidth() + target.getBbWidth() + .8, 2)
                    && getSensing().hasLineOfSight(target) && attackCooldown == 0) {
                swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT); doHurtTarget(server, target); attackCooldown = 20; pursuitTicks=0;
            }
            return;
        }
        switch (command) {
            case FOLLOW, FIGHT -> { if (sameWorld && distanceToSqr(p) > 4) walkTo(p.position()); else getNavigation().stop(); }
            case STAY -> wander(5);
            case GUARD -> wander(20);
            case SORT, HARVEST -> work.tick(command == HomunculusCommand.HARVEST);
            case DESTROY -> march();
        }
    }
    private void wander(int radius) {
        Vec3 center = Vec3.atBottomCenterOf(anchor());
        if (position().distanceToSqr(center) > radius*radius) { walkTo(center); return; }
        if (!getNavigation().isDone() || random.nextInt(8) != 0) return;
        Vec3 destination = DefaultRandomPos.getPos(this, radius, 3);
        if (destination != null && destination.distanceToSqr(center) <= radius*radius) walkTo(destination);
    }
    private void march() {
        marchProgress = Math.max(marchProgress, position().subtract(marchStart).dot(marchDirection));
        if (marchProgress >= 198 || stuckTicks >= 600) {
            command = HomunculusCommand.STAY; anchor = blockPosition(); getNavigation().stop(); return;
        }
        if (position().distanceToSqr(previousPosition) < .04) stuckTicks += 10; else stuckTicks = 0;
        previousPosition = position();
        Vec3 next=marchStart.add(marchDirection.scale(Math.min(200, marchProgress + 12)));
        walkTo(new Vec3(next.x,getY(),next.z));
    }
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit); work.dropCargo(level); work.releaseTickets();
    }
    @Override public void remove(RemovalReason reason) {
        // Preserve tickets on server shutdown/unload; release on death or dimension transfer.
        if (reason.shouldDestroy() || reason == RemovalReason.CHANGED_DIMENSION) work.releaseTickets();
        super.remove(reason);
    }
    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("HomunculusStage", stage()); out.putInt("Fed", fed); out.putString("Command", command.name());
        if (owner != null) out.putString("Owner", owner.toString());
        out.store("SkinProfile", net.minecraft.world.item.component.ResolvableProfile.CODEC, skinProfile());
        out.putString("SkinId", skinId()); out.putString("SkinName", skinName());
        out.store("Anchor", BlockPos.CODEC, anchor());
        out.store("MarchStart", Vec3.CODEC, marchStart); out.store("MarchDirection", Vec3.CODEC, marchDirection);
        out.putDouble("MarchProgress", marchProgress); out.putInt("MarchStuck", stuckTicks); work.save(out);
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        entityData.set(STAGE, Math.clamp(in.getIntOr("HomunculusStage", 0), 0, 2));
        fed = Math.clamp(in.getIntOr("Fed", 0), 0, stage() == 0 ? 39 : 59);
        try { owner = UUID.fromString(in.getStringOr("Owner", "")); } catch (IllegalArgumentException ex) { owner = null; }
        in.read("SkinProfile", net.minecraft.world.item.component.ResolvableProfile.CODEC).ifPresent(p -> entityData.set(PROFILE,p));
        entityData.set(SKIN_ID, in.getStringOr("SkinId", "")); entityData.set(SKIN_NAME, in.getStringOr("SkinName", ""));
        try { command = HomunculusCommand.valueOf(in.getStringOr("Command", "STAY")); } catch (IllegalArgumentException ex) { command = HomunculusCommand.STAY; }
        if (command.requiredStage > stage()) command = HomunculusCommand.STAY;
        anchor = in.read("Anchor", BlockPos.CODEC).orElse(blockPosition());
        marchStart = in.read("MarchStart", Vec3.CODEC).orElse(position()); marchDirection = in.read("MarchDirection", Vec3.CODEC).orElse(Vec3.ZERO).normalize();
        marchProgress = in.getDoubleOr("MarchProgress", 0); stuckTicks = in.getIntOr("MarchStuck", 0);
        work.load(in); applyStage();
    }
}
