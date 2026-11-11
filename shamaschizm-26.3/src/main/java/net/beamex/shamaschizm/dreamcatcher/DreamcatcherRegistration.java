package net.beamex.shamaschizm.dreamcatcher;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class DreamcatcherRegistration {
    public static DreamcatcherBlock BLOCK;
    public static Item ITEM;
    private DreamcatcherRegistration() {}
    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            var id=Shamaschizm.id("dreamcatcher");
            BLOCK=new DreamcatcherBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK,id))
                    .noCollision().noOcclusion().strength(.2F).sound(SoundType.HANGING_ROOTS));
            helper.register(id,BLOCK);
        });
        event.register(Registries.ITEM, helper -> {
            var id=Shamaschizm.id("dreamcatcher");
            ITEM=new BlockItem(BLOCK,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id))
                    .component(DataComponents.ITEM_NAME,Component.translatableWithFallback("block.shamaschizm.dreamcatcher","Dreamcatcher")));
            helper.register(id,ITEM);
        });
    }
    @SubscribeEvent public static void tab(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey().identifier().toString().equals("minecraft:functional_blocks"))event.accept(ITEM);
    }
}
