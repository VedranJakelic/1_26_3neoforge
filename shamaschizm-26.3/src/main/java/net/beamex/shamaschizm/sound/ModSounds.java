// src/main/java/net/beamex/shamaschizm/sound/ModSounds.java
package net.beamex.shamaschizm.sound;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Shamaschizm.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SCHIZM_01 = register("schizm_01");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCHIZM_02 = register("schizm_02");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCHIZM_03 = register("schizm_03");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCHIZM_04 = register("schizm_04");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIJANSA3 = register("ambijansa3");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHAMAN_1 = register("shaman1");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHAMAN_2 = register("shaman2");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHAMAN_HURT_1 = register("shamanhurt1");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHAMAN_HURT_2 = register("shamanhurt2");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHAMAN_HURT_3 = register("shamanhurt3");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHAMAN_DEATH = register("shamandeath");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHAMAN_CHANT_1 = register("shamanchant1");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHAMAN_CHANT_2 = register("shamanchant2");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROACH_CRAWL = register("roach_crawl");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String path) {
        Identifier id = Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, path);
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, path);
    }

    private ModSounds() {}
}
