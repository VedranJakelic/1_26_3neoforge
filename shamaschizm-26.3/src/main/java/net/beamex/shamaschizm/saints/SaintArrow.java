package net.beamex.shamaschizm.saints;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.level.Level;

/** Collides with terrain and players; all other mobs are transparent to this shot. */
public final class SaintArrow extends Arrow {
    public SaintArrow(EntityType<? extends Arrow> type, Level level) { super(type, level); }
    @Override protected boolean canHitEntity(Entity entity) {
        return entity instanceof Player && super.canHitEntity(entity);
    }
    @Override protected void doKnockback(net.minecraft.world.entity.LivingEntity target,
                                         net.minecraft.world.damagesource.DamageSource source) {
        super.doKnockback(target, source);
        var direction = this.getDeltaMovement().multiply(1,0,1).normalize();
        target.push(direction.x * 3.0D, .1D, direction.z * 3.0D);
    }
}
