package net.beamex.shamaschizm.entity.custom;

import net.beamex.shamaschizm.plantedsword.PlantedSwordEntity;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.UUID;
import net.beamex.shamaschizm.entity.CoffinRegistration;
import net.beamex.shamaschizm.entity.ModEntities;
import net.beamex.shamaschizm.entity.SkeletonVariantRegistration;
import net.beamex.shamaschizm.event.ModItemBootstrap;
import net.beamex.shamaschizm.registry.ModDataComponents;
import net.beamex.shamaschizm.world.block.CoffinCollisionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Persistent two-block animated coffin with one outcome chosen on first opening. */
public final class CoffinEntity extends PathfinderMob {
    public static final int OPEN_ANIMATION_TICKS = 80;
    private static final int OUTCOME_UNDECIDED = -1;
    private static final int OUTCOME_ROACHES = 0;
    private static final int OUTCOME_LOOT = 1;
    private static final int OUTCOME_SKELETON = 2;
    private static final int OUTCOME_LEGENDARY_DISC = 3;
    private static final int DIRT_BREAK_PROGRESS = 4;
    private static final int SKELETON_REVEAL_TICKS = 50;

    private static final EntityDataAccessor<Boolean> OPEN =
            SynchedEntityData.defineId(CoffinEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> OPEN_TICKS =
            SynchedEntityData.defineId(CoffinEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OUTCOME =
            SynchedEntityData.defineId(CoffinEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DIRT_VISIBLE =
            SynchedEntityData.defineId(CoffinEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Direction> FACING =
            SynchedEntityData.defineId(CoffinEntity.class, EntityDataSerializers.DIRECTION);

    private static final EntityDataAccessor<Boolean> SAINTS_LID_GONE =
            SynchedEntityData.defineId(CoffinEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimationState clientOpenAnimation = new AnimationState();
    private int dirtProgress;
    private boolean outcomeResolved;
    private boolean swordTriggered;
    private UUID revealedSkeletonId;
    private int skeletonRevealTicks;

    public CoffinEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void registerGoals() {}

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        super.defineSynchedData(data);
        data.define(OPEN, false);
        data.define(SAINTS_LID_GONE, false);
        data.define(OPEN_TICKS, 0);
        data.define(OUTCOME, OUTCOME_UNDECIDED);
        data.define(DIRT_VISIBLE, false);
        data.define(FACING, Direction.NORTH);
    }

    public boolean isOpen() { return this.entityData.get(OPEN); }
    public int getOpenTicks() { return this.entityData.get(OPEN_TICKS); }
    public boolean isDirtVisible() { return this.entityData.get(DIRT_VISIBLE); }
    public Direction getCoffinFacing() { return this.entityData.get(FACING); }
    public AnimationState getClientOpenAnimation() { return this.clientOpenAnimation; }

    public void setCoffinFacing(Direction direction) {
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

    @Override
    public float rotate(Rotation rotation) {
        this.setCoffinFacing(rotation.rotate(this.getCoffinFacing()));
        return this.getYRot();
    }

    @Override
    public float mirror(Mirror mirror) {
        float before = this.getYRot();
        this.setCoffinFacing(mirror.mirror(this.getCoffinFacing()));
        return 2.0F * this.getYRot() - before;
    }

    public void markSaintsEncounter(UUID id) {
        this.getPersistentData().putString(net.beamex.shamaschizm.saints.CatacombSaints.ENCOUNTER_TAG, id.toString());
    }
    public boolean isSaintsCoffin() {
        return net.beamex.shamaschizm.saints.CatacombSaints.encounterId(this) != null;
    }
    public boolean isSaintsLidRemoved() { return this.entityData.get(SAINTS_LID_GONE); }
    public void removeSaintsLid() { this.entityData.set(SAINTS_LID_GONE, true); }
    public void openForSaints() {
        if(this.level().isClientSide() || this.isOpen()) return;
        this.outcomeResolved=true; this.swordTriggered=false;
        this.entityData.set(OUTCOME, OUTCOME_LOOT);
        this.entityData.set(DIRT_VISIBLE, false);
        this.entityData.set(OPEN, true);this.entityData.set(OPEN_TICKS, 0);
        this.updateCollisionBlocks(true);
        this.playSound(CoffinRegistration.OPEN_SOUND,1.0F,1.0F);
    }
    public void releaseSaintsCollision() {
        this.getPersistentData().putBoolean("SaintsMounted", true);
        this.removeCollisionBlocks();
    }
    public void releasePositiveLootAt(ServerLevel level, Vec3 at) {
        if(this.random.nextInt(20)==0){
            ItemEntity disc=new ItemEntity(level,at.x,at.y+.5,at.z,new ItemStack(net.beamex.shamaschizm.saints.audio.GallopDisc.DISC));
            disc.setDefaultPickUpDelay();level.addFreshEntity(disc);
        }
        for(int i=0;i<3;i++) {
            ItemEntity item=new ItemEntity(level,at.x,at.y+.5,at.z,randomLoot(level));
            item.setDefaultPickUpDelay();level.addFreshEntity(item);
        }
    }

    public InteractionResult tryInteract(Player player, InteractionHand hand) {
        if (this.isSaintsCoffin()) {
            if(!this.level().isClientSide() && !this.isOpen())
                player.sendOverlayMessage(Component.literal("Challenge the saints by taking the central spear."));
            return InteractionResult.SUCCESS;
        }
        if (this.isOpen()) return InteractionResult.PASS;
        if (this.level().isClientSide()) return InteractionResult.SUCCESS;

        if (!this.level().getEntitiesOfClass(PlantedSwordEntity.class,
                this.getBoundingBox().inflate(10.0D),
                sword -> sword.locksCoffins() && sword.distanceToSqr(this) <= 100.0D).isEmpty()) {
            player.sendOverlayMessage(Component.literal("Pull out the nearby weapon to open this coffin."));
            return InteractionResult.SUCCESS_SERVER;
        }

        int roll = this.random.nextInt(100);
        int outcome = roll < 5 ? OUTCOME_LEGENDARY_DISC : roll < 20 ? OUTCOME_ROACHES : roll < 75 ? OUTCOME_LOOT : OUTCOME_SKELETON;
        this.entityData.set(OUTCOME, outcome);
        this.entityData.set(DIRT_VISIBLE, outcome != OUTCOME_ROACHES);
        this.entityData.set(OPEN, true);
        this.entityData.set(OPEN_TICKS, 0);
        this.updateCollisionBlocks(true);
        this.playSound(CoffinRegistration.OPEN_SOUND, 1.0F, 1.0F);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Bypasses the random outcome and dirt digging for a room sword encounter. */
    public void openFromPlantedSword() {
        if (this.isSaintsCoffin()) return;
        if (this.level().isClientSide() || this.isOpen() || this.isRemoved()) return;
        this.swordTriggered = true;
        this.outcomeResolved = false;
        this.entityData.set(OUTCOME, OUTCOME_SKELETON);
        this.entityData.set(DIRT_VISIBLE, false);
        this.entityData.set(OPEN, true);
        this.entityData.set(OPEN_TICKS, 0);
        this.updateCollisionBlocks(true);
        this.playSound(CoffinRegistration.OPEN_SOUND, 1.0F, 1.0F);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult result = this.tryInteract(player, hand);
        return result == InteractionResult.PASS ? super.mobInteract(player, hand) : result;
    }

    /** Called by the invisible shell when the player left-clicks the opened dirt. */
    public void tryDig(Player player) {
        if (this.isSaintsCoffin()) return;
        if (this.level().isClientSide() || !this.isOpen() || !this.isDirtVisible()
                || this.getOpenTicks() < 68 || this.outcomeResolved) return;

        boolean shovel = player.getMainHandItem().is(Items.WOODEN_SHOVEL)
                || player.getMainHandItem().is(Items.STONE_SHOVEL)
                || player.getMainHandItem().is(Items.IRON_SHOVEL)
                || player.getMainHandItem().is(Items.GOLDEN_SHOVEL)
                || player.getMainHandItem().is(Items.DIAMOND_SHOVEL)
                || player.getMainHandItem().is(Items.NETHERITE_SHOVEL);
        this.dirtProgress += shovel ? DIRT_BREAK_PROGRESS : 1;
        this.playSound(SoundEvents.GRAVEL_HIT, 0.75F, 0.9F + this.random.nextFloat() * 0.2F);
        spawnDirtParticles(8);
        if (this.dirtProgress < DIRT_BREAK_PROGRESS) return;

        this.entityData.set(DIRT_VISIBLE, false);
        this.outcomeResolved = true;
        this.playSound(SoundEvents.GRAVEL_BREAK, 1.0F, 0.9F);
        spawnDirtParticles(35);
        if (this.entityData.get(OUTCOME) == OUTCOME_LEGENDARY_DISC) {
            ItemEntity disc=new ItemEntity(this.level(),getX(),getY()+.65,getZ(),new ItemStack(net.beamex.shamaschizm.saints.audio.GallopDisc.DISC));
            disc.setDefaultPickUpDelay();this.level().addFreshEntity(disc);
            releaseLoot((ServerLevel)this.level());
        } else if (this.entityData.get(OUTCOME) == OUTCOME_LOOT) {
            releaseLoot((ServerLevel)this.level());
        } else if (this.entityData.get(OUTCOME) == OUTCOME_SKELETON) {
            releaseSkeleton((ServerLevel)this.level());
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player) this.tryDig(player);
        return false;
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

        if (this.isOpen()) {
            int ticks = Math.min(OPEN_ANIMATION_TICKS, this.getOpenTicks() + 1);
            this.entityData.set(OPEN_TICKS, ticks);
            if (ticks == OPEN_ANIMATION_TICKS && this.swordTriggered && !this.outcomeResolved) {
                releaseSkeleton((ServerLevel)this.level());
                // If spawning was denied, retry instead of resolving an empty coffin.
                this.outcomeResolved = this.revealedSkeletonId != null;
            }
            if (ticks == OPEN_ANIMATION_TICKS
                    && this.entityData.get(OUTCOME) == OUTCOME_ROACHES
                    && !this.outcomeResolved) {
                this.outcomeResolved = true;
                releaseRoaches((ServerLevel)this.level());
            }
        }
        if (this.skeletonRevealTicks > 0 && --this.skeletonRevealTicks == 0) {
            explodeAndAwakenSkeleton((ServerLevel)this.level());
        }
        if (!this.getPersistentData().getBooleanOr("SaintsMounted", false)
                && (this.tickCount <= 1 || this.tickCount % 20 == 0)) installCollisionBlocks();
    }

    private void releaseRoaches(ServerLevel level) {
        for (int i = 0; i < 33; i++) {
            BabyRoachEntity roach = ModEntities.BABY_ROACH.get().create(level, EntitySpawnReason.EVENT);
            if (roach == null) continue;
            double along = this.random.nextDouble() * 1.25D - 0.625D;
            double across = this.random.nextDouble() * 0.45D - 0.225D;
            Vec3 forward = Vec3.atLowerCornerOf(this.getCoffinFacing().getOpposite().getUnitVec3i());
            Vec3 side = Vec3.atLowerCornerOf(this.getCoffinFacing().getClockWise().getUnitVec3i());
            Vec3 position = this.position().add(forward.scale(along)).add(side.scale(across)).add(0.0D, 0.18D, 0.0D);
            roach.snapTo(position.x, position.y, position.z, this.getYRot(), 0.0F);
            roach.setPersistenceRequired();
            level.addFreshEntity(roach);
        }
    }

    private void releaseLoot(ServerLevel level) {
        for (int i = 0; i < 3; i++) {
            ItemStack stack = randomLoot(level);
            ItemEntity item = new ItemEntity(level, this.getX(), this.getY() + 0.65D, this.getZ(), stack);
            item.setDeltaMovement((this.random.nextDouble() - 0.5D) * 0.18D,
                    0.22D + this.random.nextDouble() * 0.08D,
                    (this.random.nextDouble() - 0.5D) * 0.18D);
            level.addFreshEntity(item);
        }
    }

    private ItemStack randomLoot(ServerLevel level) {
        return switch (this.random.nextInt(15)) {
            case 0 -> new ItemStack(Items.EMERALD, 1 + this.random.nextInt(10));
            case 1 -> new ItemStack(Items.GOLD_INGOT, 5 + this.random.nextInt(11));
            case 2 -> severelyDamaged(randomChainmail());
            case 3 -> new ItemStack(Items.DIAMOND, 2 + this.random.nextInt(9));
            case 4 -> new ItemStack(ModItemBootstrap.ANCIENT_BLUEPRINT);
            case 5 -> enchantedIronArmor(level);
            case 6 -> randomEnchantedBook(level);
            case 7 -> damaged(new ItemStack(Items.NETHERITE_AXE), 0.08F, 0.22F);
            case 8 -> new ItemStack(Items.NETHERITE_SCRAP);
            case 9 -> new ItemStack(Items.GOLDEN_APPLE);
            case 10 -> new ItemStack(Items.TRIAL_KEY);
            case 11 -> new ItemStack(Items.OMINOUS_TRIAL_KEY);
            case 12 -> enchantedDiamondSword(level);
            case 13 -> new ItemStack(Items.SPECTRAL_ARROW, 2 + this.random.nextInt(19));
            default -> enchantedBow(level);
        };
    }

    private ItemStack randomChainmail() {
        return new ItemStack(switch (this.random.nextInt(4)) {
            case 0 -> Items.CHAINMAIL_HELMET;
            case 1 -> Items.CHAINMAIL_CHESTPLATE;
            case 2 -> Items.CHAINMAIL_LEGGINGS;
            default -> Items.CHAINMAIL_BOOTS;
        });
    }

    private ItemStack enchantedIronArmor(ServerLevel level) {
        ItemStack stack = new ItemStack(switch (this.random.nextInt(4)) {
            case 0 -> Items.IRON_HELMET;
            case 1 -> Items.IRON_CHESTPLATE;
            case 2 -> Items.IRON_LEGGINGS;
            default -> Items.IRON_BOOTS;
        });
        HolderLookup.RegistryLookup<Enchantment> registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        stack.enchant(registry.getOrThrow(Enchantments.PROTECTION), 7);
        stack.enchant(registry.getOrThrow(Enchantments.UNBREAKING), 3);
        return severelyDamaged(stack);
    }

    private ItemStack randomEnchantedBook(ServerLevel level) {
        List<Holder.Reference<Enchantment>> choices = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).listElements().toList();
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        if (choices.isEmpty()) return book;
        Holder.Reference<Enchantment> enchantment = choices.get(this.random.nextInt(choices.size()));
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(enchantment, 1 + this.random.nextInt(6));
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        return book;
    }

    private ItemStack enchantedDiamondSword(ServerLevel level) {
        ItemStack stack = severelyDamaged(new ItemStack(Items.DIAMOND_SWORD));
        HolderLookup.RegistryLookup<Enchantment> registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        stack.enchant(registry.getOrThrow(Enchantments.SMITE), 6);
        stack.enchant(registry.getOrThrow(Enchantments.SWEEPING_EDGE), 3);
        return stack;
    }

    private ItemStack enchantedBow(ServerLevel level) {
        ItemStack stack = damaged(new ItemStack(Items.BOW), 0.10F, 0.35F);
        HolderLookup.RegistryLookup<Enchantment> registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        stack.enchant(registry.getOrThrow(Enchantments.FLAME), 1);
        stack.enchant(registry.getOrThrow(Enchantments.INFINITY), 1);
        return stack;
    }

    private ItemStack severelyDamaged(ItemStack stack) {
        int remaining = 1 + this.random.nextInt(Math.min(10, Math.max(1, stack.getMaxDamage() - 1)));
        stack.setDamageValue(Math.max(0, stack.getMaxDamage() - remaining));
        return stack;
    }

    private ItemStack damaged(ItemStack stack, float minimumRemaining, float maximumRemaining) {
        int remaining = Math.max(1, Math.round(stack.getMaxDamage()
                * (minimumRemaining + this.random.nextFloat() * (maximumRemaining - minimumRemaining))));
        stack.setDamageValue(stack.getMaxDamage() - remaining);
        return stack;
    }

    private void releaseSkeleton(ServerLevel level) {
        SkeletonVariantEntity skeleton = SkeletonVariantRegistration.TYPE.create(level, EntitySpawnReason.EVENT);
        if (skeleton != null) {
            // A sleeping humanoid model is anchored near its feet rather than its centre.
            // Put that anchor near the closed foot end so the whole body remains in the coffin.
            Vec3 back = Vec3.atLowerCornerOf(this.getCoffinFacing().getOpposite().getUnitVec3i()).scale(1.30D);
            skeleton.snapTo(this.getX() + back.x, this.getY() + 0.18D, this.getZ() + back.z,
                    this.getYRot(), 0.0F);
            skeleton.setPersistenceRequired();
            skeleton.getPersistentData().putBoolean(SkeletonVariantEntity.EQUIPPED_TAG, true);
            equipCoffinSkeleton(level, skeleton);
            skeleton.waitInsideCoffin(this.getCoffinFacing());
            if (!level.addFreshEntity(skeleton)) return;
            this.revealedSkeletonId = skeleton.getUUID();
            this.skeletonRevealTicks = SKELETON_REVEAL_TICKS;
        }
    }

    private void explodeAndAwakenSkeleton(ServerLevel level) {
        if (this.revealedSkeletonId != null
                && level.getEntity(this.revealedSkeletonId) instanceof SkeletonVariantEntity skeleton
                && skeleton.isAlive()) {
            // Return the entity anchor to the coffin centre before it stands upright.
            Vec3 centre = Vec3.atLowerCornerOf(
                    this.getCoffinFacing().getOpposite().getUnitVec3i()).scale(0.45D);
            skeleton.snapTo(this.getX() + centre.x, this.getY() + 0.18D,
                    this.getZ() + centre.z, skeleton.getYRot(), 0.0F);
            skeleton.beginCoffinAwakening(this.getCoffinFacing());
        }
        level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.45D, this.getZ(),
                8, 0.65D, 0.32D, 0.9D, 0.08D);
        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.35F, 0.9F);
        this.removeCollisionBlocks();
        this.discard();
    }

    private void equipCoffinSkeleton(ServerLevel level, SkeletonVariantEntity skeleton) {
        skeleton.setItemSlot(EquipmentSlot.HEAD, enchantedCoffinHelmet(level));
        skeleton.setItemSlot(EquipmentSlot.CHEST, ancientArmor(new ItemStack(Items.NETHERITE_CHESTPLATE)));
        skeleton.setItemSlot(EquipmentSlot.LEGS, ancientArmor(new ItemStack(Items.NETHERITE_LEGGINGS)));
        skeleton.setItemSlot(EquipmentSlot.FEET, ancientArmor(new ItemStack(Items.NETHERITE_BOOTS)));
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND)) {
            skeleton.setDropChance(slot, 0.085F);
        }
    }

    private ItemStack ancientArmor(ItemStack stack) {
        stack.set(ModDataComponents.RAISED_ENCHANT_CAP, true);
        stack.set(ModDataComponents.ANCIENT_APPEARANCE, true);
        return severelyDamaged(stack);
    }

    private ItemStack enchantedCoffinHelmet(ServerLevel level) {
        ItemStack helmet = severelyDamaged(new ItemStack(Items.CHAINMAIL_HELMET));
        HolderLookup.RegistryLookup<Enchantment> registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        helmet.enchant(registry.getOrThrow(Enchantments.THORNS), 3);
        helmet.enchant(registry.getOrThrow(Enchantments.FIRE_PROTECTION), 9);
        return helmet;
    }

    private void spawnDirtParticles(int count) {
        if (!(this.level() instanceof ServerLevel level)) return;
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                this.getX(), this.getY() + 0.48D, this.getZ(), count,
                0.38D, 0.18D, 0.72D, 0.05D);
    }

