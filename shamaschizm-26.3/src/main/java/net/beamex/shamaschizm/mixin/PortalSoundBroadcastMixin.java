package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.localportal.PortalSounds;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PortalSoundBroadcastMixin {
    @Inject(method="broadcast(Lnet/minecraft/world/entity/player/Player;DDDDLnet/minecraft/resources/ResourceKey;Lnet/minecraft/network/protocol/Packet;)V",at=@At("TAIL"))
    private void portal$sound(Player except,double x,double y,double z,double radius,ResourceKey<Level> dimension,Packet<?> packet,CallbackInfo ci){
        PortalSounds.broadcast((PlayerList)(Object)this,dimension,new Vec3(x,y,z),radius,packet);
    }
}
