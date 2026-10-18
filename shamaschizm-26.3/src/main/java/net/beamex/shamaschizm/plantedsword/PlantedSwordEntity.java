package net.beamex.shamaschizm.plantedsword;

import net.beamex.shamaschizm.entity.custom.CoffinEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

/** A stored sword, not an ItemEntity: no aging, automatic pickup, gravity or mob AI. */
public final class PlantedSwordEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> SWORD =
            SynchedEntityData.defineId(PlantedSwordEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> LEAN_X =
            SynchedEntityData.defineId(PlantedSwordEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LEAN_Z =
            SynchedEntityData.defineId(PlantedSwordEntity.class, EntityDataSerializers.FLOAT);

    private boolean generatedInRoom;
    private java.util.UUID saintsEncounter;

    public void bindSaintsEncounter(java.util.UUID id) { this.saintsEncounter = id; }
    public java.util.UUID saintsEncounterId() { return this.saintsEncounter; }

    public PlantedSwordEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setPermanentlyInvulnerable(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        data.define(SWORD, ItemStack.EMPTY);
        data.define(LEAN_X, 0.0F);
        data.define(LEAN_Z, 0.0F);
    }

    public void configure(ItemStack sword, float leanX, float leanZ) {
        this.entityData.set(SWORD, sword.copyWithCount(1));
        this.entityData.set(LEAN_X, leanX);
        this.entityData.set(LEAN_Z, leanZ);
    }

    /** Called only for new entities placed by the dungeon generator. */
    public void markGeneratedInRoom() { this.generatedInRoom = true; }

    public boolean locksCoffins() {
        return this.generatedInRoom && !this.isRemoved() && !this.sword().isEmpty();
    }

    public ItemStack sword() { return this.entityData.get(SWORD); }
    public float leanX() { return this.entityData.get(LEAN_X); }
    public float leanZ() { return this.entityData.get(LEAN_Z); }

    @Override
    public void tick() {
        // Deliberately no baseTick: this stationary display cannot burn, enter
        // portals or be removed by environmental/void ticking. Chunk saving is
        // handled by the normal Entity machinery, independent of baseTick.
        this.setDeltaMovement(Vec3.ZERO);
        this.clearFire();
        if (PlantedSwords.isSpear(this.sword())) {
            // Pickable along the longer shaft; still entirely non-colliding.
            this.setBoundingBox(new AABB(this.getX() - 0.4D, this.getY(), this.getZ() - 0.4D,
                    this.getX() + 0.4D, this.getY() + 2.85D, this.getZ() + 0.4D));
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.isSpectator() || this.isRemoved() || this.sword().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide()) return InteractionResult.SUCCESS;
        // Vanilla's entity-use packet also checks reach. Keep a conservative
        // second check here for direct calls from other mods.
        if (player.distanceToSqr(this) > 36.0D) return InteractionResult.PASS;
        if (this.saintsEncounter != null) {
            var encounter=net.beamex.shamaschizm.saints.CatacombEncounterEntity.resolveChallenge((ServerLevel)this.level(),this);
            if(encounter!=null)encounter.activate(player);
            else player.sendOverlayMessage(Component.literal("The saints' arena is incomplete or not fully loaded. All four sealed coffins must be nearby."));
            return InteractionResult.SUCCESS_SERVER;
        }
        ItemStack restored = this.sword().copy();
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, restored);
        } else {
            // Only insert into an empty inventory slot. No partial insertion,
            // creative-mode discard, dropping, or duplication on a full bag.
            int slot = player.getInventory().getFreeSlot();
            if (slot < 0) {
                player.sendOverlayMessage(Component.literal("Make room in your inventory to pick up the weapon."));
                return InteractionResult.SUCCESS_SERVER;
            }
            player.getInventory().setItem(slot, restored);
        }
        // Capture provenance before consuming the stored sword. A failed pickup
        // returns above and cannot trigger any coffins.
        boolean releaseCoffins = this.locksCoffins();
        this.entityData.set(SWORD, ItemStack.EMPTY);
        if (releaseCoffins) {
            for (CoffinEntity coffin : this.level().getEntitiesOfClass(CoffinEntity.class,
                    this.getBoundingBox().inflate(10.0D),
                    coffin -> !coffin.isOpen() && !coffin.isRemoved()
                            && coffin.distanceToSqr(this) <= 100.0D)) {
                coffin.openFromPlantedSword();
            }
        }
        this.discard();
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (!this.sword().isEmpty()) output.store("Sword", ItemStack.CODEC, this.sword());
        if (this.saintsEncounter != null) output.putString("SaintsEncounter", this.saintsEncounter.toString());
        output.putBoolean("GeneratedInRoom", this.generatedInRoom);
        output.putFloat("LeanX", this.leanX());
        output.putFloat("LeanZ", this.leanZ());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        try { this.saintsEncounter = java.util.UUID.fromString(input.getString("SaintsEncounter").orElse("")); }
        catch (IllegalArgumentException ignored) { this.saintsEncounter = null; }
        this.generatedInRoom = input.getBooleanOr("GeneratedInRoom", false);
        this.entityData.set(SWORD, input.read("Sword", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        this.entityData.set(LEAN_X, input.getFloatOr("LeanX", 0.0F));
        this.entityData.set(LEAN_Z, input.getFloatOr("LeanZ", 0.0F));
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setPermanentlyInvulnerable(true);
    }

    @Override public boolean isPickable() { return !this.isRemoved(); }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith(Entity other) { return false; }
    @Override public boolean canCollideWith(Entity other) { return false; }
    @Override public void push(Entity other) {}
    @Override public void push(double x, double y, double z) {}
    @Override public void move(MoverType type, Vec3 movement) {}
    @Override public PushReaction getPistonPushReaction() { return PushReaction.IGNORE_ENTITY; }
    @Override public boolean isIgnoringBlockTriggers() { return true; }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float damage) { return false; }
}
