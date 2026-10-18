package net.beamex.shamaschizm.serpent;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
public final class SerpentPart extends PartEntity<SeaSerpent> {
 public final int index;
 private final EntityDimensions size;
 public SerpentPart(SeaSerpent parent,int index,float width,float height){super(parent);this.index=index;size=EntityDimensions.scalable(width,height);refreshDimensions();}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){}
 @Override protected void addAdditionalSaveData(ValueOutput out){}
 @Override protected void readAdditionalSaveData(ValueInput in){}
 @Override public EntityDimensions getDimensions(Pose pose){return size==null?EntityDimensions.scalable(.1F,.1F):size;}
 @Override public boolean canBeHitByProjectile(){return index<SeaSerpent.NODES&&!getParent().corpse();}
 @Override public boolean isPickable(){return index<SeaSerpent.NODES||!getParent().weapon(index-SeaSerpent.NODES).isEmpty();}
 @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){return index<SeaSerpent.NODES&&getParent().hurtServer(level,source,amount);}
 @Override public InteractionResult interact(Player player,InteractionHand hand,Vec3 hit){return index>=SeaSerpent.NODES&&getParent().takeWeapon(player,index-SeaSerpent.NODES)?InteractionResult.SUCCESS:InteractionResult.PASS;}
 @Override public boolean is(Entity other){return other==this||other==getParent();}
 @Override public boolean isPushable(){return false;}
 @Override public boolean canBeCollidedWith(Entity other){return false;}
}
