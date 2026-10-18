package net.beamex.shamaschizm.world.block;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class SchizmStoneRegistration {
    public static final Identifier ID = Shamaschizm.id("schizm_stone");
    public static Block SCHIZM_STONE;

    private SchizmStoneRegistration() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ID);
            SCHIZM_STONE = new SchizmStoneBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).setId(key));
            helper.register(ID, SCHIZM_STONE);
        });
    }
}
