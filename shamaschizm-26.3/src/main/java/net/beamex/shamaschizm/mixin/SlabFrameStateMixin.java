package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.slab.SlabFrameState;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.*;
@Mixin(ItemFrameRenderState.class)
public abstract class SlabFrameStateMixin implements SlabFrameState {
 @Unique private Identifier shamaschizm$slab;
 public Identifier shamaschizm$slabTexture(){return shamaschizm$slab;}
 public void shamaschizm$slabTexture(Identifier texture){shamaschizm$slab=texture;}
}
