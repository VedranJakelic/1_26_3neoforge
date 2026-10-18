package net.beamex.shamaschizm.nabonidus;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class Nabonidus {
    public static final Identifier BLOCK_ID = Shamaschizm.id("stone_eye_of_the_guardian");
    public static final Identifier CURSE_ID = Shamaschizm.id("curse_of_nabonidus");
    public static StoneEyeBlock BLOCK;
    public static Item ITEM;
    public static Holder<MobEffect> CURSE;
    private Nabonidus() {}
    private static final ThreadLocal<Boolean> ALLOW_REMOVAL = ThreadLocal.withInitial(() -> false);

    private static boolean removeCurse(ServerPlayer player) {
        ALLOW_REMOVAL.set(true);
        try { return player.removeEffect(curse()); }
        finally { ALLOW_REMOVAL.remove(); }
    }

    @SubscribeEvent
    public static void preventOtherCures(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Remove event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isAlive()
                && event.getEffect().equals(curse()) && !ALLOW_REMOVAL.get()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.MOB_EFFECT, helper -> helper.register(CURSE_ID, new CurseEffect()));
        event.register(Registries.BLOCK, helper -> {
            BLOCK = new StoneEyeBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, BLOCK_ID)).strength(3.0F, 6.0F));
            helper.register(BLOCK_ID, BLOCK);
        });
        event.register(Registries.ITEM, helper -> {
            ITEM = new BlockItem(BLOCK, new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, BLOCK_ID)).useBlockDescriptionPrefix());
            helper.register(BLOCK_ID, ITEM);
        });
    }

    public static Holder<MobEffect> curse() {
        if (CURSE == null) CURSE = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                .getOrThrow(ResourceKey.create(Registries.MOB_EFFECT, CURSE_ID));
        return CURSE;
    }

    @SubscribeEvent
    public static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(ITEM);
    }

    @SubscribeEvent
    public static void leftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        var state = event.getLevel().getBlockState(event.getPos());
        if (state.is(BLOCK) && state.getValue(StoneEyeBlock.STAGE) == 0) {
            BLOCK.activate(state, event.getLevel(), event.getPos());
            // Preserve the first click even when creative mode would instantly break it.
            event.setCanceled(true);
        }
    }

    public static void cure(ServerPlayer player) {
        if (removeCurse(player)) player.level().playSound(null, player.getX(), player.getY(),
                player.getZ(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @SubscribeEvent
    public static void eat(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getItem().is(Items.ENCHANTED_GOLDEN_APPLE)) cure(player);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        var effect = player.getEffect(curse());
        if (effect == null) return;
        if (player.level().dimension().equals(Level.OVERWORLD)) { cure(player); return; }
        // This runs outside LivingEntity's active-effect iterator. The extra tick
        // compensates for advancement immediately before the old final effect tick.
        if (!effect.isInfiniteDuration() && effect.getDuration() <= 1) {
            int next = Math.min(255, effect.getAmplifier() + 1);
            removeCurse(player);
            player.addEffect(new MobEffectInstance(curse(), 3601, next, false, false, true));
        }
        var health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;
        // Attribute MAX_HEALTH clamps to at least 1. Compute the unclamped value
        // so reaching zero really kills, including other mods' health modifiers.
        double base = health.getBaseValue();
        for (var modifier : health.getModifiers()) if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) base += modifier.amount();
        double value = base;
        for (var modifier : health.getModifiers()) if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) value += base * modifier.amount();
        for (var modifier : health.getModifiers()) if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) value *= 1.0 + modifier.amount();
        if (value <= 0) player.kill(player.level());
        else if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }
}
