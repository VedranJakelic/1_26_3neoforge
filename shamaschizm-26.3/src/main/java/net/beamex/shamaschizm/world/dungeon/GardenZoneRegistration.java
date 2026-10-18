package net.beamex.shamaschizm.world.dungeon;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Registry entry for the server-saved, client-tracked garden audio zone. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class GardenZoneRegistration {
    public static final Identifier ID = Shamaschizm.id("garden_zone");
    public static EntityType<GardenZoneEntity> GARDEN_ZONE;

    private GardenZoneRegistration() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.ENTITY_TYPE, helper -> {
            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ID);
            GARDEN_ZONE = EntityType.Builder.<GardenZoneEntity>of(GardenZoneEntity::new, MobCategory.MISC)
                    .sized(0.01F, 0.01F)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build(key);
            helper.register(ID, GARDEN_ZONE);
        });
    }
}
