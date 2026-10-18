package net.beamex.shamaschizm.elf;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class ElfRegistration {
    public static EntityType<ElfEntity> ELF;
    @SubscribeEvent public static void register(RegisterEvent e) {
        e.register(Registries.ENTITY_TYPE, h -> {
            var id=Shamaschizm.id("garden_elf");
            ELF=EntityType.Builder.<ElfEntity>of(ElfEntity::new,MobCategory.MISC)
                    .fireImmune().sized(0.5F,1.4F).clientTrackingRange(10).updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,id));
            h.register(id,ELF);
        });
    }
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent e) {
        e.put(ELF,ElfEntity.attributes().build());
    }
}
