package net.beamex.shamaschizm.relics.client;

import java.util.List;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.relics.RelicRegistration;
import net.minecraft.client.color.block.BlockTintSources;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid=Shamaschizm.MOD_ID,value=Dist.CLIENT)
public final class RelicClient {
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.BlockTintSources event){
        event.register(List.of(BlockTintSources.constant(0xFFFFFFFF)),RelicRegistration.DIAMOND_CLUSTER);
    }
}
