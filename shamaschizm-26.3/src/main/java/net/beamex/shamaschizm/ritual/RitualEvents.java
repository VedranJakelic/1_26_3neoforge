package net.beamex.shamaschizm.ritual;

import java.util.List;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Pattern coordinates were extracted from ritualcircle.nbt; the anchor is the origin. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class RitualEvents {
    private RitualEvents() {}

    // Relative to the anchor (0,0,0). The anchor is one block above crying obsidian.
    // Snow *type*, air positions, rotation, candle count and candle color are not checked.
    private static final int[][] CANDLES = {
        {-3,-1,-1}, {-3,-1,0}, {-3,-1,1},
        {-2,-1,-2}, {-2,-1,2},
        {-1,-1,-3}, {-1,-1,3},
        {0,-1,-3}, {0,-1,3},
        {1,-1,-3}, {1,-1,3},
        {2,-1,-2}, {2,-1,2},
        {3,-1,-1}, {3,-1,0}, {3,-1,1}
    };
    private static final int[][] CORNERS = {{-3,-3},{-3,3},{3,-3},{3,3}};

    // Called before the vanilla anchor can explode. Only valid circles are intercepted.
    @SubscribeEvent
    public static void onAnchorUse(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState anchor = level.getBlockState(pos);
        if (!anchor.is(Blocks.RESPAWN_ANCHOR)) return;
        if (!event.getItemStack().is(Items.GLOWSTONE)) return;
        if (!isRitual(level, pos, false)) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(level instanceof ServerLevel server)) return;

        int charges = anchor.getValue(RespawnAnchorBlock.CHARGE);
        Player player = event.getEntity();
        if (charges >= 4) return;
        if (!player.getAbilities().instabuild) event.getItemStack().shrink(1);
        if (charges < 3) {
            server.setBlock(pos, anchor.setValue(RespawnAnchorBlock.CHARGE, charges + 1), 3);
            server.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1F, 1F);
            return;
        }

        // The fourth glowstone completes the ritual. This is not a Level.explode call:
        // no explosion damage, fire or terrain destruction can occur.
        server.removeBlock(pos, false);
        for (int[] p : CORNERS) {
            BlockPos corner = pos.offset(p[0], 0, p[1]);
            BlockState old = server.getBlockState(corner);
            server.setBlock(corner, Blocks.SOUL_CAMPFIRE.defaultBlockState()
                    .setValue(CampfireBlock.FACING, old.getValue(CampfireBlock.FACING))
                    .setValue(CampfireBlock.LIT, true), 3);
        }
        server.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D, 4, 0.6D, 0.5D, 0.6D, 0);
        server.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D, 80, 2D, 0.8D, 2D, 0.02D);
        server.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.5F, 0.65F);
        player.sendOverlayMessage(
                Component.literal("The ritual is complete.")
        );
    }

    /** Before activation: regular lit campfires, anchor and lit candles. */
    private static boolean isRitual(Level level, BlockPos anchor, boolean complete) {
        if (!level.getBlockState(anchor.below()).is(Blocks.CRYING_OBSIDIAN)) return false;
        // The original center is unrestricted once activated: a crafting table
        // may replace the anchor. Completion is recognized by soul campfires.
        if (!complete && !level.getBlockState(anchor).is(Blocks.RESPAWN_ANCHOR)) return false;
        for (int[] p : CANDLES) {
            BlockState state = level.getBlockState(anchor.offset(p[0],p[1],p[2]));
            if (!(state.getBlock() instanceof CandleBlock) || !state.getValue(CandleBlock.LIT)) return false;
        }
        for (int[] p : CORNERS) {
            // Original snow support can be any present block, but not missing entirely.
            if (level.getBlockState(anchor.offset(p[0], -1, p[1])).isAir()) return false;
            BlockState state = level.getBlockState(anchor.offset(p[0],0,p[1]));
            if (!state.is(complete ? Blocks.SOUL_CAMPFIRE : Blocks.CAMPFIRE)
                    || !state.getValue(CampfireBlock.LIT)) return false;
        }
        return true;
    }

    /** No global recipe unlock: the player must stand within a completed 7x7 circle. */
    public static boolean canCraftHere(Player player) {
        if (player.level().isClientSide()) return true; // server is authoritative; client may predict
        if (!(player.level() instanceof ServerLevel level)) return false;
        BlockPos feet = player.blockPosition();
        for (int y = -2; y <= 2; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos center = feet.offset(x, y, z);
                    if (isRitual(level, center, true)) return true;
                }
            }
        }
        return false;
    }

    // Regular villagers flee within 12 blocks, without replacing their normal Brain.
    // On each fear update, vanilla navigation is pointed away from the nearest '?' villager.
    @SubscribeEvent
    public static void fear(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Villager villager) || villager instanceof QuestionVillager) return;
        if (villager.level().isClientSide() || villager.tickCount % 10 != 0) return;
        List<QuestionVillager> nearby = villager.level().getEntitiesOfClass(
                QuestionVillager.class, villager.getBoundingBox().inflate(12.0D),
                candidate -> candidate.isAlive());
        if (nearby.isEmpty()) return;
        QuestionVillager nearest = null;
        double distance = Double.MAX_VALUE;
        for (QuestionVillager q : nearby) {
            double d = villager.distanceToSqr(q);
            if (d < distance) { nearest = q; distance = d; }
        }
        if (nearest == null || distance >= 144D) return;
        double dx = villager.getX() - nearest.getX();
        double dz = villager.getZ() - nearest.getZ();
        double norm = Math.sqrt(dx * dx + dz * dz);
        if (norm < 0.01D) { dx = 1; dz = 0; norm = 1; }
        villager.getNavigation().moveTo(villager.getX() + dx / norm * 12D,
                villager.getY(), villager.getZ() + dz / norm * 12D, 1.25D);
    }

    // Register separate attributes for the entity type, keeping all normal villager trades/AI.
    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(RitualRegistry.QUESTION_VILLAGER.get(), Villager.createAttributes().build());
    }
}
