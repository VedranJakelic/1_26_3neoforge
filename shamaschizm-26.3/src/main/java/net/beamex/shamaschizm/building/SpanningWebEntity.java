package net.beamex.shamaschizm.building;

import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Legacy save compatibility ONLY. New placement never creates this entity. */
public final class SpanningWebEntity extends Entity {
    private static final EntityDataAccessor<Float> WIDTH=SynchedEntityData.defineId(SpanningWebEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT=SynchedEntityData.defineId(SpanningWebEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> FLIPPED=SynchedEntityData.defineId(SpanningWebEntity.class,EntityDataSerializers.BOOLEAN);
    public SpanningWebEntity(EntityType<? extends SpanningWebEntity> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(WIDTH,1F);b.define(HEIGHT,1F);b.define(FLIPPED,false);}
    public float width(){return entityData.get(WIDTH);}public float height(){return entityData.get(HEIGHT);}
    public boolean flipped(){return entityData.get(FLIPPED);}
    public WebRectangle rectangle(){return new WebRectangle(position(),width(),height(),getYRot());}
    @Override public void tick(){
        super.tick();setDeltaMovement(Vec3.ZERO);
        if(level() instanceof ServerLevel server && (tickCount==1||tickCount%40==0)
                && WebBlocks.install(server,rectangle(),flipped()))discard();
    }
    public void mine(ServerPlayer player,boolean down){} // Old unused payload compatibility.
    @Override public boolean isPickable(){return false;}
    @Override public boolean isPushable(){return false;}
    @Override public boolean canBeCollidedWith(Entity other){return false;}
    @Override public boolean canCollideWith(Entity other){return false;}
    @Override public void push(Entity other){}
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){return false;}
    @Override protected void addAdditionalSaveData(ValueOutput o){o.putFloat("WebWidth",width());o.putFloat("WebHeight",height());o.putBoolean("WebFlipped",flipped());}
    @Override protected void readAdditionalSaveData(ValueInput i){
        entityData.set(WIDTH,Math.max(.0625F,Math.min(5,i.getFloatOr("WebWidth",1))));
        entityData.set(HEIGHT,Math.max(.0625F,Math.min(5,i.getFloatOr("WebHeight",1))));
        entityData.set(FLIPPED,i.getBooleanOr("WebFlipped",false));
    }
}
