package net.beamex.shamaschizm.saints;

import java.util.UUID;
import java.util.List;
import java.util.Optional;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.CoffinEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.ShieldItem;

import net.minecraft.world.item.component.BlocksAttacks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class CatacombSaints {
    public static final String ENCOUNTER_TAG = "CatacombSaintsEncounter";
    public static final String ROLE_TAG = "CatacombSaintsRole";
    public static EntityType<CatacombEncounterEntity> ENCOUNTER;
    public static EntityType<CoffinEntity> COFFIN2;
    public static EntityType<SaintArrow> ARROW;
    public static EntityType<SaintThrownSpear> THROWN_SPEAR;
    public static Item EGG, LID;
    private static final net.minecraft.resources.Identifier SLOW = Shamaschizm.id("coffin_lid_weight");
    private CatacombSaints() {}

    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.ENTITY_TYPE, helper -> {
            var id = Shamaschizm.id("catacomb_saints");
            ENCOUNTER = EntityType.Builder.<CatacombEncounterEntity>of(CatacombEncounterEntity::new, MobCategory.MISC)
                    .sized(.1F,.1F).clientTrackingRange(16).updateInterval(20).noLootTable()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,id));
            helper.register(id,ENCOUNTER);
            id = Shamaschizm.id("coffin2");
            COFFIN2 = EntityType.Builder.<CoffinEntity>of(CoffinEntity::new,MobCategory.MISC)
                    .sized(.85F,.6F).clientTrackingRange(16).updateInterval(1).noLootTable()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,id));
            helper.register(id,COFFIN2);
            id = Shamaschizm.id("saint_arrow");
            ARROW = EntityType.Builder.<SaintArrow>of(SaintArrow::new,MobCategory.MISC)
                    .sized(.5F,.5F).clientTrackingRange(8).updateInterval(1).noLootTable()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,id));
            helper.register(id,ARROW);
            id=Shamaschizm.id("saint_thrown_spear");
            THROWN_SPEAR=EntityType.Builder.<SaintThrownSpear>of(SaintThrownSpear::new,MobCategory.MISC)
                    .sized(.5F,.5F).clientTrackingRange(8).updateInterval(1).noLootTable()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,id));
            helper.register(id,THROWN_SPEAR);
        });
        event.register(Registries.ITEM, helper -> {
            var id=Shamaschizm.id("catacomb_saints_spawn_egg");
            EGG=new CatacombEggItem(new Item.Properties().spawnEgg(ENCOUNTER).setId(ResourceKey.create(Registries.ITEM,id)));
            helper.register(id,EGG);
            id=Shamaschizm.id("catacomb_coffin_lid");
            // Like vanilla 26.2 shields, resolve registry-dependent components lazily.
            // Reading Items.SHIELD.components() in RegisterEvent crashes before binding.
            LID=new ShieldItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).durability(768)
                    .delayedComponent(DataComponents.BLOCKS_ATTACKS, context -> new BlocksAttacks(
                            0.25F, 0.0F,
                            List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
                            new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
                            Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
                            Optional.of(SoundEvents.SHIELD_BLOCK), Optional.of(SoundEvents.SHIELD_BREAK)))
                    .component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK));
            helper.register(id,LID);
        });
    }
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) {
        event.put(COFFIN2,CoffinEntity.createAttributes().build());
    }
    @SubscribeEvent public static void tab(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey().identifier().toString().equals("minecraft:spawn_eggs")) event.accept(EGG);
        if(event.getTabKey().identifier().toString().equals("minecraft:combat")) event.accept(LID);
    }
    public static UUID encounterId(Entity entity) {
        try { return UUID.fromString(entity.getPersistentData().getStringOr(ENCOUNTER_TAG,"")); }
        catch(IllegalArgumentException e){return null;}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void died(LivingDeathEvent event) {
        if(!(event.getEntity().level() instanceof ServerLevel level)) return;
        UUID id=encounterId(event.getEntity());
        if(id!=null) {
            net.beamex.shamaschizm.saints.vindication.SaintRemains.scatter(level,event.getEntity(),id,event.getEntity().getPersistentData().getIntOr(ROLE_TAG,-1));
            SaintsDeathData.get(level).record(id,event.getEntity().getPersistentData().getIntOr(ROLE_TAG,-1));
            if(level.getEntity(id) instanceof CatacombEncounterEntity encounter)
                encounter.recordDeath(event.getEntity().getUUID());
        }
    }
    @SubscribeEvent public static void maceFall(net.neoforged.neoforge.event.entity.living.LivingFallEvent event) {
        if(encounterId(event.getEntity())!=null && event.getEntity().getPersistentData().getBooleanOr("SaintsMaceAirborne",false))event.setCanceled(true);
    }
    @SubscribeEvent public static void lidWeight(PlayerTickEvent.Post event) {
        var player=event.getEntity();
        if(player.level().isClientSide())return;
        var speed=player.getAttribute(Attributes.MOVEMENT_SPEED);
        if(speed==null)return;
        boolean held=player.getMainHandItem().is(LID)||player.getOffhandItem().is(LID);
        if(held && !speed.hasModifier(SLOW))
            speed.addTransientModifier(new AttributeModifier(SLOW,-.05D,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        else if(!held && speed.hasModifier(SLOW)) speed.removeModifier(SLOW);
    }
}
