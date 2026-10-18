package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.stag.client.AntlerState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(LivingEntityRenderState.class)
public class AntlerRenderStateMixin implements AntlerState {
 @Unique private boolean shamaschizm$antlers;
 public boolean shamaschizm$hasAntlers(){return shamaschizm$antlers;}
 public void shamaschizm$setAntlers(boolean value){shamaschizm$antlers=value;}
}
