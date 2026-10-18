package net.beamex.shamaschizm.event;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.ModEntities;
import net.beamex.shamaschizm.entity.custom.ShamanEntity;
import net.beamex.shamaschizm.entity.custom.RoachEntity;
import net.beamex.shamaschizm.entity.custom.BabyRoachEntity;
import net.beamex.shamaschizm.entity.custom.GiantCentipedeEntity;
import net.beamex.shamaschizm.entity.custom.RegularCentipedeEntity;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
// Your environment requires the `.listener` package:
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;


@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class AttributesInit {

    private AttributesInit() {}

    @SubscribeEvent
    public static void onEntityAttributes(final EntityAttributeCreationEvent event) {
        event.put(ModEntities.SHAMAN.get(), ShamanEntity.createAttributes().build());
        event.put(ModEntities.ROACH.get(), RoachEntity.createAttributes().build());
        event.put(ModEntities.BABY_ROACH.get(), BabyRoachEntity.createAttributes().build());
        event.put(ModEntities.REGULAR_CENTIPEDE.get(), RegularCentipedeEntity.createAttributes().build());
        event.put(ModEntities.GIANT_CENTIPEDE.get(), GiantCentipedeEntity.createAttributes().build());
    }
}
