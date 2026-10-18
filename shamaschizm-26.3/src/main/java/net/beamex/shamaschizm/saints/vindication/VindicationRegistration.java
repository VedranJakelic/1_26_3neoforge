package net.beamex.shamaschizm.saints.vindication;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class VindicationRegistration {
 public static EntityType<VindicationEntity> BOSS;
 public static EntityType<SaintBoneEntity> BONE;
 public static EntityType<TemplarProjectile> SPEAR_PROJECTILE;
 public static Item SPEAR;public static Block SOUL_FIRE;
 @SubscribeEvent public static void register(RegisterEvent e){
  e.register(Registries.ENTITY_TYPE,h->{
   var id=Shamaschizm.id("vindication");BOSS=EntityType.Builder.<VindicationEntity>of(VindicationEntity::new,MobCategory.MONSTER)
    .sized(1.0F,1.0F).clientTrackingRange(16).updateInterval(1).fireImmune().noLootTable().build(ResourceKey.create(Registries.ENTITY_TYPE,id));h.register(id,BOSS);
   id=Shamaschizm.id("saint_bone");BONE=EntityType.Builder.<SaintBoneEntity>of(SaintBoneEntity::new,MobCategory.MISC)
    .sized(.3F,.3F).clientTrackingRange(12).updateInterval(1).fireImmune().noLootTable().build(ResourceKey.create(Registries.ENTITY_TYPE,id));h.register(id,BONE);
   id=Shamaschizm.id("templar_spear");SPEAR_PROJECTILE=EntityType.Builder.<TemplarProjectile>of(TemplarProjectile::new,MobCategory.MISC)
    .sized(.5F,.5F).clientTrackingRange(10).updateInterval(1).noLootTable().build(ResourceKey.create(Registries.ENTITY_TYPE,id));h.register(id,SPEAR_PROJECTILE);
  });
  e.register(Registries.ITEM,h->{var id=Shamaschizm.id("templars_sin");
   // Component initializers cannot read another item's components: binding order is not guaranteed.
   // Build the same netherite spear properties directly. TemplarSpearItem.use removes KINETIC_WEAPON
   // before player use, while mounted Saints keep that component for their charge attack.
   SPEAR=new TemplarSpearItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id))
    .spear(ToolMaterial.NETHERITE,1.15F,1.2F,.4F,2.5F,9F,5.5F,5.1F,8.75F,4.6F)
    .fireResistant());
   h.register(id,SPEAR);
  });
  e.register(Registries.BLOCK,h->{var id=Shamaschizm.id("vindication_soul_fire");SOUL_FIRE=new VindicationSoulFire(BlockBehaviour.Properties.ofFullCopy(Blocks.SOUL_FIRE).setId(ResourceKey.create(Registries.BLOCK,id)));h.register(id,SOUL_FIRE);});
 }
 @SubscribeEvent public static void attributes(EntityAttributeCreationEvent e){e.put(BOSS,Mob.createMobAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,33).add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE,1).build());}
 @SubscribeEvent public static void tab(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e){if(e.getTabKey().identifier().toString().equals("minecraft:combat"))e.accept(SPEAR);}
}
