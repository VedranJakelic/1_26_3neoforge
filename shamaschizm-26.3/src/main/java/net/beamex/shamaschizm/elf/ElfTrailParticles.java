package net.beamex.shamaschizm.elf;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class ElfTrailParticles {
    public static final SimpleParticleType TRAIL=new SimpleParticleType(false){};
    @SubscribeEvent public static void register(RegisterEvent event){
        event.register(Registries.PARTICLE_TYPE,h->h.register(Shamaschizm.id("elf_trail"),TRAIL));
    }
}
