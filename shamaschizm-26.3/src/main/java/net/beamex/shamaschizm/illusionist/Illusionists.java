package net.beamex.shamaschizm.illusionist;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class Illusionists {
 public static EntityType<Illusionist> TYPE, LEGACY_TYPE;
 public static Item EGG;
 public static final net.minecraft.sounds.SoundEvent GREETING=net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.Identifier.fromNamespaceAndPath("shamaschizm_illusionist","greeting"));
 public static final net.minecraft.sounds.SoundEvent CHOICE=net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.Identifier.fromNamespaceAndPath("shamaschizm_illusionist","choice"));
 @SubscribeEvent public static void register(RegisterEvent e){
  e.register(Registries.SOUND_EVENT,h->{h.register(GREETING.location(),GREETING);h.register(CHOICE.location(),CHOICE);});
  e.register(Registries.ENTITY_TYPE,h->{var id=Shamaschizm.id("illusionist");TYPE=EntityType.Builder.<Illusionist>of(Illusionist::new,MobCategory.MISC).sized(1.8F,2.5F).clientTrackingRange(10).updateInterval(3).build(ResourceKey.create(Registries.ENTITY_TYPE,id));h.register(id,TYPE);var old=Shamaschizm.id("dark_eyes_illusionist");LEGACY_TYPE=EntityType.Builder.<Illusionist>of(Illusionist::new,MobCategory.MISC).sized(1.8F,2.5F).clientTrackingRange(10).updateInterval(3).build(ResourceKey.create(Registries.ENTITY_TYPE,old));h.register(old,LEGACY_TYPE);});
  e.register(Registries.ITEM,h->{var id=Shamaschizm.id("illusionist_spawn_egg");EGG=new SpawnEggItem(new Item.Properties().spawnEgg(TYPE).setId(ResourceKey.create(Registries.ITEM,id)));h.register(id,EGG);var old=Shamaschizm.id("dark_eyes_illusionist_spawn_egg");h.register(old,new SpawnEggItem(new Item.Properties().spawnEgg(TYPE).setId(ResourceKey.create(Registries.ITEM,old))));});
 }
 @SubscribeEvent public static void attributes(EntityAttributeCreationEvent e){e.put(TYPE,Illusionist.attributes().build());e.put(LEGACY_TYPE,Illusionist.attributes().build());}
 @SubscribeEvent public static void tab(BuildCreativeModeTabContentsEvent e){if(e.getTabKey()==CreativeModeTabs.SPAWN_EGGS)e.accept(EGG);}
}
