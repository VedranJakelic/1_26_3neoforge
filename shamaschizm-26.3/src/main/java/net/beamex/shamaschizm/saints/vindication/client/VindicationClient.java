package net.beamex.shamaschizm.saints.vindication.client;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.saints.vindication.VindicationRegistration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID,value=Dist.CLIENT)
public final class VindicationClient {
 private static final java.util.Map<net.beamex.shamaschizm.saints.vindication.VindicationEntity,Boolean> sounds=new java.util.WeakHashMap<>();
 @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  var mc=net.minecraft.client.Minecraft.getInstance();if(mc.level==null||mc.player==null){sounds.clear();return;}
  java.util.Set<Integer> hidden=new java.util.HashSet<>();
  for(var piece:mc.level.getEntitiesOfClass(net.beamex.shamaschizm.saints.vindication.SaintBoneEntity.class,mc.player.getBoundingBox().inflate(160))){
   if(hidden.add(piece.corpseId()) && mc.level.getEntity(piece.corpseId()) instanceof net.minecraft.world.entity.LivingEntity corpse && !corpse.isAlive()){
    corpse.setInvisible(true);
    // Client-only equipment removal: server loot and experience are unchanged.
    for(var slot:net.minecraft.world.entity.EquipmentSlot.values())corpse.setItemSlot(slot,net.minecraft.world.item.ItemStack.EMPTY);
   }
  }
  sounds.keySet().removeIf(b->b.isRemoved()||b.level()!=mc.level||(!b.isCharging()&&b.beamTicks()==0));
  for(var boss:mc.level.getEntitiesOfClass(net.beamex.shamaschizm.saints.vindication.VindicationEntity.class,mc.player.getBoundingBox().inflate(96)))
   if(boss.isAlive()&&(boss.isCharging()||boss.beamTicks()>0)&&!sounds.containsKey(boss)){sounds.put(boss,true);mc.getSoundManager().play(new VindicationChargeSound(boss));}
 }

 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(VindicationRegistration.BOSS,VindicationRenderer::new);e.registerEntityRenderer(VindicationRegistration.BONE,SaintBoneRenderer::new);e.registerEntityRenderer(VindicationRegistration.SPEAR_PROJECTILE,TemplarSpearRenderer::new);}
 @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e){e.registerLayerDefinition(VindicationModel.LAYER,VindicationModel::layer);}
}
