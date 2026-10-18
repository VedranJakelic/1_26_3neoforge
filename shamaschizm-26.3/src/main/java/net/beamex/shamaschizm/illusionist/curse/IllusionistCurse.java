package net.beamex.shamaschizm.illusionist.curse;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="shamaschizm")
public final class IllusionistCurse {
    public static final int SMOKE_TICKS=240, DISGUISE_TICKS=1200, TOTAL_TICKS=SMOKE_TICKS+DISGUISE_TICKS;
    public static final int DAMAGE_INTERVAL=40;
    public static final float DROWNING_DAMAGE=2F;
    private static final String TIME="IllusionistCurseTicks", SEED="IllusionistCurseSeed";
    private static final String X="IllusionistCurseX",Y="IllusionistCurseY",Z="IllusionistCurseZ";
    private static final java.util.Map<ServerPlayer,Boolean> SYNCED=new java.util.WeakHashMap<>();
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST) public static void killed(LivingDeathEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) {
            player.getPersistentData().remove(TIME);
            send(player,0);SYNCED.remove(player);return;
        }
        var id=BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        if(!id.getNamespace().equals("shamaschizm") ||
                !(id.getPath().equals("illusionist") || id.getPath().equals("dark_eyes_illusionist")))return;
        var attacker=event.getSource().getEntity();
        if(!(attacker instanceof ServerPlayer))attacker=event.getEntity().getKillCredit();
        if(!(attacker instanceof ServerPlayer player) || !player.isAlive())return;
        var data=player.getPersistentData();
        data.putInt(TIME,TOTAL_TICKS);data.putLong(SEED,player.getRandom().nextLong());
        var origin=event.getEntity().position().add(0,1.2,0);
        data.putDouble(X,origin.x);data.putDouble(Y,origin.y);data.putDouble(Z,origin.z);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,TOTAL_TICKS,0,false,false,true));
        player.level().sendParticles(ParticleTypes.WHITE_SMOKE,origin.x,origin.y,origin.z,70,.6,.8,.6,.035);
        send(player,TOTAL_TICKS);SYNCED.put(player,true);
    }
    private static void send(ServerPlayer player,int ticks) {
        PacketDistributor.sendToPlayer(player,new IllusionistCurseNetwork.State(ticks,
                player.getPersistentData().getLong(SEED).orElse(0L)));
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        var data=player.getPersistentData();int remaining=data.getInt(TIME).orElse(0);
        if(remaining<=0)return;
        if(!player.isAlive()){data.remove(TIME);send(player,0);return;}
        if(SYNCED.put(player,true)==null || remaining%20==0)send(player,remaining);
        // Restore after milk/reconnect without shortening any unrelated, longer blindness.
        var blindness=player.getEffect(MobEffects.BLINDNESS);
        if(blindness==null || blindness.getDuration()<remaining)
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,remaining,0,false,false,true));
        if(remaining>DISGUISE_TICKS) {
            int elapsed=TOTAL_TICKS-remaining;
            Vec3 head=player.getEyePosition();
            Vec3 origin=new Vec3(data.getDouble(X).orElse(head.x),data.getDouble(Y).orElse(head.y),data.getDouble(Z).orElse(head.z));
            double t=Math.min(1,elapsed/18.0);t=t*t*(3-2*t);
            Vec3 smoke=origin.lerp(head,t);
            if(elapsed%2==0) {
                player.level().sendParticles(ParticleTypes.WHITE_SMOKE,smoke.x,smoke.y,smoke.z,8,.35,.3,.35,.012);
                player.level().sendParticles(ParticleTypes.BUBBLE,head.x,head.y-.1,head.z,3,.15,.15,.15,.02);
            }
            // Six one-heart drowning hits over 12 seconds; enough to hurt without
            // making the following hallucination unreachable from full health.
            if((elapsed+1)%DAMAGE_INTERVAL==0)
                player.hurtServer(player.level(),player.damageSources().drown(),DROWNING_DAMAGE);
        }
        if(!player.isAlive())return;
        remaining--;
        if(remaining==0){data.remove(TIME);send(player,0);SYNCED.remove(player);}
        else {data.putInt(TIME,remaining);if(remaining==DISGUISE_TICKS)send(player,remaining);}
    }
}
