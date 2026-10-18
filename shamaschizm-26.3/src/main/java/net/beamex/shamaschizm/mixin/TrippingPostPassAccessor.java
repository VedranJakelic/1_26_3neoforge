package net.beamex.shamaschizm.mixin;
import java.util.Map;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(PostPass.class)
public interface TrippingPostPassAccessor {
    @Accessor("customUniforms") Map<String, GpuBuffer> shamaschizm$uniforms();
}
