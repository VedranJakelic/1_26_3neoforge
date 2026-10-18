package net.beamex.shamaschizm.effect;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;


public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, Shamaschizm.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> TRIPPING = EFFECTS.register(
            "tripping",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0xAA33FF) {}
    );

    private ModEffects() {}
}
