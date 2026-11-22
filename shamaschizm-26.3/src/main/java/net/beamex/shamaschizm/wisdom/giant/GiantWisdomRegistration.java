package net.beamex.shamaschizm.wisdom.giant;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class GiantWisdomRegistration {
    public static EntityType<GiantWisdom> TYPE;
    public static Item EGG;
    @SubscribeEvent public static void register(RegisterEvent event){
        event.register(Registries.ENTITY_TYPE,h->{var id=Shamaschizm.id("giant_tainted_wisdom");
            TYPE=EntityType.Builder.<GiantWisdom>of(GiantWisdom::new,MobCategory.MONSTER).sized(2.7F,2.25F)
                .clientTrackingRange(12).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE,id));h.register(id,TYPE);});
        event.register(Registries.ITEM,h->{var id=Shamaschizm.id("giant_tainted_wisdom_spawn_egg");
            EGG=new SpawnEggItem(new Item.Properties().spawnEgg(TYPE).setId(ResourceKey.create(Registries.ITEM,id))){
                @Override public InteractionResult useOn(UseOnContext context){
                    if(!(context.getLevel() instanceof ServerLevel level))return InteractionResult.SUCCESS;
                    var pos=context.getClickedPos();var face=context.getClickedFace();
                    var player=context.getPlayer();
                    if(player!=null&&!player.mayUseItemAt(pos.relative(face),face,context.getItemInHand()))return InteractionResult.FAIL;
                    GiantWisdom mob=TYPE.create(level,EntitySpawnReason.SPAWN_ITEM_USE);if(mob==null)return InteractionResult.FAIL;
                    Vec3 normal=Vec3.atLowerCornerOf(face.getUnitVec3i());
                    Vec3 root=Vec3.atCenterOf(pos).add(normal.scale(.505));
                    Vec3 body=root.add(normal.scale(2.5));
                    mob.snapTo(body.x,body.y,body.z,player==null?0:player.getYRot()+180,0);
                    if(!level.noCollision(mob)){mob.discard();return InteractionResult.FAIL;}
                    mob.tether(pos,root);
                    if(!level.addFreshEntity(mob))return InteractionResult.FAIL;
                    context.getItemInHand().consume(1,player);
                    return InteractionResult.SUCCESS;
                }
            };h.register(id,EGG);});
    }
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent e){e.put(TYPE,GiantWisdom.giantAttributes().build());}
    @SubscribeEvent public static void tab(BuildCreativeModeTabContentsEvent e){if(e.getTabKey()==CreativeModeTabs.SPAWN_EGGS)e.accept(EGG);}
}