    public boolean canInstallCollisionBlocks() {
        for (BlockPos pos : collisionPositions()) {
            if (!this.level().getBlockState(pos).canBeReplaced()
                    && !this.level().getBlockState(pos).is(CoffinRegistration.COLLISION)) return false;
        }
        return true;
    }

    public void installCollisionBlocks() {
        BlockPos[] positions = collisionPositions();
        for (int index = 0; index < positions.length; index++) {
            BlockPos pos = positions[index];
            if (this.level().getBlockState(pos).canBeReplaced()
                    || this.level().getBlockState(pos).is(CoffinRegistration.COLLISION)) {
                this.level().setBlock(pos, CoffinRegistration.COLLISION.defaultBlockState()
                        .setValue(CoffinCollisionBlock.FACING, this.getCoffinFacing())
                        .setValue(CoffinCollisionBlock.OPEN, this.isOpen())
                        .setValue(CoffinCollisionBlock.FRONT, index == 0), 3);
            }
        }
    }

    private void updateCollisionBlocks(boolean open) {
        for (BlockPos pos : collisionPositions()) {
            if (this.level().getBlockState(pos).is(CoffinRegistration.COLLISION)) {
                this.level().setBlock(pos, this.level().getBlockState(pos)
                        .setValue(CoffinCollisionBlock.OPEN, open), 3);
            }
        }
    }

