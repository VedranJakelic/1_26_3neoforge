package net.beamex.shamaschizm.ritual;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** A real villager with normal villager AI, a separate entity id and an unusual face. */
public final class QuestionVillager extends Villager {
    private static final Component FLESH_GOLEM_TITLE = Component.literal("Flesh golem");

    public QuestionVillager(EntityType<? extends Villager> type, Level level) {
        super(type, level);
        setCustomName(Component.literal("?"));
        setPersistenceRequired();
    }

    /** Only the trading UI title changes; the entity's nameplate stays unchanged. */
    @Override
    public void openTradingScreen(Player player, Component title, int level) {
        var profession = getVillagerData().profession();
        boolean employed = !profession.is(VillagerProfession.NONE)
                && !profession.is(VillagerProfession.NITWIT);
        super.openTradingScreen(player, employed ? FLESH_GOLEM_TITLE : title, level);
    }
}
