package net.beamex.shamaschizm.corpse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Holds the actual captured death drops until their vanilla item lifetime expires. */
public final class CorpseSkeletonEntity extends Skeleton {
    private static final int DESPAWN_TICKS = 6000;
    private static final EntityDataAccessor<Boolean> LYING =
            SynchedEntityData.defineId(CorpseSkeletonEntity.class, EntityDataSerializers.BOOLEAN);
    private final List<ItemStack> storedItems = new ArrayList<>();
    private final List<ItemStack> wornArmor = new ArrayList<>();
    private int itemAge;
    private UUID originalPlayer;

    public CorpseSkeletonEntity(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        super.defineSynchedData(data);
        data.define(LYING, false);
    }

    public boolean isLying() { return this.entityData.get(LYING); }

    public void storeDeathDrops(List<ItemStack> items, List<ItemStack> worn, UUID playerId) {
        if (this.level().isClientSide() || this.isLying()) return;
        for (ItemStack item : items) if (!item.isEmpty()) this.storedItems.add(item.copy());
        for (ItemStack armor : worn) if (!armor.isEmpty()) this.wornArmor.add(armor.copy());
        this.itemAge = 0;
        this.originalPlayer = playerId;
        this.entityData.set(LYING, true);
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setPose(Pose.SLEEPING);
        this.setDeltaMovement(Vec3.ZERO);
        this.setAggressive(false);
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isLying() && this.level() instanceof ServerLevel activeLevel
                && this.tickCount % 20 == 0 && this.originalPlayer != null) {
            ServerPlayer owner = activeLevel.getServer().getPlayerList().getPlayer(this.originalPlayer);
            if (owner != null && owner.isAlive() && owner.level() == activeLevel
                    && this.distanceToSqr(owner) < 48.0D * 48.0D) this.setTarget(owner);
        }
        if (!this.isLying()) return;
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setPose(Pose.SLEEPING);
        this.setDeltaMovement(Vec3.ZERO);
        this.clearFire();
        if (!(this.level() instanceof ServerLevel level)) return;
        if (++this.itemAge >= DESPAWN_TICKS) {
            this.expireItems();
        } else if (this.tickCount % 8 == 0
                && !level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(8.0D)).isEmpty()) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY() + 0.25D, this.getZ(),
                    2, 0.4D, 0.12D, 0.4D, 0.003D);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!this.isLying()) return super.hurtServer(level, source, amount);
        if (source.getDirectEntity() instanceof ServerPlayer player) this.returnItems(player);
        return false;
    }

    private void returnItems(ServerPlayer player) {
        if (!this.isLying() || this.isRemoved()) return;
        this.entityData.set(LYING, false);
        for (ItemStack saved : this.storedItems) {
            ItemStack stack = saved.copy();
            player.getInventory().add(stack);
            if (!stack.isEmpty()) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        this.storedItems.clear();
        this.discard();
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    private void expireItems() {
        if (!this.isLying()) return;
        // Match each worn armor piece to a real captured drop. Vanishing items
        // are absent from the drops and are never recreated here.
        for (ItemStack preferred : this.wornArmor) {
            var equipment = preferred.get(DataComponents.EQUIPPABLE);
            if (equipment == null || !isArmorSlot(equipment.slot())) continue;
            if (!this.getItemBySlot(equipment.slot()).isEmpty()) continue;
            for (ItemStack available : this.storedItems) {
                if (ItemStack.isSameItemSameComponents(preferred, available)) {
                    this.setItemSlot(equipment.slot(), available.copyWithCount(1));
                    break;
                }
            }
        }
        for (ItemStack original : this.storedItems) {
            ItemStack stack = original.copy();
            var equipment = stack.get(DataComponents.EQUIPPABLE);
            if (equipment != null) {
                EquipmentSlot slot = equipment.slot();
                if (isArmorSlot(slot) && this.getItemBySlot(slot).isEmpty()) {
                    this.setItemSlot(slot, stack.copyWithCount(1));
                    continue;
                }
            }
        }
        for (ItemStack original : this.storedItems) {
            if (isWeapon(original)) {
                this.setItemSlot(EquipmentSlot.MAINHAND, original.copyWithCount(1));
                break;
            }
        }
        this.storedItems.clear();
        this.wornArmor.clear();
        this.entityData.set(LYING, false);
        this.setPose(Pose.STANDING);
        this.clearSleepingPos();
        this.setNoGravity(false);
        this.setNoAi(false);
        this.reassessWeaponGoal();
    }

    private static boolean isWeapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.SPEARS)
                || stack.is(ItemTags.AXES) || stack.is(Items.BOW)
                || stack.is(Items.CROSSBOW) || stack.is(Items.TRIDENT)
                || stack.is(Items.MACE);
    }
    private static boolean isArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return this.isLying() ? false : super.removeWhenFarAway(distanceSquared);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("LyingWithDrops", this.isLying());
        output.putInt("StoredItemAge", this.itemAge);
        if (this.originalPlayer != null) output.putString("OriginalPlayer", this.originalPlayer.toString());
        if (!this.storedItems.isEmpty()) {
            output.store("StoredDeathItems", ItemStack.CODEC.listOf(), List.copyOf(this.storedItems));
        }
        if (!this.wornArmor.isEmpty())
            output.store("OriginalWornArmor", ItemStack.CODEC.listOf(), List.copyOf(this.wornArmor));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.storedItems.clear();
        this.storedItems.addAll(input.read("StoredDeathItems", ItemStack.CODEC.listOf()).orElse(List.of()));
        this.wornArmor.clear();
        this.wornArmor.addAll(input.read("OriginalWornArmor", ItemStack.CODEC.listOf()).orElse(List.of()));
        this.itemAge = Math.max(0, input.getIntOr("StoredItemAge", 0));
        this.originalPlayer = input.getString("OriginalPlayer").map(value -> {
            try { return UUID.fromString(value); }
            catch (IllegalArgumentException ignored) { return null; }
        }).orElse(null);
        this.entityData.set(LYING, input.getBooleanOr("LyingWithDrops", false));
        if (this.isLying()) {
            this.setNoAi(true);
            this.setNoGravity(true);
            this.setPose(Pose.SLEEPING);
        }
    }
}
