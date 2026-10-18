package net.beamex.shamaschizm;

import com.mojang.logging.LogUtils;
import net.beamex.shamaschizm.effect.ModEffects;
import net.beamex.shamaschizm.entity.ModEntities;
import net.beamex.shamaschizm.worldgen.ShamaschizmStructures;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import net.beamex.shamaschizm.sound.ModSounds;

@Mod(Shamaschizm.MOD_ID)
public final class Shamaschizm {
    public static final String MOD_ID = "shamaschizm";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Shamaschizm(IEventBus modBus) {
        ModEntities.ENTITY_TYPES.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ShamaschizmStructures.STRUCTURE_TYPES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
