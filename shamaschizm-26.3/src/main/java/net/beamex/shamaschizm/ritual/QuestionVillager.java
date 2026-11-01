package net.beamex.shamaschizm.ritual;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;

/** A real villager with normal villager AI, a separate entity id and an unusual face. */
public final class QuestionVillager extends Villager {
    public QuestionVillager(EntityType<? extends Villager> type, Level level) {
        super(type, level);
        setCustomName(Component.literal("?"));
        setPersistenceRequired();
    }
}
