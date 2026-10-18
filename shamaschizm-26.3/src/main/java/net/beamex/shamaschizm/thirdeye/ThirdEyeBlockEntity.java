package net.beamex.shamaschizm.thirdeye;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ThirdEyeBlockEntity extends TheEndPortalBlockEntity {
    private static final String PENDING_PLAYERS = "PendingPlayers";
    private static final int TELEPORT_RADIUS = 60;
    private static final int RANDOM_COLUMNS = 512;
    private final Set<UUID> pendingPlayers = new LinkedHashSet<>();

    public ThirdEyeBlockEntity(BlockPos pos, BlockState state) {
        super(ThirdEyeRegistration.BLOCK_ENTITY, pos, state);
    }

    @Override
    public boolean shouldRenderFace(Direction direction) {
        return direction.getAxis().isHorizontal();
    }

    public void setPendingPlayers(List<ServerPlayer> players) {
        pendingPlayers.clear();
        for (ServerPlayer player : players) pendingPlayers.add(player.getUUID());
        setChanged();
    }

    public void applyNausea(ServerLevel level) {
        for (ServerPlayer player : resolvePlayers(level)) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 60, 0));
        }
    }

    public void teleportPendingPlayers(ServerLevel level, BlockPos center) {
        for (ServerPlayer player : resolvePlayers(level)) {
            BlockPos destination = findDestination(level, player, center);
            if (destination == null) continue;
            Vec3 target = Vec3.atBottomCenterOf(destination);
            player.teleportTo(level, target.x, target.y, target.z, Set.of(),
                    player.getYRot(), player.getXRot(), true);
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0.0F;
        }
        pendingPlayers.clear();
        setChanged();
    }

    private List<ServerPlayer> resolvePlayers(ServerLevel level) {
        return pendingPlayers.stream()
                .map(uuid -> level.getServer().getPlayerList().getPlayer(uuid))
                .filter(player -> player != null && player.isAlive() && !player.isSpectator()
                        && player.level() == level)
                .toList();
    }

    private static BlockPos findDestination(ServerLevel level, ServerPlayer player,
                                            BlockPos center) {
        var random = level.getRandom();
        for (int attempt = 0; attempt < RANDOM_COLUMNS; attempt++) {
            int dx = random.nextIntBetweenInclusive(-TELEPORT_RADIUS, TELEPORT_RADIUS);
            int dz = random.nextIntBetweenInclusive(-TELEPORT_RADIUS, TELEPORT_RADIUS);
            int horizontalSqr = dx * dx + dz * dz;
            if (horizontalSqr > TELEPORT_RADIUS * TELEPORT_RADIUS || horizontalSqr < 16) continue;

            int maxDy = (int)Math.floor(Math.sqrt(
                    TELEPORT_RADIUS * TELEPORT_RADIUS - horizontalSqr));
            int heightCount = maxDy * 2 + 1;
            int firstDy = random.nextInt(heightCount) - maxDy;
            for (int offset = 0; offset < heightCount; offset++) {
                int dy = -maxDy + Math.floorMod(firstDy + maxDy + offset, heightCount);
                BlockPos candidate = center.offset(dx, dy, dz);
                if (isSafe(level, player, candidate)) return candidate.immutable();
            }
        }
        return null;
    }

    private static boolean isSafe(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (!level.getWorldBorder().isWithinBounds(pos)
                || pos.getY() <= level.getMinY()
                || pos.getY() + 1 > level.getMaxY()
                || !level.getBlockState(pos).isAir()
                || !level.getBlockState(pos.above()).isAir()
                || !level.getFluidState(pos).isEmpty()
                || !level.getFluidState(pos.above()).isEmpty()) return false;

        BlockState floor = level.getBlockState(pos.below());
        if (!floor.isFaceSturdy(level, pos.below(), Direction.UP)
                || floor.is(Blocks.MAGMA_BLOCK)
                || floor.is(Blocks.CAMPFIRE)
                || floor.is(Blocks.SOUL_CAMPFIRE)
                || floor.is(Blocks.CACTUS)
                || floor.is(Blocks.POINTED_DRIPSTONE)) return false;

        Vec3 target = Vec3.atBottomCenterOf(pos);
        AABB box = player.getDimensions(Pose.STANDING).makeBoundingBox(target);
        return level.noCollision(player, box);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store(PENDING_PLAYERS, UUIDUtil.CODEC_LINKED_SET,
                new LinkedHashSet<>(pendingPlayers));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        pendingPlayers.clear();
        input.read(PENDING_PLAYERS, UUIDUtil.CODEC_LINKED_SET)
                .ifPresent(pendingPlayers::addAll);
    }
}
