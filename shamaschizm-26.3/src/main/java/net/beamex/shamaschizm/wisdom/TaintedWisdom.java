package net.beamex.shamaschizm.wisdom;

import java.util.*;
import net.beamex.shamaschizm.centipede.BiteVisuals;
import net.beamex.shamaschizm.earthworm.Earthworms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.gameevent.*;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Blind floating predator: position memories come from movement and audible events only. */
public final class TaintedWisdom extends Monster implements VibrationSystem {
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(TaintedWisdom.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RELEASED = SynchedEntityData.defineId(TaintedWisdom.class, EntityDataSerializers.BOOLEAN);
    private static final List<EntityDataAccessor<Vector3fc>> NODES = new ArrayList<>();
    static { for (int i = 0; i < StingerRope.NODES; i++) NODES.add(SynchedEntityData.defineId(TaintedWisdom.class, EntityDataSerializers.VECTOR3)); }
    private final StingerRope rope = new StingerRope();
    private VibrationSystem.Data vibrationData = new VibrationSystem.Data();
    private final VibrationSystem.User vibrationUser = new Hearing();
    private final DynamicGameEventListener<VibrationSystem.Listener> vibrationListener =
            new DynamicGameEventListener<>(new VibrationSystem.Listener(this));
    private long nextVibrationAt;
    private Vec3 previousPosition;
    private Vec3 destination, heardPosition;
    private long heardUntil;
    private int wanderTicks, cooldown, thrustTicks;
    private Vec3 thrustTarget;
    private boolean struck;
    private WisdomAttachments.Anchor[] anchors;
    private int anchorSeed;

    public TaintedWisdom(EntityType<? extends Monster> type, Level level) {
        super(type, level); setNoGravity(true); xpReward = 2;
        if (!level.isClientSide()) entityData.set(SEED, random.nextInt());
    }
    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 2).add(Attributes.MOVEMENT_SPEED, .08)
                .add(Attributes.FOLLOW_RANGE, 24).add(Attributes.ATTACK_DAMAGE, 2);
    }
    @Override protected void registerGoals() {} // No nearest-player, sight, or retaliation target goal.
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b); b.define(SEED, 0); b.define(RELEASED, false);
        double down = 0;
        for (int i = 0; i < StingerRope.NODES; i++) {
            if (i > 0) down += StingerRope.LENGTHS[i - 1];
            b.define(NODES.get(i), new Vector3f(0, (float)(.0625 - down), 0));
        }
    }
    public WisdomAttachments.Anchor[] anchors() {
        int seed = entityData.get(SEED);
        if (anchors == null || seed != anchorSeed) { anchorSeed = seed; anchors = WisdomAttachments.create(seed); }
        return anchors;
    }
    public boolean wormsReleased() { return entityData.get(RELEASED); }
    public Vector3f stingerPoint(int i) { return new Vector3f(entityData.get(NODES.get(i))); }
    @Override public void travel(Vec3 input) {} // Server owns collision-aware float movement.
    @Override public boolean causeFallDamage(double distance, float multiplier, DamageSource source) { return false; }
    @Override public int getMaxSpawnClusterSize() { return 1; }
    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (!isAlive()) {
            // deathTime advances only after a real, non-cancelled death.
            if (deathTime > 0 && !wormsReleased()) releaseWorms(server);
            return;
        }
        setNoGravity(true); setTarget(null); getNavigation().stop();
        // Teleporting must not retain an incoming vibration or a destination in a distant portal room.
        if(previousPosition!=null&&previousPosition.distanceToSqr(position())>16){
            vibrationData=new VibrationSystem.Data();heardPosition=null;destination=null;heardUntil=0;nextVibrationAt=0;
            thrustTicks=0;thrustTarget=null;
        }
        VibrationSystem.Ticker.tick(server,vibrationData,vibrationUser);
        if (cooldown > 0) cooldown--;
        boolean pursuing = heardPosition != null && server.getGameTime() < heardUntil;
        if(!pursuing&&heardPosition!=null){heardPosition=null;destination=null;wanderTicks=0;}
        if (pursuing) destination = heardPosition.add(0, 2.3, 0);
        else if (--wanderTicks <= 0 || destination == null || position().distanceToSqr(destination) < .3) {
            wanderTicks = 60 + random.nextInt(100);
            destination = position().add((random.nextDouble() - .5) * 8, (random.nextDouble() - .5) * 2, (random.nextDouble() - .5) * 8);
        }
        Vec3 direction = destination.subtract(position());
        Vec3 wanted = direction.lengthSqr() < .04 ? Vec3.ZERO : direction.normalize().scale(pursuing ? .055 : .028);
        Vec3 velocity = getDeltaMovement().scale(.8).add(wanted.scale(.2));
        setDeltaMovement(velocity); move(MoverType.SELF, velocity);
        previousPosition=position();
        if (horizontalCollision || verticalCollision) { wanderTicks = 0; setDeltaMovement(Vec3.ZERO); }
        if (velocity.horizontalDistanceSqr() > 1e-6) {
            float yaw = (float)Math.toDegrees(Math.atan2(-velocity.x, velocity.z));
            setYRot(net.minecraft.util.Mth.rotLerp(.08F, getYRot(), yaw));
            yBodyRot = getYRot(); yHeadRot = getYRot();
        }
        if (pursuing && cooldown == 0 && thrustTicks == 0 && position().distanceToSqr(heardPosition.add(0, 1, 0)) < 8) {
            thrustTicks = 20; cooldown = 55; struck = false; thrustTarget = heardPosition.add(0, .9, 0);
        }
        Vec3 oldTip = rope.point(StingerRope.NODES - 1);
        Vec3 aim = thrustTicks > 0 ? thrustTarget : null;
        rope.tick(position().add(0, .0625, 0), aim, (a, b) -> clipRope(server, a, b));
        if (thrustTicks > 0) {
            thrustTicks--;
            if (!struck && oldTip != null) sting(server, oldTip, rope.point(StingerRope.NODES - 1));
        }
        for (int i = 0; i < StingerRope.NODES; i++) {
            Vec3 p = rope.point(i).subtract(position());
            entityData.set(NODES.get(i), new Vector3f((float)p.x, (float)p.y, (float)p.z));
        }
    }
    @Override public VibrationSystem.Data getVibrationData(){return vibrationData;}
    @Override public VibrationSystem.User getVibrationUser(){return vibrationUser;}
    @Override public void updateDynamicGameEventListener(java.util.function.BiConsumer<DynamicGameEventListener<?>,ServerLevel> action){
        if(level() instanceof ServerLevel server)action.accept(vibrationListener,server);
    }
    private final class Hearing implements VibrationSystem.User {
        private final PositionSource positionSource=new EntityPositionSource(TaintedWisdom.this,.3F);
        @Override public int getListenerRadius(){return 16;}
        @Override public PositionSource getPositionSource(){return positionSource;}
        @Override public TagKey<GameEvent> getListenableEvents(){return GameEventTags.WARDEN_CAN_LISTEN;}
        @Override public boolean canTriggerAvoidVibration(){return true;}
        @Override public boolean canReceiveVibration(ServerLevel server,BlockPos pos,Holder<GameEvent> event,GameEvent.Context context){
            if(!isAlive()||isNoAi()||server.getGameTime()<nextVibrationAt||!server.getWorldBorder().isWithinBounds(pos))return false;
            Entity source=context.sourceEntity();
            if(source==TaintedWisdom.this||source instanceof TaintedWisdom)return false;
            return !(source instanceof net.minecraft.world.entity.player.Player p)||(!p.isCreative()&&!p.isSpectator());
        }
        @Override public void onReceiveVibration(ServerLevel server,BlockPos pos,Holder<GameEvent> event,Entity source,Entity projectileOwner,float distance){
            if(!isAlive()||isNoAi()||server.getGameTime()<nextVibrationAt)return;
            // Follow the emitted location, including projectile impacts, never a silent player's live position.
            heardPosition=Vec3.atBottomCenterOf(pos);heardUntil=server.getGameTime()+200;nextVibrationAt=server.getGameTime()+20;
        }
    }
    private Vec3 clipRope(ServerLevel server, Vec3 a, Vec3 b) {
        var hit = server.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS ? b : hit.getLocation().add(a.subtract(b).normalize().scale(.035));
    }
    private void sting(ServerLevel server, Vec3 from, Vec3 to) {
        // A portal jump must never become a swept attack across the intervening world.
        if (from.distanceToSqr(to) > 9) return;
        for (ServerPlayer player : server.players()) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()) continue;
            var box = player.getBoundingBox().inflate(.12);
            if (!box.contains(to) && box.clip(from, to).isEmpty()) continue;
            Vec3 center = player.getBoundingBox().getCenter();
            if (server.clip(new ClipContext(position().add(0, .3, 0), center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS) continue;
            if (player.hurtServer(server, damageSources().mobAttack(this), (float)getAttributeValue(Attributes.ATTACK_DAMAGE))) {
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0), this);
                PacketDistributor.sendToPlayer(player, new BiteVisuals.Bite(player.getId()));
                struck = true; break;
            }
        }
    }
    private void releaseWorms(ServerLevel server) {
        entityData.set(RELEASED, true);
        var attached=anchors();
        for (int i=0;i<attached.length+5;i++) {
            var anchor=attached[i<attached.length?i:random.nextInt(attached.length)];
            var worm = Earthworms.TYPE.create(server, EntitySpawnReason.TRIGGERED);
            if (worm == null) continue;
            Vec3 normal = WisdomAttachments.rotate(anchor.normal(), getYRot());
            Vec3 at = position().add(WisdomAttachments.rotate(anchor.point(), getYRot())).add(normal.scale(.04));
            worm.snapTo(at.x, at.y, at.z, getYRot(), 0);
            worm.fallAndWait(normal.scale(.07).add((random.nextDouble()-.5)*.08, .06 + random.nextDouble() * .04, (random.nextDouble()-.5)*.08), 300 + random.nextInt(101));
            server.addFreshEntity(worm);
        }
    }
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean playerKill) {
        super.dropCustomDeathLoot(level, source, playerKill); spawnAtLocation(level, new ItemStack(WisdomRegistration.BRAIN));
    }
    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out); out.store("WisdomVibration",VibrationSystem.Data.CODEC,vibrationData); out.putInt("WisdomSeed", entityData.get(SEED)); out.putBoolean("WormsReleased", wormsReleased());
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in); vibrationData=in.read("WisdomVibration",VibrationSystem.Data.CODEC).orElseGet(VibrationSystem.Data::new); entityData.set(SEED, in.getIntOr("WisdomSeed", random.nextInt()));
        entityData.set(RELEASED, in.getBooleanOr("WormsReleased", false)); setNoGravity(true);
    }
}
