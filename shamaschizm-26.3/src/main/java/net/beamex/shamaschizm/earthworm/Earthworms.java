package net.beamex.shamaschizm.earthworm;
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
public final class Earthworms {
 public static EntityType<EarthwormEntity> TYPE;
 public static Item EGG;
 @SubscribeEvent public static void register(RegisterEvent e){
  e.register(Registries.ENTITY_TYPE,h->{var id=Shamaschizm.id("earthworm");TYPE=EntityType.Builder.<EarthwormEntity>of(EarthwormEntity::new,MobCategory.AMBIENT).sized(.12F,.07F).clientTrackingRange(8).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE,id));h.register(id,TYPE);});
  e.register(Registries.ITEM,h->{var id=Shamaschizm.id("earthworm_spawn_egg");EGG=new SpawnEggItem(new Item.Properties().spawnEgg(TYPE).setId(ResourceKey.create(Registries.ITEM,id)));h.register(id,EGG);});
 }
 @SubscribeEvent public static void attributes(EntityAttributeCreationEvent e){e.put(TYPE,EarthwormEntity.attributes().build());}
 @SubscribeEvent public static void tab(BuildCreativeModeTabContentsEvent e){if(e.getTabKey()==CreativeModeTabs.SPAWN_EGGS)e.accept(EGG);}
}
