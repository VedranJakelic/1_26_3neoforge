package net.beamex.shamaschizm.illusionist.curse.client;

import com.google.common.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import net.beamex.shamaschizm.illusionist.curse.IllusionistCurse;
import net.beamex.shamaschizm.illusionist.curse.IllusionistCurseNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import java.util.*;

/** Changes render states, never the real world entities or their interactions. */
@EventBusSubscriber(modid="shamaschizm", value=Dist.CLIENT)
public final class IllusionistCurseClient {
    private static final ContextKey<Integer> SOURCE = new ContextKey<>(id("illusionist_disguise_source"));
    private static final List<net.minecraft.client.particle.Particle> SMOKE_PARTICLES=new ArrayList<>();
    private static final Map<UUID,LivingEntity> PROXIES=new HashMap<>();
    private static ClientLevel cachedLevel;
    private static int remaining;
    private static long seed;
    private static Identifier id(String path){return Identifier.fromNamespaceAndPath("shamaschizm",path);}

    @SubscribeEvent public static void registerModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<EntityRenderer<Entity,EntityRenderState>>() {},
                (entity,state) -> state.setRenderData(SOURCE,entity.getId()));
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {reset();}
    private static void clearSmoke(){for(var particle:SMOKE_PARTICLES)particle.remove();SMOKE_PARTICLES.clear();}
    private static void reset(){clearSmoke();remaining=0;seed=0;PROXIES.clear();cachedLevel=null;IllusionistCurseNetwork.PENDING.clear();}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.getConnection()==null){reset();return;}
        if(mc.level!=cachedLevel){clearSmoke();PROXIES.clear();cachedLevel=mc.level;}
        if(!mc.isPaused() && remaining>0)remaining--;
        IllusionistCurseNetwork.State packet;
        while((packet=IllusionistCurseNetwork.PENDING.poll())!=null){
            if(packet.seed()!=seed){PROXIES.clear();seed=packet.seed();}
            remaining=Math.clamp(packet.remaining(),0,IllusionistCurse.TOTAL_TICKS);
        }
        if(remaining==0 || mc.player==null || !mc.player.isAlive())PROXIES.clear();
        if(remaining<=IllusionistCurse.DISGUISE_TICKS || mc.player==null || !mc.player.isAlive())clearSmoke();
        else if(!mc.isPaused() && mc.level!=null)spawnSmoke(mc);
        if(mc.level!=null && mc.player!=null && mc.player.tickCount%40==0)
            PROXIES.entrySet().removeIf(entry -> mc.level.getEntity(entry.getKey())==null);
    }
    private static boolean active(){var p=Minecraft.getInstance().player;return remaining>0 && remaining<=IllusionistCurse.DISGUISE_TICKS && p!=null && p.isAlive();}
    private static boolean eligible(Entity entity){
        return entity instanceof Player || entity.getType()==EntityTypes.CREEPER
                || entity.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD)
                || entity instanceof net.minecraft.world.entity.monster.skeleton.AbstractSkeleton
                || entity instanceof net.minecraft.world.entity.monster.zombie.Zombie
                || entity instanceof LivingEntity living && living.isInvertedHealAndHarm();
    }
    private static int choice(UUID id,int count){
        long x=id.getMostSignificantBits()^Long.rotateLeft(id.getLeastSignificantBits(),23)^seed;
        x=(x^(x>>>30))*0xbf58476d1ce4e5b9L;x=(x^(x>>>27))*0x94d049bb133111ebL;
        return (int)Math.floorMod(x^(x>>>31),(long)count);
    }
    private static LivingEntity createDisguise(Entity source, Minecraft mc){
        EntityType<?> type;
        if(source instanceof Player){
            type=choice(source.getUUID(),2)==0?BuiltInRegistries.ENTITY_TYPE.getValue(id("illusionist")):EntityTypes.CREEPER;
            if(type==null)type=EntityTypes.CREEPER;
        }else{
            int kind=choice(source.getUUID(),4);
            if(kind==3 && mc.getConnection()!=null){
                List<GameProfile> profiles=mc.getConnection().getOnlinePlayers().stream().map(info->info.getProfile())
                        .filter(profile->!profile.id().equals(mc.player.getUUID()))
                        .sorted(Comparator.comparing(profile->profile.id().toString())).toList();
                if(!profiles.isEmpty())return new RemotePlayer(mc.level,profiles.get(choice(source.getUUID(),profiles.size())));
                kind=0; // Single-player: no other online player's skin to borrow.
            }
            type=switch(kind){case 1->EntityTypes.SHEEP;case 2->EntityTypes.CHICKEN;default->EntityTypes.PIG;};
        }
        Entity proxy=type.create(mc.level,EntitySpawnReason.LOAD);
        return proxy instanceof LivingEntity living?living:null;
    }
    @SubscribeEvent public static void disguise(ExtractLevelRenderStateEvent event){
        if(!active())return;
        Minecraft mc=Minecraft.getInstance();
        var states=event.getRenderState().entityRenderStates;
        float partial=event.getRenderState().worldPartialTicks;
        for(int i=0;i<states.size();i++){
            EntityRenderState original=states.get(i);
            Integer sourceId=original.getRenderData(SOURCE);
            if(sourceId==null)continue;
            Entity source=event.getLevel().getEntity(sourceId);
            if(source==null || source==mc.player || !eligible(source))continue;
            LivingEntity proxy=PROXIES.get(source.getUUID());
            if(proxy==null){
                if(PROXIES.size()>=512)PROXIES.clear();
                proxy=createDisguise(source,mc);if(proxy==null)continue;
                PROXIES.put(source.getUUID(),proxy);
            }
            // Never add these view-only entities to ClientLevel or send them to the server.
            // Render-only proxies never join the world, so Minecraft does not assign
            // their IDs. Renderers (including item/skin extraction) still require one.
            proxy.setId(source.getId());
            proxy.copyPosition(source);proxy.setOldPosAndRot(source.oldPosition(),source.yRotO,source.xRotO);
            proxy.tickCount=source.tickCount;proxy.setDeltaMovement(source.getDeltaMovement());
            proxy.setOnGround(source.onGround());proxy.setInvisible(source.isInvisible());
            if(source instanceof LivingEntity living){
                proxy.yBodyRot=living.yBodyRot;proxy.yBodyRotO=living.yBodyRotO;
                proxy.yHeadRot=living.yHeadRot;proxy.yHeadRotO=living.yHeadRotO;
                proxy.hurtTime=living.hurtTime;proxy.deathTime=living.deathTime;
            }
            EntityRenderState disguised=mc.getEntityRenderDispatcher().extractEntity(proxy,partial);
            disguised.x=original.x;disguised.y=original.y;disguised.z=original.z;
            disguised.lightCoords=original.lightCoords;disguised.ageInTicks=original.ageInTicks;
            disguised.isInvisible=original.isInvisible;disguised.outlineColor=original.outlineColor;
            disguised.nameTag=null;disguised.scoreText=null;disguised.nameTagAttachment=null;
            disguised.passengerOffset=original.passengerOffset;
            if(original instanceof LivingEntityRenderState a && disguised instanceof LivingEntityRenderState b){
                b.walkAnimationPos=a.walkAnimationPos;b.walkAnimationSpeed=a.walkAnimationSpeed;
                b.bodyRot=a.bodyRot;b.yRot=a.yRot;b.xRot=a.xRot;
                b.hasRedOverlay=a.hasRedOverlay;b.deathTime=a.deathTime;b.isInvisibleToPlayer=a.isInvisibleToPlayer;
            }
            states.set(i,disguised);
        }
    }
    private static void spawnSmoke(Minecraft mc){
        SMOKE_PARTICLES.removeIf(particle -> !particle.isAlive());
        var camera=mc.gameRenderer.mainCamera();
        if(!camera.isInitialized())return;
        var center=camera.position();var forward=camera.forwardVector();
        var left=camera.leftVector();var up=camera.upVector();
        var random=mc.level.getRandom();
        float arrival=Math.clamp((IllusionistCurse.TOTAL_TICKS-remaining)/18F,0,1);
        int count=Math.round(20*arrival);
        // Actual short-lived world particles, concentrated just beyond the camera's
        // near plane. No GUI drawing, screen tint, or fullscreen texture is used.
        for(int i=0;i<count && SMOKE_PARTICLES.size()<520;i++){
            double depth=.32+random.nextDouble()*.72;
            double sideways=(random.nextDouble()-.5)*depth*2.1;
            double vertical=(random.nextDouble()-.5)*depth*1.6;
            double x=center.x+forward.x()*depth+left.x()*sideways+up.x()*vertical;
            double y=center.y+forward.y()*depth+left.y()*sideways+up.y()*vertical;
            double z=center.z+forward.z()*depth+left.z()*sideways+up.z()*vertical;
            var motion=mc.player.getDeltaMovement();
            var particle=mc.particleEngine.createParticle(net.minecraft.core.particles.ParticleTypes.WHITE_SMOKE,
                    x,y,z,motion.x*.65+(random.nextDouble()-.5)*.025,
                    motion.y*.65+.008,motion.z*.65+(random.nextDouble()-.5)*.025);
            if(particle!=null){particle.scale(2.8F+random.nextFloat()*1.2F);particle.setLifetime(18+random.nextInt(9));SMOKE_PARTICLES.add(particle);}
        }
    }
}
