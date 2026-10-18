package net.beamex.shamaschizm.event;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.Schizm;
import net.beamex.shamaschizm.world.ai.SchizmZombieGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class SchizmMobEvents {
    /** Schizm-only local cap; vanilla's comparable hostile cap is much higher. */
    private static final int MAX_NEARBY_HOSTILES = 22;
    private static final double CAP_HORIZONTAL_RADIUS = 64.0;
    private static final double CAP_VERTICAL_RADIUS = 48.0;

    private SchizmMobEvents() {}

    private static boolean forbidden(Entity entity) {
        return entity.getType() == EntityTypes.WITCH || entity.getType() == EntityTypes.ENDERMAN;
    }

    @SubscribeEvent
    public static void checkSpawn(MobSpawnEvent.PositionCheck event) {
        ServerLevel level = event.getLevel().getLevel();
        if (!level.dimension().equals(Schizm.KEY)) return;

        if (forbidden(event.getEntity())) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
            return;
        }

        // Commands, spawn eggs, structures and mob spawners remain under the
        // map maker's control. Only ordinary natural hostile spawning is capped.
        if (event.getSpawnType() == EntitySpawnReason.NATURAL
                && event.getEntity().getType().getCategory() == MobCategory.MONSTER) {
            AABB area = new AABB(
                    event.getX() - CAP_HORIZONTAL_RADIUS,
                    event.getY() - CAP_VERTICAL_RADIUS,
                    event.getZ() - CAP_HORIZONTAL_RADIUS,
                    event.getX() + CAP_HORIZONTAL_RADIUS,
                    event.getY() + CAP_VERTICAL_RADIUS,
                    event.getZ() + CAP_HORIZONTAL_RADIUS);

            int nearby = level.getEntitiesOfClass(Monster.class, area,
                    monster -> monster.isAlive() && !monster.isSpectator()).size();
            if (nearby >= MAX_NEARBY_HOSTILES) {
                event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
            }
        }
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        // Includes eggs, commands, conversions and transfers. Existing saved mobs are retained.
        if (level.dimension().equals(Schizm.KEY) && !event.loadedFromDisk() && forbidden(event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        var type = event.getEntity().getType();
        if ((type == EntityTypes.ZOMBIE || type == EntityTypes.DROWNED || type == EntityTypes.HUSK)
                && event.getEntity() instanceof Zombie zombie) {
            boolean present = zombie.getGoalSelector().getAvailableGoals().stream()
                    .anyMatch(goal -> goal.getGoal() instanceof SchizmZombieGoal);
            // Install in all dimensions, but the goal only runs in Schizm. This also covers transfers.
            if (!present) zombie.getGoalSelector().addGoal(0, new SchizmZombieGoal(zombie));
        }
    }
}
