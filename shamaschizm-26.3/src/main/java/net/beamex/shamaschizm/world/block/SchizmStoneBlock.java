package net.beamex.shamaschizm.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;

/** Stone-identical terrain with no ticking or stored infestation state. */
public final class SchizmStoneBlock extends Block {
    private static final float SILVERFISH_CHANCE = 0.35F;

    public SchizmStoneBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos,
                                   ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (!level.getGameRules().get(GameRules.BLOCK_DROPS)
                || EnchantmentHelper.hasTag(tool, EnchantmentTags.PREVENTS_INFESTED_SPAWNS)
                || level.getRandom().nextFloat() >= SILVERFISH_CHANCE) return;

        Silverfish silverfish = EntityTypes.SILVERFISH.create(level, EntitySpawnReason.TRIGGERED);
        if (silverfish != null) {
            silverfish.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
            level.addFreshEntity(silverfish);
            silverfish.spawnAnim();
        }
    }
}
