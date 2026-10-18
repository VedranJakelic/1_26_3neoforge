package net.beamex.shamaschizm.stag;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class AntlerShearing {
    private static void remove(ServerPlayer actor,ItemStack helmet,ItemStack shears,InteractionHand hand){
        Antlers.set(helmet,false);
        ItemStack drop=new ItemStack(StagRegistration.ANTLER);
        if(!actor.getInventory().add(drop))actor.drop(drop,false,net.minecraft.util.Prediction.SERVER_ONLY);
        if(!actor.hasInfiniteMaterials())shears.hurtAndBreak(1,actor,hand);
        actor.level().playSound(null,actor.blockPosition(),SoundEvents.SHEEP_SHEAR,SoundSource.PLAYERS,1,1);
    }
    @SubscribeEvent public static void friend(PlayerInteractEvent.EntityInteract e){
        if(!(e.getTarget() instanceof Player wearer)||wearer==e.getEntity()
            ||!e.getItemStack().is(Items.SHEARS)||!Antlers.has(wearer.getItemBySlot(EquipmentSlot.HEAD)))return;
        if(e.getEntity() instanceof ServerPlayer actor){
            var helmet=wearer.getItemBySlot(EquipmentSlot.HEAD).copy();remove(actor,helmet,e.getItemStack(),e.getHand());
            wearer.setItemSlot(EquipmentSlot.HEAD,helmet);
        }
        e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
    }
    @SubscribeEvent public static void own(PlayerInteractEvent.RightClickItem e){if(shearHeld(e)){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);}}
    @SubscribeEvent public static void block(PlayerInteractEvent.RightClickBlock e){if(shearHeld(e)){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);}}
    private static boolean shearHeld(PlayerInteractEvent e){
        Player p=e.getEntity();if(!p.isShiftKeyDown())return false;
        InteractionHand opposite=e.getHand()==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
        ItemStack used=p.getItemInHand(e.getHand()),other=p.getItemInHand(opposite);
        InteractionHand shearHand;ItemStack helmet,shears;InteractionHand helmetHand;
        if(used.is(Items.SHEARS)&&Antlers.has(other)){shears=used;helmet=other;shearHand=e.getHand();helmetHand=opposite;}
        else if(other.is(Items.SHEARS)&&Antlers.has(used)){shears=other;helmet=used;shearHand=opposite;helmetHand=e.getHand();}
        else return false;
        if(p instanceof ServerPlayer actor){helmet=helmet.copy();remove(actor,helmet,shears,shearHand);p.setItemInHand(helmetHand,helmet);}
        return true;
    }
}
