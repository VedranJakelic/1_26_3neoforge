package net.beamex.shamaschizm.ritual;

import java.util.ArrayList;
import java.util.List;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.stag.Antlers;
import net.beamex.shamaschizm.world.block.BrazierBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.extensions.IEntityExtension;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Goat sacrifice at a four-block obsidian monolith; all rewards are server-authoritative. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class GloriaRitual {
    private static final String PENDING = "shamaschizm_gloria_sacrifice";
    private static final List<Sacrifice> DEATHS = new ArrayList<>();
    private record Altar(BlockPos base, int candles, int fires) {}
    private record Sacrifice(LivingDeathEvent event, Goat goat, ServerPlayer player,
                             ServerLevel level, Altar altar, boolean antlers) {}
    private GloriaRitual() {}

    /** Individual candles count, including all 1-4 candles in a lit candle block. */
    public static int durationTicks(int candles, int fires) {
        return (Math.max(30, Math.min(180, candles * 15)) + fires * 30) * 60 * 20;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Goat goat)
                || !(goat.level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || player.level() != level || !player.isAlive()) return;
        Altar altar = findAltar(level, goat.position(), player.position());
        if (altar == null) return;
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        // Capture the helmet at the fatal hit, not when the horn is used later.
        DEATHS.add(new Sacrifice(event, goat, player, level, altar,
                Antlers.helmet(helmet) && Antlers.has(helmet)));
    }

    @SubscribeEvent
    public static void afterTick(ServerTickEvent.Post event) { settleDeaths(); }

    private static void settleDeaths() {
        if (DEATHS.isEmpty()) return;
        List<Sacrifice> deaths = List.copyOf(DEATHS);
        DEATHS.clear();
        for (Sacrifice death : deaths) {
            // Wait until all death listeners have run: cancelled deaths grant nothing.
            if (death.event.isCanceled() || !death.goat.isDeadOrDying()
                    || !death.player.isAlive() || death.player.isRemoved()
                    || death.player.level() != death.level) continue;
            int duration = durationTicks(death.altar.candles, death.altar.fires);
            bless(death.player, duration);
            CompoundTag data = ((IEntityExtension) death.player).getPersistentData();
            data.remove(PENDING);
            if (death.antlers) {
                CompoundTag pending = new CompoundTag();
                pending.putLong("Base", death.altar.base.asLong());
                pending.putString("Dimension", death.level.dimension().identifier().toString());
                pending.putLong("Expires", death.level.getGameTime() + duration);
                data.put(PENDING, pending);
            }
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { DEATHS.clear(); }

    private static Altar findAltar(ServerLevel level, Vec3 goat, Vec3 player) {
        BlockPos origin = BlockPos.containing(goat);
        Altar best = null;
        double distance = Double.MAX_VALUE;
        for (int y = -6; y <= 6; y++) for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
            BlockPos base = origin.offset(x, y, z);
            Vec3 center = Vec3.atBottomCenterOf(base);
            double d = center.distanceToSqr(goat);
            if (d > 36 || d >= distance || center.distanceToSqr(player) > 36
                    || !level.hasChunkAt(base) || !isPillar(level, base)) continue;
            Altar altar = countLights(level, base);
            if (altar.candles == 0) continue;
            best = altar;
            distance = d;
        }
        return best;
    }

    private static boolean isPillar(ServerLevel level, BlockPos base) {
        if (level.getBlockState(base.below()).is(Blocks.OBSIDIAN)
                || level.getBlockState(base.above(4)).is(Blocks.OBSIDIAN)) return false;
        for (int y = 0; y < 4; y++) if (!level.getBlockState(base.above(y)).is(Blocks.OBSIDIAN)) return false;
        return true;
    }

    private static Altar countLights(ServerLevel level, BlockPos base) {
        int candles = 0, fires = 0;
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            BlockPos pos = base.offset(x, 0, z);
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!isLit(state)) continue;
            if (state.getBlock() instanceof CandleBlock) candles += state.getValue(CandleBlock.CANDLES);
            else if (state.getBlock() instanceof CampfireBlock || state.getBlock() instanceof BrazierBlock) fires++;
        }
        return new Altar(base.immutable(), candles, fires);
    }

    private static boolean isLit(BlockState state) {
        return state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
    }

    /** Called only from the server's genuine goat-horn use, after vanilla cooldown checks. */
    public static boolean completeWithHorn(ServerPlayer player) {
        settleDeaths(); // A kill and horn use can arrive in the same server tick.
        CompoundTag data = ((IEntityExtension) player).getPersistentData();
        if (!data.contains(PENDING)) return false;
        CompoundTag pending = data.getCompoundOrEmpty(PENDING);
        ServerLevel level = (ServerLevel) player.level();
        if (!player.isAlive() || !pending.getStringOr("Dimension", "").equals(level.dimension().identifier().toString())) return false;
        if (pending.getLongOr("Expires", 0L) <= level.getGameTime()) {
            data.remove(PENDING);
            return false;
        }
        BlockPos base = BlockPos.of(pending.getLongOr("Base", 0L));
        if (Vec3.atBottomCenterOf(base).distanceToSqr(player.position()) > 36
                || !level.hasChunkAt(base) || !isPillar(level, base)
                || !player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) || !player.hasEffect(MobEffects.LUCK)) return false;
        // Consume the authorization first, so one sacrifice can upgrade only once.
        data.remove(PENDING);
        bless(player, MobEffectInstance.INFINITE_DURATION);
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            BlockPos pos = base.offset(x, 0, z);
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!isLit(state)) continue;
            if (state.getBlock() instanceof CandleBlock || state.getBlock() instanceof CampfireBlock
                    || state.getBlock() instanceof BrazierBlock) {
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), 3);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                if (state.getBlock() instanceof BrazierBlock) BrazierBlock.notifyNearbyGates(level, pos);
            }
        }
        for (int y = 0; y < 4; y++) level.setBlock(base.above(y), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
        return true;
    }

    private static void bless(ServerPlayer player, int duration) {
        apply(player, MobEffects.HERO_OF_THE_VILLAGE, duration);
        apply(player, MobEffects.LUCK, duration);
    }

    private static void apply(ServerPlayer player, Holder<MobEffect> effect, int duration) {
        MobEffectInstance old = player.getEffect(effect);
        int amplifier = old == null ? 0 : old.getAmplifier();
        if (old != null) duration = old.isInfiniteDuration() || duration == MobEffectInstance.INFINITE_DURATION
                ? MobEffectInstance.INFINITE_DURATION : Math.max(duration, old.getDuration());
        // Replacing guarantees no potion particles, even if an earlier copy was visible.
        player.removeEffect(effect);
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, true));
    }
}
