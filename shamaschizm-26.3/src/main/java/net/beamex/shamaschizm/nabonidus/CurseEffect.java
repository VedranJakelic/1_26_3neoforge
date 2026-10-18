package net.beamex.shamaschizm.nabonidus;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.*;

public final class CurseEffect extends MobEffect {
    public CurseEffect() {
        super(MobEffectCategory.HARMFUL, 0x252020);
        addAttributeModifier(Attributes.MAX_HEALTH, Nabonidus.CURSE_ID, -2.0,
                AttributeModifier.Operation.ADD_VALUE);
    }
    @Override
    public void addAttributeModifiers(AttributeMap attributes, int amplifier) {
        // Amplifier 0 = curse I = no hearts lost. Base implementation uses a+1.
        super.addAttributeModifiers(attributes, amplifier - 1);
    }
    @Override
    public void createModifiers(int amplifier, java.util.function.BiConsumer<net.minecraft.core.Holder<Attribute>, AttributeModifier> consumer) {
        super.createModifiers(amplifier - 1, consumer);
    }
}
