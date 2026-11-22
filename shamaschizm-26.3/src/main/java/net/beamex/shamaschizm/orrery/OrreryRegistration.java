package net.beamex.shamaschizm.orrery;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class OrreryRegistration {
    public static OrreryBlock BLOCK;
    public static Item ITEM;
    public static BlockEntityType<OrreryBlockEntity> TYPE;
    public static SoundEvent SQUEAK;
    @SubscribeEvent public static void register(RegisterEvent e){
        var id=Shamaschizm.id("orrery");
        e.register(Registries.BLOCK,h->{BLOCK=new OrreryBlock(BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.COPPER).noOcclusion().dynamicShape().setId(ResourceKey.create(Registries.BLOCK,id)));h.register(id,BLOCK);});
        e.register(Registries.ITEM,h->{ITEM=new BlockItem(BLOCK,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).useBlockDescriptionPrefix());h.register(id,ITEM);});
        e.register(Registries.BLOCK_ENTITY_TYPE,h->{TYPE=new BlockEntityType<>(OrreryBlockEntity::new,java.util.Set.of(BLOCK));h.register(id,TYPE);});
        e.register(Registries.SOUND_EVENT,h->{var sound=net.minecraft.resources.Identifier.fromNamespaceAndPath("shamaschizm_orrery","squeak");SQUEAK=SoundEvent.createVariableRangeEvent(sound);h.register(sound,SQUEAK);});
    }
    @SubscribeEvent public static void tab(BuildCreativeModeTabContentsEvent e){if(e.getTabKey()==CreativeModeTabs.REDSTONE_BLOCKS)e.accept(ITEM);}
}
