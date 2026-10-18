package net.beamex.shamaschizm.entity.custom;

import net.beamex.shamaschizm.menu.ShamanTradeMenu;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

public class ShamanEntity extends PathfinderMob implements Merchant, MenuProvider {

    // ---- Nearby-player guidance ----
    // These values are deliberately kept here so the timing/radius is easy to tune later.
    private static final double GUIDANCE_RADIUS = 10.0D;
    private static final int GUIDANCE_CHECK_INTERVAL_TICKS = 20;
    private static final int LINGER_TICKS_REQUIRED = 30 * 20;
    private static final long GUIDANCE_SPEECH_PAUSE_TICKS = 4 * 20L;
    private static final long PLAYER_GUIDANCE_COOLDOWN_TICKS = 10 * 60 * 20L;
    private static final long FAILED_SEARCH_RETRY_TICKS = 10 * 60 * 20L;
    private static final int MAX_STRUCTURE_DISTANCE_BLOCKS = 4000;
    private static final int MAX_STRUCTURE_DISTANCE_CHUNKS = (MAX_STRUCTURE_DISTANCE_BLOCKS + 15) / 16;
    private static final TagKey<Structure> AGARTHA_STRUCTURES = TagKey.create(
            Registries.STRUCTURE,
            Identifier.fromNamespaceAndPath("shamaschizm", "agartha")
    );

    // ---- Sound ids ----
    private static final Identifier SHAMAN1_ID = Identifier.tryParse("shamaschizm:shaman1");
    private static final Identifier SHAMAN2_ID = Identifier.tryParse("shamaschizm:shaman2");
    private static final Identifier HURT1_ID   = Identifier.tryParse("shamaschizm:shamanhurt1");
    private static final Identifier HURT2_ID   = Identifier.tryParse("shamaschizm:shamanhurt2");
    private static final Identifier HURT3_ID   = Identifier.tryParse("shamaschizm:shamanhurt3");
    private static final Identifier DEATH_ID   = Identifier.tryParse("shamaschizm:shamandeath");

    // Optional client anim hook
    public final net.minecraft.world.entity.AnimationState idleAnimationState =
            new net.minecraft.world.entity.AnimationState();

    private final MerchantOffers offers = new MerchantOffers();
    private @Nullable Player tradingPlayer;
    private int villagerXp = 0;
    private final Map<UUID, Integer> playerLingerTicks = new HashMap<>();
    private final Map<UUID, Long> playerGuidanceCooldowns = new HashMap<>();
    private final Map<UUID, Integer> playerGuidanceStages = new HashMap<>();
    private final Map<UUID, Long> nextGuidanceStageTimes = new HashMap<>();
    private @Nullable BlockPos cachedAgarthaPosition;
    private @Nullable Identifier cachedAgarthaBiome;
    private long retryAgarthaSearchAfter;

