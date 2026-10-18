package net.beamex.shamaschizm.plants;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class DecorativePlants {
 public static LargeEyeBlossomBlock BLOSSOM,STALK;
 public static OvertreeRootBlock ROOT;
 public static Item BLOSSOM_ITEM,STALK_ITEM,ROOT_ITEM;
 @SubscribeEvent public static void register(RegisterEvent event){
  event.register(Registries.BLOCK,h->{
   BLOSSOM=new LargeEyeBlossomBlock(flower("large_eye_blossom").randomTicks()
    .lightLevel(s->s.getValue(LargeEyeBlossomBlock.STAGE)==3?1:0),false);
   STALK=new LargeEyeBlossomBlock(flower("large_eye_blossom_stalk"),true);
   ROOT=new OvertreeRootBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HANGING_ROOTS)
    .setId(ResourceKey.create(Registries.BLOCK,Shamaschizm.id("overtree_root")))
    .overrideLootTable(java.util.Optional.of(ResourceKey.create(Registries.LOOT_TABLE,Shamaschizm.id("blocks/overtree_root")))));
   h.register(Shamaschizm.id("large_eye_blossom"),BLOSSOM);h.register(Shamaschizm.id("large_eye_blossom_stalk"),STALK);h.register(Shamaschizm.id("overtree_root"),ROOT);
  });
  event.register(Registries.ITEM,h->{
   BLOSSOM_ITEM=item("large_eye_blossom",BLOSSOM);STALK_ITEM=item("large_eye_blossom_stalk",STALK);ROOT_ITEM=item("overtree_root",ROOT);
   h.register(Shamaschizm.id("large_eye_blossom"),BLOSSOM_ITEM);h.register(Shamaschizm.id("large_eye_blossom_stalk"),STALK_ITEM);h.register(Shamaschizm.id("overtree_root"),ROOT_ITEM);
  });
 }
 private static BlockBehaviour.Properties flower(String id){return BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY)
  .setId(ResourceKey.create(Registries.BLOCK,Shamaschizm.id(id))).noLootTable().noCollision().offsetType(BlockBehaviour.OffsetType.XZ);}
 private static Item item(String id,Block b){return new BlockItem(b,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,Shamaschizm.id(id))).useBlockDescriptionPrefix());}
 @SubscribeEvent public static void tab(BuildCreativeModeTabContentsEvent e){
  if(e.getTabKey()==ResourceKey.create(Registries.CREATIVE_MODE_TAB,Identifier.withDefaultNamespace("natural_blocks"))){e.accept(BLOSSOM_ITEM);e.accept(STALK_ITEM);e.accept(ROOT_ITEM);}
 }
}
