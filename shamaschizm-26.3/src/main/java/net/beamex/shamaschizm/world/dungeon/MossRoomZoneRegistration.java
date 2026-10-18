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

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class MossRoomZoneRegistration {
    public static final Identifier ID = Shamaschizm.id("moss_room_zone");
    public static EntityType<MossRoomZoneEntity> MOSS_ROOM_ZONE;

    private MossRoomZoneRegistration() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.ENTITY_TYPE, helper -> {
            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ID);
            MOSS_ROOM_ZONE = EntityType.Builder.<MossRoomZoneEntity>of(
                            MossRoomZoneEntity::new, MobCategory.MISC)
                    .sized(0.01F, 0.01F)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .noLootTable()
                    .build(key);
            helper.register(ID, MOSS_ROOM_ZONE);
        });
    }
}
