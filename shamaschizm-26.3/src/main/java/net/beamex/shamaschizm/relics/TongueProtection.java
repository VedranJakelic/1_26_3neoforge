package net.beamex.shamaschizm.relics;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Persistent per-life protection. No client camera or portal state is involved. */
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class TongueProtection {
    private static final String TAG="shamaschizm_uncorrupted_tongue";
    private TongueProtection() {}
    public static boolean active(Entity entity){return entity.entityTags().contains(TAG);}
    public static void grant(ServerPlayer player){
        player.addTag(TAG);
        player.removeEffect(MobEffects.WITHER);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void effect(MobEffectEvent.Applicable event){
        if(event.getEntity() instanceof ServerPlayer player && active(player)
                && event.getEffectInstance().getEffect().equals(MobEffects.WITHER))
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }
    private static void release(ServerPlayer player){
        if(!active(player))return;
        // Direct drop at the real body, independent of keepInventory and corpse loot capture.
        ItemEntity drop=new ItemEntity(player.level(),player.getX(),player.getY()+.25,player.getZ(),
                new ItemStack(RelicRegistration.TONGUE));
        drop.setDefaultPickUpDelay();
        if(player.level().addFreshEntity(drop))player.removeTag(TAG);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(event.getEntity() instanceof ServerPlayer player && !player.isAlive())release(player);
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        if(event.isWasDeath()){
            if(event.getOriginal() instanceof ServerPlayer original)release(original);
            player.removeTag(TAG);
        }else if(active(event.getOriginal()))player.addTag(TAG);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        if(event.getEntity() instanceof ServerPlayer player && !player.isAlive())release(player);
    }
}
