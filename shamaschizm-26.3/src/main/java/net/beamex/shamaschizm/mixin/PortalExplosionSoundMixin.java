package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.localportal.PortalSounds;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class PortalExplosionSoundMixin {
    // Runs only after a non-cancelled explosion executes. Sound only: never relay blast damage.
    @Inject(method="explode",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/ServerExplosion;explode()I",shift=At.Shift.AFTER))
    private void portal$explosion(Entity entity,DamageSource damage,ExplosionDamageCalculator calculator,
        double x,double y,double z,float radius,boolean fire,Level.ExplosionInteraction interaction,
        ParticleOptions small,ParticleOptions large,WeightedList<ExplosionParticleInfo> particles,
        Holder<SoundEvent> sound,CallbackInfo ci){
        if(entity!=null&&entity.isSilent())return;
        var level=(ServerLevel)(Object)this;
        if(!level.dimension().equals(net.beamex.shamaschizm.world.Schizm.KEY))return;
        var pos=new Vec3(x,y,z);
        long seed=level.getGameTime()^Double.doubleToLongBits(x)^Long.rotateLeft(Double.doubleToLongBits(y),21)^Long.rotateLeft(Double.doubleToLongBits(z),42);
        var random=net.minecraft.util.RandomSource.create(seed);
        float pitch=(1+(random.nextFloat()-random.nextFloat())*.2f)*.7f;
        PortalSounds.relay(level,pos,pos,64,sound.value(),SoundSource.BLOCKS,4,pitch,seed,-1);
    }
}
