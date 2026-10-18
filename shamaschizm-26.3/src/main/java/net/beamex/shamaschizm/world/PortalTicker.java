package net.beamex.shamaschizm.world;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.registry.ModBlocks;
import net.beamex.shamaschizm.world.teleport.SchizmTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class PortalTicker {
    private PortalTicker() {}

    // 1.21.6+: use Post type; no deprecated phase.
    @net.neoforged.bus.api.SubscribeEvent
    public static void onPlayerTick(final PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (!sp.isAlive() || sp.isSpectator() || sp.isPassenger() || sp.isVehicle() || sp.isOnPortalCooldown()) return;

        BlockPos hit = getTouchedPortalAccurate(sp);
        if (hit == null) return;

        ServerLevel here = (ServerLevel) sp.level();
        if (here.dimension().equals(Level.OVERWORLD)) {
            SchizmTeleporter.teleportOverworldToSchizm(sp, hit);
        } else if (here.dimension().equals(net.beamex.shamaschizm.world.Schizm.KEY)) {
            SchizmTeleporter.teleportSchizmToOverworld(sp, hit);
        }
        sp.setPortalCooldown();
    }

    // Accurate: only count as touching if the player's AABB intersects the portal *shape* (thin pane)
    private static BlockPos getTouchedPortalAccurate(ServerPlayer sp) {
        AABB playerBox = sp.getBoundingBox().deflate(0.001); // why: avoid floating-point grazing
        VoxelShape playerShape = Shapes.create(playerBox);

        // Iterate the few blocks overlapped by the AABB
        BlockPos min = BlockPos.containing(Math.floor(playerBox.minX), Math.floor(playerBox.minY), Math.floor(playerBox.minZ));
        BlockPos max = BlockPos.containing(Math.floor(playerBox.maxX), Math.floor(playerBox.maxY), Math.floor(playerBox.maxZ));

        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (sp.level().getBlockState(p).getBlock() != ModBlocks.SCHIZM_PORTAL) continue;

                    BlockState st = sp.level().getBlockState(p);
                    VoxelShape portalShape = st.getShape(sp.level(), p).move(p.getX(), p.getY(), p.getZ());
                    if (Shapes.joinIsNotEmpty(portalShape, playerShape, BooleanOp.AND)) {
                        return p.immutable();
                    }
                }
            }
        }
        return null;
    }
}
