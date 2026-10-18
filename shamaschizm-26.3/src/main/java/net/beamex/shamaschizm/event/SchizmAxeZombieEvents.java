package net.beamex.shamaschizm.event;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.Schizm;
import net.beamex.shamaschizm.world.ai.SchizmAxeShieldGoal;
import net.beamex.shamaschizm.world.dungeon.GardenZoneEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/** Natural axe loadouts and shield-raising reactions; server side only. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class SchizmAxeZombieEvents {
    public static final float AXE_SPAWN_CHANCE = 0.15F;
    private static final String PENDING_AXE = "ShamaschizmPendingAxe";

    private SchizmAxeZombieEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void markNaturalSpawn(FinalizeSpawnEvent event) {
        ServerLevel level = event.getLevel().getLevel();
        if (event.isSpawnCancelled() || !level.dimension().equals(Schizm.KEY)
                || event.getSpawnType() != EntitySpawnReason.NATURAL
                || event.getEntity().getType() != EntityTypes.ZOMBIE
                || !(event.getEntity() instanceof Zombie zombie)
                || GardenZoneEntity.contains(level, zombie.blockPosition())
                || zombie.getPersistentData().contains("ShamaschizmAncientLoadout")) return;
        if (zombie.getRandom().nextFloat() < AXE_SPAWN_CHANCE) {
            zombie.getPersistentData().putBoolean(PENDING_AXE, true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void join(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(Schizm.KEY)
                || !(event.getEntity() instanceof Zombie zombie)) return;
        // Runs after the existing depth-equipment handler. This transient flag
        // is set only at natural spawn, so chunk reloads never reroll equipment.
        if (zombie.getPersistentData().getBooleanOr(PENDING_AXE, false)) {
            zombie.getPersistentData().remove(PENDING_AXE);
            if (zombie.getPersistentData().getIntOr(SchizmSpawnEvents.SHIELD_FRONTLINER_TAG, 0) != 1
                    && !zombie.isPassenger()) {
                zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            }
        }
        // Install even before an axe is equipped, allowing later item pickup.
        // Existing vanilla melee goals retain movement, reach and hit timing.
        if (goal(zombie) == null) {
            zombie.getGoalSelector().addGoal(1, new SchizmAxeShieldGoal(zombie));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void shieldRaised(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)
                || !(player.level() instanceof ServerLevel level)
                || !level.dimension().equals(Schizm.KEY)
                || player.isCreative() || player.isSpectator()
                || event.getDuration() <= 0
                || !event.getItem().has(DataComponents.BLOCKS_ATTACKS)
                || player.getCooldowns().isOnCooldown(event.getItem())) return;
        for (Zombie zombie : level.getEntitiesOfClass(Zombie.class,
                player.getBoundingBox().inflate(SchizmAxeShieldGoal.TARGET_RADIUS),
                SchizmAxeShieldGoal::isAxeZombie)) {
            SchizmAxeShieldGoal goal = goal(zombie);
            if (goal != null) goal.rush(player);
        }
    }

    private static SchizmAxeShieldGoal goal(Zombie zombie) {
        for (var wrapped : zombie.getGoalSelector().getAvailableGoals()) {
            if (wrapped.getGoal() instanceof SchizmAxeShieldGoal goal) return goal;
        }
        return null;
    }
}
