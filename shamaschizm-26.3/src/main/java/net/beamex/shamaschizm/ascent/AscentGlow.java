package net.beamex.shamaschizm.ascent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class AscentGlow {
    public static final int LIGHT_LEVEL = 11;
    public static final int UPDATE_TICKS = 1;
    public static final ResourceKey<TrimPattern> PATTERN = ResourceKey.create(
            Registries.TRIM_PATTERN, Identifier.fromNamespaceAndPath("shamaschizm", "ascent"));
    public static final ResourceKey<TrimMaterial> MATERIAL = ResourceKey.create(
            Registries.TRIM_MATERIAL, Identifier.fromNamespaceAndPath("shamaschizm", "glow_ink"));
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private AscentGlow() {}

    public static boolean isGlowing(ItemStack stack) {
        ArmorTrim trim = stack.get(DataComponents.TRIM);
        return trim != null && trim.pattern().is(PATTERN) && trim.material().is(MATERIAL);
    }

    public static boolean isWearingGlow(LivingEntity entity) {
        if (!entity.isAlive() || entity.isSpectator()) return false;
        for (EquipmentSlot slot : ARMOR) {
            if (isGlowing(entity.getItemBySlot(slot))) return true;
        }
        return false;
    }

    public static boolean canHostLight(BlockState state) {
        // Only actual air or plain source water; never replace plants or waterlogged blocks.
        return state.isAir() || state.is(AscentBootstrap.LIGHT)
                || (state.is(Blocks.WATER) && state.getFluidState().isSource());
    }

    /** Find a free cell around the wearer without replacing occupied terrain. */
    public static BlockPos sourcePosition(LivingEntity entity) {
        BlockPos head = BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ());
        BlockPos torso = BlockPos.containing(entity.getX(),
                (entity.getY() + entity.getEyeY()) * 0.5, entity.getZ());
        BlockPos feet = entity.blockPosition();
        // Head first avoids carpet, snow layers, short grass, slabs and stairs.
        // Above-head also handles tall grass when it occupies both body cells.
        for (BlockPos candidate : new BlockPos[]{head, torso, feet, head.above()}) {
            if (candidate.getY() < entity.level().getMinY()
                    || candidate.getY() > entity.level().getMaxY()
                    || !entity.level().getWorldBorder().isWithinBounds(candidate)) continue;
            if (canHostLight(entity.level().getBlockState(candidate))) return candidate;
        }
        return null;
    }
}
