package net.beamex.shamaschizm.event;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.CiglunEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/** Turns nearby Cigluns against players who behave violently or destructively. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class CiglunProvocationEvents {
    private CiglunProvocationEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void playerAttackedMob(AttackEntityEvent event) {
        Entity victim = event.getTarget();
        if (!event.isCanceled()
                && victim instanceof Mob
                && !(victim instanceof CiglunEntity)) {
            provokeNearby(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void playerBrokeBlock(BreakBlockEvent event) {
        if (!event.isCanceled()) provokeNearby(event.getPlayer());
    }

    /** Called by the Slot mixin only after an item is actually taken. */
    public static void playerLootedContainer(Player player, Container container) {
        if (isLootContainer(container)) provokeNearby(player);
    }

    private static boolean isLootContainer(Container container) {
        return container instanceof RandomizableContainer
                || container instanceof CompoundContainer
                || container instanceof ContainerEntity;
    }

    private static void provokeNearby(Player player) {
        if (!(player.level() instanceof ServerLevel level)
                || !player.isAlive()
                || player.isSpectator()
                || player.isCreative()) return;

        double range = CiglunEntity.PROVOCATION_RANGE;
        AABB area = player.getBoundingBox().inflate(range);
        for (CiglunEntity ciglun : level.getEntitiesOfClass(
                CiglunEntity.class, area, CiglunEntity::isAlive)) {
            ciglun.provoke(player);
        }
    }
}
