package net.beamex.shamaschizm.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.GuardianRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(GuardianRenderer.class)
public interface GuardianBeamInvoker {
 @Invoker("renderBeam")
 static void shamaschizm$beam(PoseStack poses,SubmitNodeCollector collector,Vec3 direction,float time,float strength,float offset){throw new AssertionError();}
}
