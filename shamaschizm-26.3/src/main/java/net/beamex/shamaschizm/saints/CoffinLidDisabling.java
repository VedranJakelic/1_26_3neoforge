package net.beamex.shamaschizm.saints;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Pickaxes counter the lid; its normal axe disable scale remains zero. */
public final class CoffinLidDisabling {
    private static final String UNTIL="CoffinLidDisabledUntil";
    private CoffinLidDisabling(){}
    public static boolean isDisabled(LivingEntity defender){
        return defender.getPersistentData().getLongOr(UNTIL,0L)>defender.level().getGameTime();
    }
    public static void onBlockedHit(ServerLevel level,LivingEntity defender,LivingEntity attacker){
        ItemStack lid=defender.getUseItem();
        if(!lid.is(CatacombSaints.LID) || !attacker.getMainHandItem().is(ItemTags.PICKAXES))return;
        defender.getPersistentData().putLong(UNTIL,level.getGameTime()+100);
        if(defender instanceof Player player)player.getCooldowns().addCooldown(lid,100);
        defender.stopUsingItem();
        defender.playSound(net.minecraft.sounds.SoundEvents.SHIELD_BREAK.value(),1.0F,1.0F);
    }
}
