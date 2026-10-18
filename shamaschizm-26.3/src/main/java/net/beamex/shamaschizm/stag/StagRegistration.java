package net.beamex.shamaschizm.stag;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class StagRegistration {
    public static EntityType<StagEntity> STAG;
    public static Item ANTLER,SPAWN_EGG;
    @SubscribeEvent public static void register(RegisterEvent e){
        e.register(Registries.ENTITY_TYPE,h->{var id=Shamaschizm.id("stag");
            STAG=EntityType.Builder.<StagEntity>of(StagEntity::new,MobCategory.CREATURE).sized(0.9F,1.8F)
                .clientTrackingRange(10).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE,id));h.register(id,STAG);});
        e.register(Registries.ITEM,h->{
            var antler=Shamaschizm.id("antler");ANTLER=new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,antler)));h.register(antler,ANTLER);
            var egg=Shamaschizm.id("stag_spawn_egg");SPAWN_EGG=new SpawnEggItem(new Item.Properties().spawnEgg(STAG).setId(ResourceKey.create(Registries.ITEM,egg)));h.register(egg,SPAWN_EGG);
        });
        e.register(Registries.RECIPE_SERIALIZER,h->h.register(Shamaschizm.id("antler_helmet"),AntlerRecipe.SERIALIZER));
    }
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent e){e.put(STAG,StagEntity.createAttributes().build());}
    @SubscribeEvent public static void placement(RegisterSpawnPlacementsEvent e){
        e.register(STAG,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Animal::checkAnimalSpawnRules,RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
    @SubscribeEvent public static void tab(BuildCreativeModeTabContentsEvent e){
        if(e.getTabKey()==CreativeModeTabs.SPAWN_EGGS)e.accept(SPAWN_EGG);
        if(e.getTabKey()==CreativeModeTabs.INGREDIENTS)e.accept(ANTLER);
    }
}
