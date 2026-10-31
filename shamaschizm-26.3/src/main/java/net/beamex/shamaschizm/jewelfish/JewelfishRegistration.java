package net.beamex.shamaschizm.jewelfish;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class JewelfishRegistration {
    public static EntityType<Jewelfish> TYPE;
    public static Item EGG,BUCKET;
    @SubscribeEvent public static void register(RegisterEvent e){
        e.register(Registries.ENTITY_TYPE,h->{var id=Shamaschizm.id("jewelfish");TYPE=EntityType.Builder.<Jewelfish>of(Jewelfish::new,MobCategory.WATER_AMBIENT)
                .sized(.65F,.45F).clientTrackingRange(8).updateInterval(1).noLootTable().build(ResourceKey.create(Registries.ENTITY_TYPE,id));h.register(id,TYPE);});
        e.register(Registries.ITEM,h->{
            var egg=Shamaschizm.id("jewelfish_spawn_egg");EGG=new SpawnEggItem(new Item.Properties().spawnEgg(TYPE).setId(ResourceKey.create(Registries.ITEM,egg)));h.register(egg,EGG);
            var bucket=Shamaschizm.id("jewelfish_bucket");BUCKET=new MobBucketItem(TYPE,Fluids.WATER,SoundEvents.BUCKET_EMPTY_FISH,new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM,bucket)));h.register(bucket,BUCKET);
        });
    }
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent e){e.put(TYPE,Jewelfish.attributes().build());}
    @SubscribeEvent public static void tab(BuildCreativeModeTabContentsEvent e){if(e.getTabKey()==CreativeModeTabs.SPAWN_EGGS)e.accept(EGG);if(e.getTabKey().equals(ResourceKey.create(Registries.CREATIVE_MODE_TAB,net.minecraft.resources.Identifier.withDefaultNamespace("tools_and_utilities"))))e.accept(BUCKET);}
}
