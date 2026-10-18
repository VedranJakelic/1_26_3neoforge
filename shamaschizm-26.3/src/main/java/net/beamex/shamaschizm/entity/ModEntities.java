package net.beamex.shamaschizm.entity;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.ShamanEntity;
import net.beamex.shamaschizm.entity.custom.RoachEntity;
import net.beamex.shamaschizm.entity.custom.BabyRoachEntity;
import net.beamex.shamaschizm.entity.custom.GiantCentipedeEntity;
import net.beamex.shamaschizm.entity.custom.RegularCentipedeEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;


public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Shamaschizm.MOD_ID);


    private static final ResourceKey<EntityType<?>> SHAMAN_KEY =
            ResourceKey.create(
                    Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "shaman")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<ShamanEntity>> SHAMAN =
            ENTITY_TYPES.register("shaman", () ->
                    EntityType.Builder.<ShamanEntity>of(ShamanEntity::new, MobCategory.CREATURE)
                            .sized(1.25f, 1.6f)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build(SHAMAN_KEY)
            );

    private static final ResourceKey<EntityType<?>> ROACH_KEY =
            ResourceKey.create(
                    Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "roach")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<RoachEntity>> ROACH =
            ENTITY_TYPES.register("roach", () ->
                    EntityType.Builder.<RoachEntity>of(RoachEntity::new, MobCategory.AMBIENT)
                            .sized(0.9F, 0.35F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build(ROACH_KEY)
            );

    private static final ResourceKey<EntityType<?>> BABY_ROACH_KEY =
            ResourceKey.create(
                    Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "baby_roach")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<BabyRoachEntity>> BABY_ROACH =
            ENTITY_TYPES.register("baby_roach", () ->
                    EntityType.Builder.<BabyRoachEntity>of(BabyRoachEntity::new, MobCategory.AMBIENT)
                            .sized(0.30F, 0.16F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build(BABY_ROACH_KEY)
            );

    private static final ResourceKey<EntityType<?>> REGULAR_CENTIPEDE_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "regular_centipede"));

    public static final DeferredHolder<EntityType<?>, EntityType<RegularCentipedeEntity>> REGULAR_CENTIPEDE =
            ENTITY_TYPES.register("regular_centipede", () ->
                    EntityType.Builder.<RegularCentipedeEntity>of(
                                    RegularCentipedeEntity::new, MobCategory.AMBIENT)
                            .sized(0.32F, 0.16F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build(REGULAR_CENTIPEDE_KEY)
            );

    private static final ResourceKey<EntityType<?>> GIANT_CENTIPEDE_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "giant_centipede"));

    public static final DeferredHolder<EntityType<?>, EntityType<GiantCentipedeEntity>> GIANT_CENTIPEDE =
            ENTITY_TYPES.register("giant_centipede", () ->
                    EntityType.Builder.<GiantCentipedeEntity>of(GiantCentipedeEntity::new, MobCategory.MONSTER)
                            .sized(0.88F, 0.42F)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .noLootTable()
                            .build(GIANT_CENTIPEDE_KEY)
            );

    private ModEntities() {}
}
