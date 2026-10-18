package net.beamex.shamaschizm.serpent;
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
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class Serpents {
 public static EntityType<SeaSerpent> TYPE;
 public static Item EGG;
 public static final SoundEvent GRAB=SoundEvent.createVariableRangeEvent(net.minecraft.resources.Identifier.fromNamespaceAndPath("shamaschizm_serpent","grab"));
 public static final SoundEvent[] IDLE={SoundEvent.createVariableRangeEvent(net.minecraft.resources.Identifier.fromNamespaceAndPath("shamaschizm_serpent","idle_1")),SoundEvent.createVariableRangeEvent(net.minecraft.resources.Identifier.fromNamespaceAndPath("shamaschizm_serpent","idle_2")),SoundEvent.createVariableRangeEvent(net.minecraft.resources.Identifier.fromNamespaceAndPath("shamaschizm_serpent","idle_3"))};
 @SubscribeEvent public static void register(RegisterEvent e){
  e.register(Registries.ENTITY_TYPE,h->{var id=Shamaschizm.id("sea_serpent");TYPE=EntityType.Builder.<SeaSerpent>of(SeaSerpent::new,MobCategory.MONSTER).sized(1.8F,1.5F).clientTrackingRange(12).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE,id));h.register(id,TYPE);});
  e.register(Registries.ITEM,h->{var id=Shamaschizm.id("sea_serpent_spawn_egg");EGG=new SpawnEggItem(new Item.Properties().spawnEgg(TYPE).setId(ResourceKey.create(Registries.ITEM,id)));h.register(id,EGG);});
  e.register(Registries.SOUND_EVENT,h->{h.register(GRAB.location(),GRAB);for(var sound:IDLE)h.register(sound.location(),sound);});
 }
 @SubscribeEvent public static void attributes(EntityAttributeCreationEvent e){e.put(TYPE,SeaSerpent.attributes().build());}
 @SubscribeEvent public static void tab(BuildCreativeModeTabContentsEvent e){if(e.getTabKey()==CreativeModeTabs.SPAWN_EGGS)e.accept(EGG);}
}
