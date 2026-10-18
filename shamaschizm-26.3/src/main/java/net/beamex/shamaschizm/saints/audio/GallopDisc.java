package net.beamex.shamaschizm.saints.audio;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class GallopDisc {
 public static Item DISC;
 @SubscribeEvent public static void register(RegisterEvent e){
  e.register(Registries.SOUND_EVENT,h->{for(String name:new String[]{"gallop_of_the_damned","saints_battle_cue"}){
   var id=Shamaschizm.id(name);h.register(id,SoundEvent.createVariableRangeEvent(id));}});
  e.register(Registries.ITEM,h->{var id=Shamaschizm.id("music_disc_gallop_of_the_damned");
   DISC=new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).stacksTo(1).rarity(Rarity.EPIC)
    .component(net.minecraft.core.component.DataComponents.LORE,new net.minecraft.world.item.component.ItemLore(java.util.List.of(
     net.minecraft.network.chat.Component.translatable("item.shamaschizm.legendary_loot").withStyle(net.minecraft.ChatFormatting.GOLD))))
    .jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG,Shamaschizm.id("gallop_of_the_damned"))));h.register(id,DISC);});
 }
 @SubscribeEvent public static void tab(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e){
  if(e.getTabKey().identifier().toString().equals("minecraft:tools_and_utilities"))e.accept(DISC);
 }
}
