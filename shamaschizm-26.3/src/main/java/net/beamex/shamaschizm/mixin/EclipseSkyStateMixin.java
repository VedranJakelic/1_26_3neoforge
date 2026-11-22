package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.eclipse.EclipseSkyData;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(SkyRenderState.class)
public abstract class EclipseSkyStateMixin implements EclipseSkyData {
    @Unique private View shamaschizm$eclipseView=View.NONE;
    public View shamaschizm$eclipse() { return shamaschizm$eclipseView; }
    public void shamaschizm$eclipse(View view) { shamaschizm$eclipseView=view; }
}