    public ShamanEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); setPersistenceRequired(); }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override public Component getDisplayName() { return Component.literal("Shaman"); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
        return new ShamanTradeMenu(id, inv, this);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (sp.level().isClientSide()) return InteractionResult.SUCCESS;

        if (!isAlive() || (tradingPlayer != null && tradingPlayer != sp)) return InteractionResult.PASS;
        this.setTradingPlayer(sp);
        if (this.offers.isEmpty()) buildOffers();
        if (sp.openMenu(this).isPresent()) {
            sp.connection.send(new ClientboundMerchantOffersPacket(
                    sp.containerMenu.containerId, this.offers, 1, this.villagerXp, false, false));
        } else {
            this.setTradingPlayer(null);
        }

        return InteractionResult.CONSUME;
    }

    // ---- Merchant impl ----
    @Override public MerchantOffers getOffers() { if (this.offers.isEmpty()) buildOffers(); return this.offers; }
    @Override public void overrideOffers(MerchantOffers newOffers) { this.offers.clear(); if (newOffers != null) this.offers.addAll(newOffers); }
    @Override public void notifyTrade(MerchantOffer offer) {}
    @Override public void notifyTradeUpdated(ItemStack stack) {}
    @Override public int getVillagerXp() { return this.villagerXp; }
    @Override public void overrideXp(int xp) { this.villagerXp = Math.max(0, xp); }
    @Override public boolean showProgressBar() { return false; }

    @Override public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }
    @Override public @Nullable Player getTradingPlayer() { return this.tradingPlayer; }
    @Override public void setTradingPlayer(@Nullable Player player) { this.tradingPlayer = player; }
    @Override public boolean isClientSide() { return this.level().isClientSide(); }

    @Override
    public boolean stillValid(Player pPlayer) {
        return isAlive() && tradingPlayer == pPlayer && distanceToSqr(pPlayer) <= 64.0;
    }

    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public void push(double x, double y, double z) { }
    @Override public void move(net.minecraft.world.entity.MoverType type, net.minecraft.world.phys.Vec3 movement) {
        // Remain at the generated horizontal position; retain gravity and ordinary damage behavior.
        super.move(type, new net.minecraft.world.phys.Vec3(0, movement.y, 0));
    }


    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("Offers", MerchantOffers.CODEC, getOffers());
        output.storeNullable("AgarthaPosition", BlockPos.CODEC, this.cachedAgarthaPosition);
        output.storeNullable("AgarthaBiome", Identifier.CODEC, this.cachedAgarthaBiome);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("Offers", MerchantOffers.CODEC).ifPresent(this::overrideOffers);
        this.cachedAgarthaPosition = input.read("AgarthaPosition", BlockPos.CODEC).orElse(null);
        this.cachedAgarthaBiome = input.read("AgarthaBiome", Identifier.CODEC).orElse(null);
        setPersistenceRequired();
        setTradingPlayer(null);
    }

    // ---- Offers ----
    private void buildOffers() {
        this.offers.clear();

        Item soulIcon     = BuiltInRegistries.ITEM.getValue(Identifier.tryParse("shamaschizm:soul_icon"));
        Item highSoulIcon = BuiltInRegistries.ITEM.getValue(Identifier.tryParse("shamaschizm:high_soul_icon"));
        if (soulIcon == null) soulIcon = Items.AMETHYST_SHARD;         // safe fallback
        if (highSoulIcon == null) highSoulIcon = Items.AMETHYST_SHARD; // safe fallback

        // Books (Soul → VI)
        ItemStack fortune6   = makeBook(Enchantments.FORTUNE,   6);
        ItemStack power6     = makeBook(Enchantments.POWER,     6);
        ItemStack sharpness6 = makeBook(Enchantments.SHARPNESS, 6);

        this.offers.add(new MerchantOffer(new ItemCost(soulIcon, 1), java.util.Optional.empty(), fortune6,   0, 999, 0, 0.0f));
        this.offers.add(new MerchantOffer(new ItemCost(soulIcon, 1), java.util.Optional.empty(), power6,     0, 999, 0, 0.0f));
        this.offers.add(new MerchantOffer(new ItemCost(soulIcon, 1), java.util.Optional.empty(), sharpness6, 0, 999, 0, 0.0f));

        // New mundane trades
        // [2 feathers -> 1 rabbit foot]
        this.offers.add(new MerchantOffer(new ItemCost(Items.FEATHER, 2), java.util.Optional.empty(),
                new ItemStack(Items.RABBIT_FOOT, 1), 0, 999, 0, 0.0f));
        // [4 gold nuggets -> 1 mossy cobblestone]
        this.offers.add(new MerchantOffer(new ItemCost(Items.GOLD_NUGGET, 4), java.util.Optional.empty(),
                new ItemStack(Items.MOSSY_COBBLESTONE, 1), 0, 999, 0, 0.0f));
        // [1 fermented spider eye -> 2 bones]
        this.offers.add(new MerchantOffer(new ItemCost(Items.FERMENTED_SPIDER_EYE, 1), java.util.Optional.empty(),
                new ItemStack(Items.BONE, 2), 0, 999, 0, 0.0f));

        // 3) Second-to-last: High soul → Ancient Upgrade (menu gates tripping)
        this.offers.add(new net.minecraft.world.item.trading.MerchantOffer(
                new net.minecraft.world.item.trading.ItemCost(highSoulIcon, 1),
                java.util.Optional.empty(),
                ancientBlueprintStack(),  // <- lazy, registry-safe
                0, 999, 0, 0.0f
        ));

        // Nether Star → Soul (UI ghost; menu updates backend souls)
        this.offers.add(new MerchantOffer(new ItemCost(Items.NETHER_STAR, 1), java.util.Optional.empty(),
                new ItemStack(soulIcon, 1), 0, 999, 0, 0.0f));
    }

    private ItemStack makeBook(ResourceKey<Enchantment> key, int lvl) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        HolderLookup.RegistryLookup<Enchantment> reg = this.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder.Reference<Enchantment> ref = reg.getOrThrow(key);
        ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mut.set(ref, lvl);
        book.set(DataComponents.STORED_ENCHANTMENTS, mut.toImmutable());
        return book;
    }

    // ---- Sounds ----
    @Override public int getAmbientSoundInterval() { return 160; }
    @Override protected SoundEvent getAmbientSound() {
        SoundEvent s1 = BuiltInRegistries.SOUND_EVENT.getValue(SHAMAN1_ID);
        SoundEvent s2 = BuiltInRegistries.SOUND_EVENT.getValue(SHAMAN2_ID);
        if (s1 == null && s2 == null) return null;
        if (s1 != null && s2 != null) return this.random.nextBoolean() ? s1 : s2;
        return (s1 != null) ? s1 : s2;
    }
    @Override public void playAmbientSound() {
        SoundEvent sound = getAmbientSound();
        if (sound != null) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    sound, getSoundSource(), 1.0F, 1.0F);
        }
    }
    @Override public SoundSource getSoundSource() { return SoundSource.NEUTRAL; }

    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        SoundEvent pick = pickHurtSound();
        return (pick != null) ? pick : super.getHurtSound(source);
    }

    @Override
    protected void playHurtSound(DamageSource source) {
        SoundEvent pick = pickHurtSound();
        if (pick != null) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    pick, getSoundSource(), 1.0F, this.getVoicePitch());
        } else {
            super.playHurtSound(source);
        }
    }

    @Override
    public SoundEvent getDeathSound() {
        SoundEvent death = resolvePackEvent(DEATH_ID);
        return (death != null) ? death : super.getDeathSound();
    }

    private SoundEvent pickHurtSound() {
        SoundEvent a = resolvePackEvent(HURT1_ID);
        SoundEvent b = resolvePackEvent(HURT2_ID);
        SoundEvent c = resolvePackEvent(HURT3_ID);
        SoundEvent[] pool = new SoundEvent[]{a, b, c};
        for (int i = 0; i < 3; i++) {
            SoundEvent p = pool[this.random.nextInt(pool.length)];
            if (p != null) return p;
        }
        return (a != null) ? a : (b != null ? b : c);
    }

    private static SoundEvent resolvePackEvent(Identifier id) {
        if (id == null) return null;
        SoundEvent reg = BuiltInRegistries.SOUND_EVENT.getValue(id);
        if (reg != null) return reg;
        return SoundEvent.createVariableRangeEvent(id);
    }

    private static net.minecraft.world.item.ItemStack ancientBlueprintStack() {
        // Resolve by ID at call time; avoids early static access before registry assigns IDs
        var id = net.beamex.shamaschizm.event.ModItemBootstrap.ANCIENT_BLUEPRINT_ID;
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id);
        if (item == null) {
            // Fail soft: no crash at load; trade simply won’t show until registry is ready
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        return new net.minecraft.world.item.ItemStack(item);
    }

    // ---- Head-only turning + idle drive ----
    @Override
    public void tick() {
        float keepBodyYaw = this.yBodyRot;
        super.tick();
        this.yBodyRot  = keepBodyYaw;
        this.yBodyRotO = keepBodyYaw;

        if (this.level().isClientSide()) {
            if (isIdleAnimationEnabled()) this.idleAnimationState.startIfStopped(this.tickCount);
            else this.idleAnimationState.stop();
        } else if (this.level() instanceof ServerLevel serverLevel
                && this.tickCount % GUIDANCE_CHECK_INTERVAL_TICKS == 0) {
            tickPlayerGuidance(serverLevel);
        }
    }

    private void tickPlayerGuidance(ServerLevel level) {
        long gameTime = level.getGameTime();
        double radiusSqr = GUIDANCE_RADIUS * GUIDANCE_RADIUS;
        Set<UUID> nearbyPlayers = new HashSet<>();

        for (ServerPlayer player : level.getPlayers(candidate ->
                candidate.isAlive()
                        && !candidate.isSpectator()
                        && candidate.distanceToSqr(this) <= radiusSqr)) {
            UUID playerId = player.getUUID();
            nearbyPlayers.add(playerId);

            Integer guidanceStage = this.playerGuidanceStages.get(playerId);
            if (guidanceStage != null) {
                if (gameTime >= this.nextGuidanceStageTimes.getOrDefault(playerId, Long.MAX_VALUE)) {
                    advanceAgarthaGuidance(level, player, guidanceStage, gameTime);
                }
                continue;
            }

            if (gameTime < this.playerGuidanceCooldowns.getOrDefault(playerId, 0L)) {
                continue;
            }

            int lingerTicks = this.playerLingerTicks.getOrDefault(playerId, 0)
                    + GUIDANCE_CHECK_INTERVAL_TICKS;
            if (lingerTicks >= LINGER_TICKS_REQUIRED) {
                beginAgarthaGuidance(player, gameTime);
                this.playerLingerTicks.put(playerId, 0);
            } else {
                this.playerLingerTicks.put(playerId, lingerTicks);
            }
        }

        // A player must linger continuously; walking out of range resets the timer.
        this.playerLingerTicks.keySet().removeIf(playerId -> !nearbyPlayers.contains(playerId));
        this.playerGuidanceStages.keySet().removeIf(playerId -> !nearbyPlayers.contains(playerId));
        this.nextGuidanceStageTimes.keySet().removeIf(playerId -> !nearbyPlayers.contains(playerId));
        this.playerGuidanceCooldowns.entrySet().removeIf(entry ->
                entry.getValue() <= gameTime && !nearbyPlayers.contains(entry.getKey()));
    }

    private void beginAgarthaGuidance(ServerPlayer player, long gameTime) {
        UUID playerId = player.getUUID();
        sendShamanDialogue(player,
                Component.translatable("dialogue.shamaschizm.shaman.agartha.search_start"));
        this.playerGuidanceStages.put(playerId, 1);
        this.nextGuidanceStageTimes.put(playerId, gameTime + GUIDANCE_SPEECH_PAUSE_TICKS);
    }

    private void advanceAgarthaGuidance(ServerLevel level, ServerPlayer player,
                                        int stage, long gameTime) {
        UUID playerId = player.getUUID();
        if (stage == 1) {
            sendShamanDialogue(player,
                    Component.translatable("dialogue.shamaschizm.shaman.agartha.search_wait"));
            this.playerGuidanceStages.put(playerId, 2);
            this.nextGuidanceStageTimes.put(playerId, gameTime + GUIDANCE_SPEECH_PAUSE_TICKS);
            return;
        }

        speakAgarthaGuidance(level, player);
        this.playerGuidanceStages.remove(playerId);
        this.nextGuidanceStageTimes.remove(playerId);
        this.playerGuidanceCooldowns.put(playerId, gameTime + PLAYER_GUIDANCE_COOLDOWN_TICKS);
    }

    private void speakAgarthaGuidance(ServerLevel level, ServerPlayer player) {
        BlockPos target = findOrGetAgartha(level);
        Component dialogue;

        if (target == null) {
            dialogue = Component.translatable("dialogue.shamaschizm.shaman.agartha.none");
        } else {
            double deltaX = target.getX() - player.getX();
            double deltaZ = target.getZ() - player.getZ();
            double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            Component direction = Component.translatable(directionTranslationKey(deltaX, deltaZ));
            Component biome = biomeName(this.cachedAgarthaBiome);

            String distanceKey = distance <= 1000.0D ? "close"
                    : distance <= 2000.0D ? "somewhat_nearby"
                    : distance <= 3000.0D ? "far"
                    : "very_far";
            dialogue = Component.translatable(
                    "dialogue.shamaschizm.shaman.agartha." + distanceKey,
                    direction,
                    biome
            );
        }

        sendShamanDialogue(player, dialogue);
    }

    private void sendShamanDialogue(ServerPlayer player, Component dialogue) {
        this.playAmbientSound();
        player.sendSystemMessage(Component.translatable(
                "dialogue.shamaschizm.shaman.prefix",
                this.getDisplayName(),
                dialogue
        ));
    }

    private @Nullable BlockPos findOrGetAgartha(ServerLevel level) {
        if (this.cachedAgarthaPosition != null) {
            return this.cachedAgarthaPosition;
        }
        if (level.getGameTime() < this.retryAgarthaSearchAfter) {
            return null;
        }

        BlockPos found = level.findNearestMapStructure(
                AGARTHA_STRUCTURES,
                this.blockPosition(),
                structureSearchRadius(level),
                false
        );
        if (found == null || horizontalDistanceSqr(this.blockPosition(), found)
                > (long) MAX_STRUCTURE_DISTANCE_BLOCKS * MAX_STRUCTURE_DISTANCE_BLOCKS) {
            this.retryAgarthaSearchAfter = level.getGameTime() + FAILED_SEARCH_RETRY_TICKS;
            return null;
        }

        this.cachedAgarthaPosition = found.immutable();
        Holder<Biome> biome = level.getUncachedNoiseBiome(
                QuartPos.fromBlock(found.getX()),
                QuartPos.fromBlock(level.getSeaLevel()),
                QuartPos.fromBlock(found.getZ())
        );
        this.cachedAgarthaBiome = biome.unwrapKey().map(ResourceKey::identifier).orElse(null);
        return this.cachedAgarthaPosition;
    }

    /**
     * Minecraft's locate radius counts random-spread placement regions, rather than literal
     * chunks. Deriving it from each placement's spacing keeps this search close to 4,000
     * blocks and avoids accidentally scanning 250 full placement regions.
     */
    private static int structureSearchRadius(ServerLevel level) {
        HolderSet.Named<Structure> structures = level.registryAccess()
                .lookupOrThrow(Registries.STRUCTURE)
                .get(AGARTHA_STRUCTURES)
                .orElse(null);
        if (structures == null) {
            return 0;
        }

        int searchRadius = 0;
        for (Holder<Structure> structure : structures) {
            for (StructurePlacement placement : level.getChunkSource()
                    .getGeneratorState()
                    .getPlacementsForStructure(structure)) {
                if (placement instanceof RandomSpreadStructurePlacement randomSpread) {
                    searchRadius = Math.max(searchRadius,
                            (MAX_STRUCTURE_DISTANCE_CHUNKS + randomSpread.spacing() - 1)
                                    / randomSpread.spacing());
                }
            }
        }
        return searchRadius;
    }

    private static long horizontalDistanceSqr(BlockPos first, BlockPos second) {
        long deltaX = (long) first.getX() - second.getX();
        long deltaZ = (long) first.getZ() - second.getZ();
        return deltaX * deltaX + deltaZ * deltaZ;
    }

    private static Component biomeName(@Nullable Identifier biomeId) {
        if (biomeId == null) {
            return Component.translatable("dialogue.shamaschizm.shaman.agartha.unknown_biome");
        }
        return Component.translatable("biome." + biomeId.getNamespace() + "." + biomeId.getPath());
    }

    private static String directionTranslationKey(double deltaX, double deltaZ) {
        int direction = Math.floorMod(
                (int) Math.floor((Math.atan2(deltaZ, deltaX) + Math.PI / 8.0D) / (Math.PI / 4.0D)),
                8
        );
        return switch (direction) {
            case 0 -> "direction.shamaschizm.east";
            case 1 -> "direction.shamaschizm.southeast";
            case 2 -> "direction.shamaschizm.south";
            case 3 -> "direction.shamaschizm.southwest";
            case 4 -> "direction.shamaschizm.west";
            case 5 -> "direction.shamaschizm.northwest";
            case 6 -> "direction.shamaschizm.north";
            default -> "direction.shamaschizm.northeast";
        };
    }

    public boolean isIdleAnimationEnabled() {
        return !this.isDeadOrDying() && !this.isAggressive() && this.getTarget() == null
                && this.getDeltaMovement().lengthSqr() < 1.0E-4;
    }
}
