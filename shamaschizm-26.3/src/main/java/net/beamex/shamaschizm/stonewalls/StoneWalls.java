package net.beamex.shamaschizm.stonewalls;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class StoneWalls {
    public static Block WALL1,WALL2,WALL3;
    public static Item ITEM1,ITEM2,ITEM3;
    @SubscribeEvent public static void register(RegisterEvent event){
        event.register(Registries.BLOCK,h->{
            WALL1=new StoneMuralBlock(properties("wall1").pushReaction(PushReaction.IMMOVEABLE));
            WALL2=new Block(properties("wall2"));WALL3=new Block(properties("wall3"));
            h.register(Shamaschizm.id("wall1"),WALL1);h.register(Shamaschizm.id("wall2"),WALL2);h.register(Shamaschizm.id("wall3"),WALL3);
        });
        event.register(Registries.ITEM,h->{
            ITEM1=item("wall1",WALL1);ITEM2=item("wall2",WALL2);ITEM3=item("wall3",WALL3);
            h.register(Shamaschizm.id("wall1"),ITEM1);h.register(Shamaschizm.id("wall2"),ITEM2);h.register(Shamaschizm.id("wall3"),ITEM3);
        });
    }
    private static BlockBehaviour.Properties properties(String name){return BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).setId(ResourceKey.create(Registries.BLOCK,Shamaschizm.id(name)));}
    private static Item item(String name,Block block){return new BlockItem(block,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Shamaschizm.id(name))).useBlockDescriptionPrefix());}
    @SubscribeEvent public static void creative(BuildCreativeModeTabContentsEvent event){
        if(event.getTabKey()==CreativeModeTabs.BUILDING_BLOCKS){event.accept(ITEM1);event.accept(ITEM2);event.accept(ITEM3);}
    }
}
