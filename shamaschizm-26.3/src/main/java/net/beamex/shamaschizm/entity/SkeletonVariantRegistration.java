package net.beamex.shamaschizm.entity;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.SkeletonVariantEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Self-contained registration for the Schizm skeleton variant. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class SkeletonVariantRegistration {
    public static final Identifier ID = Shamaschizm.id("skeleton_variant1");
    public static EntityType<SkeletonVariantEntity> TYPE;

    private SkeletonVariantRegistration() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.ENTITY_TYPE, helper -> {
            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ID);
            TYPE = EntityType.Builder.<SkeletonVariantEntity>of(
                            SkeletonVariantEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F)
                    .ridingOffset(-0.7F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build(key);
            helper.register(ID, TYPE);
        });
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE, AbstractSkeleton.createAttributes().build());
    }
}