    private void removeCollisionBlocks() {
        for (BlockPos pos : collisionPositions()) {
            if (this.level().getBlockState(pos).is(CoffinRegistration.COLLISION)) {
                this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private BlockPos[] collisionPositions() {
        BlockPos base = BlockPos.containing(this.getX(), this.getY(), this.getZ());
        return new BlockPos[] {base, base.relative(this.getCoffinFacing().getOpposite())};
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        double halfWidth = this.getCoffinFacing().getAxis() == Direction.Axis.Z ? 0.48D : 0.98D;
        double halfDepth = this.getCoffinFacing().getAxis() == Direction.Axis.Z ? 0.98D : 0.48D;
        return new AABB(position.x - halfWidth, position.y, position.z - halfDepth,
                position.x + halfWidth, position.y + 0.65D, position.z + halfDepth);
    }

    @Override public boolean isPushable() { return false; }
    @Override public void push(Entity entity) {}
    @Override public void push(double x, double y, double z) {}
    @Override protected void pushEntities() {}
    @Override public boolean removeWhenFarAway(double distanceSquared) { return false; }

    @Override
    public void onRemoval(Entity.RemovalReason reason) {
        if (!this.level().isClientSide() && reason.shouldDestroy()) removeCollisionBlocks();
        super.onRemoval(reason);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("SaintsLidGone", this.isSaintsLidRemoved());
        output.putBoolean("CoffinOpen", this.isOpen());
        output.putInt("CoffinOpenTicks", this.getOpenTicks());
        output.putInt("CoffinOutcome", this.entityData.get(OUTCOME));
        output.putBoolean("CoffinDirt", this.isDirtVisible());
        output.putInt("CoffinDirtProgress", this.dirtProgress);
        output.putBoolean("CoffinResolved", this.outcomeResolved);
        output.putBoolean("SwordTriggered", this.swordTriggered);
        output.putInt("CoffinFacing", this.getCoffinFacing().get2DDataValue());
        output.putInt("SkeletonRevealTicks", this.skeletonRevealTicks);
        if (this.revealedSkeletonId != null) {
            output.putString("RevealedSkeleton", this.revealedSkeletonId.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(SAINTS_LID_GONE, input.getBooleanOr("SaintsLidGone", false));
        this.entityData.set(OPEN, input.getBooleanOr("CoffinOpen", false));
        this.entityData.set(OPEN_TICKS, input.getIntOr("CoffinOpenTicks", 0));
        this.entityData.set(OUTCOME, input.getIntOr("CoffinOutcome", OUTCOME_UNDECIDED));
        this.entityData.set(DIRT_VISIBLE, input.getBooleanOr("CoffinDirt", false));
        this.dirtProgress = input.getIntOr("CoffinDirtProgress", 0);
        this.outcomeResolved = input.getBooleanOr("CoffinResolved", false);
        this.swordTriggered = input.getBooleanOr("SwordTriggered", false);
        this.skeletonRevealTicks = input.getIntOr("SkeletonRevealTicks", 0);
        this.revealedSkeletonId = input.getString("RevealedSkeleton")
                .flatMap(value -> {
                    try { return java.util.Optional.of(UUID.fromString(value)); }
                    catch (IllegalArgumentException ignored) { return java.util.Optional.empty(); }
                }).orElse(null);
        this.setCoffinFacing(Direction.from2DDataValue(
                input.getIntOr("CoffinFacing", Direction.NORTH.get2DDataValue())));
        this.setPersistenceRequired();
    }
}
