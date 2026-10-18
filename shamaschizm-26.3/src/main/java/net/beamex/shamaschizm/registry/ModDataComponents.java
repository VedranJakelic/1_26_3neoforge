package net.beamex.shamaschizm.registry;

import com.mojang.serialization.Codec;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Data component registration without IEventBus or RegistryObject#get(). */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class ModDataComponents {
    private ModDataComponents() {}

    public static DataComponentType<Boolean> RAISED_ENCHANT_CAP;
    /** Marks vanilla netherite armor whose equipped model is replaced by the Ancient set. */
    public static DataComponentType<Boolean> ANCIENT_APPEARANCE;

    @SubscribeEvent
    public static void onRegister(final RegisterEvent e) {
        e.register(Registries.DATA_COMPONENT_TYPE, helper -> {
            RAISED_ENCHANT_CAP = DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL) // required for container_set_slot
                    .build();
            helper.register(
                    Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "raised_enchant_cap"),
                    RAISED_ENCHANT_CAP
            );

            ANCIENT_APPEARANCE = DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build();
            helper.register(
                    Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "ancient_appearance"),
                    ANCIENT_APPEARANCE
            );
        });
    }
}
