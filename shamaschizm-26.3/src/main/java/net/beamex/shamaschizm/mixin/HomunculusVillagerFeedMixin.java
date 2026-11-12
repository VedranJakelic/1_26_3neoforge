package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.ritual.HomunculusRitualEvents;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Credit only food the villager actually picks up. */
@Mixin(Villager.class)
public abstract class HomunculusVillagerFeedMixin {
    @Unique private int shamaschizm$foodBefore;
    @Unique private ServerPlayer shamaschizm$feeder;
    @Inject(method="pickUpItem",at=@At("HEAD"))
    private void shamaschizm$beforeFood(ServerLevel level,ItemEntity item,CallbackInfo ci){
        shamaschizm$feeder=null;shamaschizm$foodBefore=0;
        if(HomunculusRitualEvents.villagerFood(item.getItem()) && item.getOwner() instanceof ServerPlayer player){
            shamaschizm$feeder=player;shamaschizm$foodBefore=item.getItem().getCount();
        }
    }
    @Inject(method="pickUpItem",at=@At("RETURN"))
    private void shamaschizm$afterFood(ServerLevel level,ItemEntity item,CallbackInfo ci){
        ServerPlayer player=shamaschizm$feeder;shamaschizm$feeder=null;
        if(player!=null && (item.isRemoved()||item.getItem().getCount()<shamaschizm$foodBefore))
            HomunculusRitualEvents.fed(player,(Villager)(Object)this);
    }
}
