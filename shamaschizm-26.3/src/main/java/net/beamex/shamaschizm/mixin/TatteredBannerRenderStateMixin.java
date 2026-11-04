package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.banner.client.TatteredBannerState;
import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BannerRenderState.class)
public abstract class TatteredBannerRenderStateMixin implements TatteredBannerState {
    @Unique private boolean shamaschizm$tattered;

    @Override public void shamaschizm$setTattered(boolean value) {
        shamaschizm$tattered = value;
    }

    @Override public boolean shamaschizm$isTattered() {
        return shamaschizm$tattered;
    }
}
