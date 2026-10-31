package net.beamex.shamaschizm.maiden;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
@EventBusSubscriber(modid="shamaschizm")
public final class MaidenRegistration {
 public static EntityType<IronMaiden> TYPE;public static EntityType<MaidenBone> BONE;public static Item EGG;public static SoundEvent OPEN,CLANG;
 @SubscribeEvent public static void register(RegisterEvent e){
  e.register(Registries.ENTITY_TYPE,h->{var id=Shamaschizm.id("iron_maiden");TYPE=EntityType.Builder.<IronMaiden>of(IronMaiden::new,MobCategory.MONSTER).sized(1.05F,2.25F).clientTrackingRange(12).updateInterval(1).noLootTable().build(ResourceKey.create(Registries.ENTITY_TYPE,id));h.register(id,TYPE);var bone=Shamaschizm.id("iron_maiden_bone");BONE=EntityType.Builder.<MaidenBone>of(MaidenBone::new,MobCategory.MISC).sized(.5F,.5F).clientTrackingRange(12).updateInterval(1).noLootTable().build(ResourceKey.create(Registries.ENTITY_TYPE,bone));h.register(bone,BONE);});
  e.register(Registries.ITEM,h->{var id=Shamaschizm.id("iron_maiden_spawn_egg");EGG=new SpawnEggItem(new Item.Properties().spawnEgg(TYPE).setId(ResourceKey.create(Registries.ITEM,id)));h.register(id,EGG);});
  e.register(Registries.SOUND_EVENT,h->{var a=Shamaschizm.id("iron_maiden_open");OPEN=SoundEvent.createVariableRangeEvent(a);h.register(a,OPEN);var b=Shamaschizm.id("iron_maiden_clang");CLANG=SoundEvent.createVariableRangeEvent(b);h.register(b,CLANG);});
 }
 @SubscribeEvent public static void attributes(EntityAttributeCreationEvent e){e.put(TYPE,IronMaiden.attributes().build());e.put(BONE,MaidenBone.attributes().build());}
 @SubscribeEvent public static void tab(BuildCreativeModeTabContentsEvent e){if(e.getTabKey()==CreativeModeTabs.SPAWN_EGGS)e.accept(EGG);}
}
