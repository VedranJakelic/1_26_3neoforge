package net.beamex.shamaschizm.plantedsword;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class PlantedSwords {
    public static final Identifier ID = Shamaschizm.id("planted_sword");
    public static EntityType<PlantedSwordEntity> TYPE;

    // Use the registry tag directly, without depending on a mapped constant name.
    public static final TagKey<Item> SPEARS = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath("minecraft", "spears"));

    public static boolean isSpear(ItemStack stack) { return stack.is(SPEARS); }

    public static boolean canPlant(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ItemTags.SWORDS) || isSpear(stack));
    }

    private PlantedSwords() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.ENTITY_TYPE, helper -> {
            TYPE = EntityType.Builder.<PlantedSwordEntity>of(PlantedSwordEntity::new, MobCategory.MISC)
                    .sized(0.55F, 1.25F).fireImmune()
                    .clientTrackingRange(10).updateInterval(20)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, ID));
            helper.register(ID, TYPE);
        });
    }

    /** Carries intent only: the server supplies the actual sword and position. */
    public record Plant() implements CustomPacketPayload {
        public static final Type<Plant> TYPE = new Type<>(Shamaschizm.id("plant_sword"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Plant> CODEC = new StreamCodec<>() {
            @Override public Plant decode(RegistryFriendlyByteBuf buffer) { return new Plant(); }
            @Override public void encode(RegistryFriendlyByteBuf buffer, Plant packet) {}
        };
        @Override public Type<Plant> type() { return TYPE; }
    }

    @SubscribeEvent
    public static void registerPayload(RegisterPayloadHandlersEvent event) {
        // Default payload handlers run on the main thread, serializing inventory transfers.
        event.registrar("1").playToServer(Plant.TYPE, Plant.CODEC, (packet, context) -> {
            if (context.player() instanceof ServerPlayer player) plant(player);
        });
    }

    private static void plant(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || !player.mayBuild()
                || !player.onGround() || player.isPassenger()
                || !player.getOffhandItem().isEmpty()) return;
        var held = player.getMainHandItem();
        if (!canPlant(held)) return;
        ServerLevel level = player.level();
        // Probe at the feet rather than at the crosshair. Collision-shape ray
        // tracing also puts the sword correctly on slabs, stairs and snow.
        Vec3 feet = player.position();
        var ground = level.clip(new ClipContext(feet.add(0, 0.15D, 0), feet.add(0, -0.35D, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (ground.getType() != HitResult.Type.BLOCK || ground.getDirection() != Direction.UP) return;
        if (!level.mayInteract(player, ground.getBlockPos())) return;
        Vec3 point = ground.getLocation();
        // Avoid a pile of overlapping, individually unselectable swords.
        var area = new net.minecraft.world.phys.AABB(point.x - 0.2D, point.y - 0.1D, point.z - 0.2D,
                point.x + 0.2D, point.y + 1.3D, point.z + 0.2D);
        if (!level.getEntitiesOfClass(PlantedSwordEntity.class, area).isEmpty()) return;
        PlantedSwordEntity sword = TYPE.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (sword == null) return;
        // A fixed 2..6 degree lean in a random direction, saved with the entity.
        double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
        float lean = 2.0F + level.getRandom().nextFloat() * 4.0F;
        sword.configure(held, (float) Math.cos(angle) * lean, (float) Math.sin(angle) * lean);
        sword.snapTo(point.x, point.y, point.z, player.getYRot(), 0.0F);
        if (!level.addFreshEntity(sword)) return;
        // This is a transfer even in Creative, not an item-use consumption rule.
        held.shrink(1);
        if (held.isEmpty()) player.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
        var block = level.getBlockState(ground.getBlockPos());
        var sound = block.getSoundType(level, ground.getBlockPos(), player);
        level.playSound(null, point.x, point.y, point.z, sound.getBreakSound(),
                SoundSource.PLAYERS, 0.7F, 0.9F + level.getRandom().nextFloat() * 0.15F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, block),
                point.x, point.y + 0.06D, point.z, 8, 0.12D, 0.05D, 0.12D, 0.035D);
    }
}
