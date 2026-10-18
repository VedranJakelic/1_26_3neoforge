package net.beamex.shamaschizm.saints;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Sweep against a raised lid before projectile movement, so fast shots cannot skip it. */
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class CoffinLidProjectiles {
    private static final String GUARD_UNTIL="SaintsGuardUntil";
    private CoffinLidProjectiles() {}
    public static long guardUntil(LivingEntity defender) {
        return defender.getPersistentData().getLongOr(GUARD_UNTIL,0L);
    }
    public static void holdGuard(LivingEntity defender,long until) {
        defender.getPersistentData().putLong(GUARD_UNTIL,Math.max(guardUntil(defender),until));
    }
    private static boolean saintGuard(LivingEntity defender) {
        return !CoffinLidDisabling.isDisabled(defender) && CatacombSaints.encounterId(defender)!=null
                && defender.getPersistentData().getIntOr(CatacombSaints.ROLE_TAG,-1)==1
                && defender.getOffhandItem().is(CatacombSaints.LID) && !defender.isInvulnerable();
    }
    @SubscribeEvent public static void intercept(EntityTickEvent.Pre event) {
        if(!(event.getEntity() instanceof Projectile projectile)
                || projectile instanceof SaintThrownSpear
                || !(projectile.level() instanceof ServerLevel level) || !projectile.isAlive())return;
        Vec3 from=projectile.position(),motion=projectile.getDeltaMovement();
        if(motion.lengthSqr()<1.0E-6)return;
        Vec3 to=from.add(motion);
        LivingEntity nearest=null;Vec3 impact=null;double nearestT=2;
        for(LivingEntity defender:level.getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(4.5),
                mob->mob.isAlive() && !CoffinLidDisabling.isDisabled(mob) && (saintGuard(mob) || (mob.isBlocking() && mob.getUseItem().is(CatacombSaints.LID))))) {
            if(defender==projectile.getOwner())continue;
            boolean saint=saintGuard(defender);
            if(saint && projectile.getOwner()!=null && CatacombSaints.encounterId(defender).equals(CatacombSaints.encounterId(projectile.getOwner())))continue;
            if(saint) {
                // React during the projectile's own pre-tick, even if the guard's AI
                // has already ticked. The larger volume protects nearby allies too.
                Vec3 centre=defender.position().add(0,.7,0);
                if(motion.dot(centre.subtract(from))<=0)continue;
                AABB guard=new AABB(centre.x-3,centre.y-2.5,centre.z-3,centre.x+3,centre.y+2.5,centre.z+3);
                Vec3 hit=guard.contains(from)?from:guard.clip(from,to).orElse(null);
                if(hit==null)continue;
                double t=Math.sqrt(hit.distanceToSqr(from)/motion.lengthSqr());
                if(t>=nearestT || level.clip(new ClipContext(from,hit,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,projectile)).getType()!=HitResult.Type.MISS)continue;
                nearest=defender;impact=hit;nearestT=t;
                continue;
            }
            Vec3 normal=defender.getLookAngle().normalize();
            double toward=motion.dot(normal);
            if(toward>=-1.0E-6)continue; // incoming from the front only
            Vec3 centre=defender.getEyePosition().add(0,-.4,0).add(normal.scale(.65));
            double distance=from.subtract(centre).dot(normal);
            if(distance<0)continue;
            double t=-distance/toward;
            if(t<0 || t>1 || t>=nearestT)continue;
            Vec3 hit=from.add(motion.scale(t)),offset=hit.subtract(centre);
            Vec3 right=normal.cross(new Vec3(0,1,0));
            if(right.lengthSqr()<1.0E-6)right=new Vec3(1,0,0);
            right=right.normalize();Vec3 up=right.cross(normal).normalize();
            if(Math.abs(offset.dot(right))>1.75 || Math.abs(offset.dot(up))>1.75)continue;
            // A lid cannot catch a shot through an intervening solid block.
            if(level.clip(new ClipContext(from,hit,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,projectile)).getType()!=HitResult.Type.MISS)continue;
            nearest=defender;impact=hit;nearestT=t;
        }
        if(nearest==null)return;
        if(saintGuard(nearest)) {
            Vec3 incoming=motion.scale(-1).normalize();
            float yaw=(float)(Math.atan2(incoming.z,incoming.x)*180/Math.PI)-90;
            nearest.setYRot(yaw);nearest.setYHeadRot(yaw);nearest.yBodyRot=yaw;
            nearest.setXRot((float)(-Math.atan2(incoming.y,incoming.horizontalDistance())*180/Math.PI));
            if(!nearest.isUsingItem())nearest.startUsingItem(InteractionHand.OFF_HAND);
            holdGuard(nearest,level.getGameTime()+20);
        }
        Entity owner=projectile.getOwner();
        projectile.setPos(impact.add(nearest.getLookAngle().scale(.08)));
        projectile.deflect(ProjectileDeflection.REVERSE,nearest,null,false,1.0F);
        projectile.needsSync=true;
        projectile.setOwner(owner); // retain ownership, notably for loyalty tridents
        BlocksAttacks blocks=nearest.getUseItem().get(DataComponents.BLOCKS_ATTACKS);
        if(blocks!=null){
            blocks.onBlocked(level,nearest);
            blocks.hurtBlockingItem(level,nearest.getUseItem(),nearest,nearest.getUsedItemHand(),3.0F);
        }
    }
}
