package net.beamex.shamaschizm.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The actual clicked points become opposite corners of one upright rectangle. */
public final class SpanningWebItem extends Item {
    private static final String PREFIX = "ShamaschizmWeb";
    public SpanningWebItem(Properties properties) { super(properties); }
    private static void clear(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            for (String name : new String[]{"Point", "Dimension", "Owner", "X", "Y", "Z", "Corner"}) tag.remove(PREFIX + name);
        });
    }
    private static void say(Player player, String text) { player.sendSystemMessage(Component.literal(text)); }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide()) { clear(player.getItemInHand(hand)); say(player, "Web selection cleared."); }
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.FAIL;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        ItemStack stack = context.getItemInHand();
        if (player.isShiftKeyDown()) { clear(stack); say(player, "Web selection cleared."); return InteractionResult.SUCCESS_SERVER; }
        Vec3 point = context.getClickLocation();
        if (!allowed(level, player, context.getClickedPos(), context)) return InteractionResult.FAIL;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String dimension = level.dimension().identifier().toString();
        if (!tag.getBooleanOr(PREFIX + "Corner", false) || !dimension.equals(tag.getStringOr(PREFIX + "Dimension", ""))
                || !player.getUUID().toString().equals(tag.getStringOr(PREFIX + "Owner", ""))) {
            clear(stack);
            CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
                data.putBoolean(PREFIX + "Corner", true);
                data.putDouble(PREFIX + "X", point.x); data.putDouble(PREFIX + "Y", point.y); data.putDouble(PREFIX + "Z", point.z);
                data.putString(PREFIX + "Dimension", dimension); data.putString(PREFIX + "Owner", player.getUUID().toString());
            });
            say(player, "First web corner set. Click the opposite corner, at a different height. Maximum 5 by 5 blocks; sneak-right-click cancels.");
            return InteractionResult.SUCCESS_SERVER;
        }
        Vec3 first = new Vec3(tag.getDoubleOr(PREFIX + "X", 0), tag.getDoubleOr(PREFIX + "Y", 0), tag.getDoubleOr(PREFIX + "Z", 0));
        double width = Math.hypot(point.x - first.x, point.z - first.z), height = Math.abs(point.y - first.y);
        if (!Double.isFinite(width) || !Double.isFinite(height) || width < .0625 || height < .0625 || width > 5.000001 || height > 5.000001) {
            say(player, "Choose corners with both width and height between 1/16 and 5 blocks. Diagonal width is measured along the web.");
            return InteractionResult.FAIL;
        }
        var rectangle = new WebRectangle(new Vec3((first.x+point.x)/2,Math.min(first.y,point.y),(first.z+point.z)/2),
                width,height,Math.toDegrees(Math.atan2(point.z-first.z,point.x-first.x)));
        var bounds = rectangle.bounds();
        // Check every crossed cell, including cells containing obstacles, without changing them.
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ))) {
            if (rectangle.intersects(new AABB(pos)) && !allowed(level, player, pos, context)) {
                say(player, "Part of this web is outside your building permissions or the loaded world.");
                return InteractionResult.FAIL;
            }
        }
        if (!WebBlocks.install(level,rectangle,first.y>point.y)) {
            say(player,"There is no free space for the web blocks, or part of the span is unavailable.");
            return InteractionResult.FAIL;
        }
        clear(stack); stack.consume(1, player);
        player.awardStat(Stats.ITEM_USED.get(this));
        level.playSound(null, BlockPos.containing(rectangle.bottom()), SoundEvents.COBWEB_PLACE, SoundSource.BLOCKS, 1, 1);
        level.gameEvent(player, GameEvent.BLOCK_PLACE, BlockPos.containing(rectangle.bottom()));
        return InteractionResult.SUCCESS_SERVER;
    }
    private static boolean allowed(ServerLevel level, Player player, BlockPos pos, UseOnContext context) {
        return level.isInWorldBounds(pos) && level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos)
                && level.mayInteract(player, pos) && player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand());
    }
}
