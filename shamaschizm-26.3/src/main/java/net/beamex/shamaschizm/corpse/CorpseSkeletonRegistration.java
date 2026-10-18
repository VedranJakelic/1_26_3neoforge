package net.beamex.shamaschizm.corpse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class CorpseSkeletonRegistration {
    public static EntityType<CorpseSkeletonEntity> TYPE;
    private static final Map<UUID, List<net.minecraft.world.item.ItemStack>> WORN_ON_DEATH = new HashMap<>();
    private CorpseSkeletonRegistration() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.ENTITY_TYPE, helper -> {
            var id = Shamaschizm.id("corpse_skeleton");
            TYPE = EntityType.Builder.<CorpseSkeletonEntity>of(CorpseSkeletonEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F).clientTrackingRange(10).updateInterval(3)
                    .noLootTable()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id));
            helper.register(id, TYPE);
        });
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(TYPE, AbstractSkeleton.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rememberWornArmor(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !player.level().dimension().equals(Schizm.KEY)) return;
        var worn = new ArrayList<net.minecraft.world.item.ItemStack>();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) worn.add(stack.copy());
        }
        WORN_ON_DEATH.put(player.getUUID(), worn);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void collectDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || !level.dimension().equals(Schizm.KEY)) return;
        CorpseSkeletonEntity corpse = TYPE.create(level, EntitySpawnReason.EVENT);
        if (corpse == null) return;
        var items = new ArrayList<net.minecraft.world.item.ItemStack>();
        for (ItemEntity drop : event.getDrops()) {
            if (!drop.getItem().isEmpty()) items.add(drop.getItem().copy());
        }
        Vec3 death = player.position();
        var ground = level.clip(new ClipContext(death.add(0, 0.1D, 0),
                death.add(0, -24.0D, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double floorY = ground.getType() == HitResult.Type.BLOCK ? ground.getLocation().y : death.y;
        corpse.snapTo(death.x, floorY, death.z, player.getYRot(), 0.0F);
        corpse.setCustomName(Component.literal(player.getName().getString() + "'s remains"));
        List<net.minecraft.world.item.ItemStack> worn = WORN_ON_DEATH.remove(player.getUUID());
        corpse.storeDeathDrops(items, worn == null ? List.of() : worn, player.getUUID());
        if (level.addFreshEntity(corpse)) event.getDrops().clear();
    }
}
