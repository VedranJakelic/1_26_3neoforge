package net.beamex.shamaschizm.event.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.registry.ModBlocks;
import net.beamex.shamaschizm.world.block.SchizmPortalBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Renders every planar connected portal component as one continuous animated surface. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class PortalVisualClient {
    private static final int FRAME_COUNT = 32;
    private static final Set<BlockPos> PORTAL_BLOCKS = new HashSet<>();
    private static List<PortalSurface> surfaces = List.of();
    private static ClientLevel currentLevel;
    private static boolean dirty = true;

    private PortalVisualClient() {}

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ClientLevel level && event.getChunk() instanceof LevelChunk chunk) {
            ensureLevel(level);
            scanChunk(chunk);
        }
    }

    @SubscribeEvent
    public static void chunkUnloaded(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel level && event.getChunk() instanceof LevelChunk chunk) {
            ensureLevel(level);
            int chunkX = chunk.getPos().x();
            int chunkZ = chunk.getPos().z();
            dirty |= PORTAL_BLOCKS.removeIf(pos -> (pos.getX() >> 4) == chunkX && (pos.getZ() >> 4) == chunkZ);
        }
    }

    /** Called by the ClientLevel mixin whenever a server block update reaches the client. */
    public static void blockChanged(ClientLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        ensureLevel(level);
        boolean wasPortal = oldState.is(ModBlocks.SCHIZM_PORTAL);
        boolean isPortal = newState.is(ModBlocks.SCHIZM_PORTAL);
        if (wasPortal == isPortal) {
            if (isPortal && oldState.getValue(SchizmPortalBlock.AXIS) != newState.getValue(SchizmPortalBlock.AXIS)) dirty=true;
            return;
        }
        if (isPortal) PORTAL_BLOCKS.add(pos.immutable());
        else PORTAL_BLOCKS.remove(pos);
        dirty = true;
    }

    @SubscribeEvent
    public static void submitPortalGeometry(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;
        ensureLevel(level);
        if (dirty) rebuildSurfaces(level);
        if (surfaces.isEmpty()) return;

        int frame = (int)(level.getGameTime() % FRAME_COUNT);
        Identifier texture = Shamaschizm.id(String.format("textures/misc/schizm_portal/frame_%02d.png", frame));
        RenderType renderType = RenderTypes.entityCutout(texture, false);
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack stack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();

        stack.pushPose();
        stack.translate(-camera.x, -camera.y, -camera.z);
        List<PortalSurface> snapshot = surfaces;
        collector.submitCustomGeometry(stack, renderType, (pose, buffer) -> {
            for (PortalSurface surface : snapshot) surface.render(pose, buffer);
        });
        stack.popPose();
    }

    private static void ensureLevel(ClientLevel level) {
        if (currentLevel != level) {
            currentLevel = level;
            PORTAL_BLOCKS.clear();
            surfaces = List.of();
            dirty = true;
        }
    }

    private static void scanChunk(LevelChunk chunk) {
        boolean changed = false;
        LevelChunkSection[] sections = chunk.getSections();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section.hasOnlyAir() || !section.maybeHas(state -> state.is(ModBlocks.SCHIZM_PORTAL))) continue;
            int baseY = chunk.getMinY() + sectionIndex * 16;
            for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
                if (section.getBlockState(x, y, z).is(ModBlocks.SCHIZM_PORTAL)) {
                    changed |= PORTAL_BLOCKS.add(new BlockPos(baseX + x, baseY + y, baseZ + z));
                }
            }
        }
        dirty |= changed;
    }

    private static void rebuildSurfaces(ClientLevel level) {
        PORTAL_BLOCKS.removeIf(pos -> level.hasChunkAt(pos) && !level.getBlockState(pos).is(ModBlocks.SCHIZM_PORTAL));
        Set<BlockPos> remaining = new HashSet<>(PORTAL_BLOCKS);
        List<PortalSurface> rebuilt = new ArrayList<>();
        while (!remaining.isEmpty()) {
            BlockPos seed = remaining.iterator().next();
            BlockState seedState = level.getBlockState(seed);
            if (!seedState.is(ModBlocks.SCHIZM_PORTAL)) {
                remaining.remove(seed);
                continue;
            }
            Direction.Axis axis = seedState.getValue(SchizmPortalBlock.AXIS);
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            List<BlockPos> blocks = new ArrayList<>();
            queue.add(seed);
            remaining.remove(seed);
            while (!queue.isEmpty()) {
                BlockPos pos = queue.removeFirst();
                blocks.add(pos);
                for (Direction direction : neighbors(axis)) {
                    BlockPos next = pos.relative(direction);
                    if (!remaining.contains(next)) continue;
                    BlockState state = level.getBlockState(next);
                    if (state.is(ModBlocks.SCHIZM_PORTAL) && state.getValue(SchizmPortalBlock.AXIS) == axis) {
                        remaining.remove(next);
                        queue.addLast(next);
                    }
                }
            }
            rebuilt.add(PortalSurface.create(axis, blocks));
        }
        surfaces = List.copyOf(rebuilt);
        dirty = false;
    }

    private static Direction[] neighbors(Direction.Axis axis) {
        return axis == Direction.Axis.X
                ? new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST}
                : new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH};
    }

    private record PortalSurface(Direction.Axis axis, List<BlockPos> blocks,
                                 double minHorizontal, double maxHorizontal,
                                 double minY, double maxY) {
        static PortalSurface create(Direction.Axis axis, List<BlockPos> blocks) {
            int minH = Integer.MAX_VALUE, maxH = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
            for (BlockPos pos : blocks) {
                int h = axis == Direction.Axis.X ? pos.getX() : pos.getZ();
                minH = Math.min(minH, h);
                maxH = Math.max(maxH, h + 1);
                minY = Math.min(minY, pos.getY());
                maxY = Math.max(maxY, pos.getY() + 1);
            }
            return new PortalSurface(axis, List.copyOf(blocks), minH, maxH, minY, maxY);
        }

        void render(PoseStack.Pose pose, VertexConsumer buffer) {
            for (BlockPos pos : blocks) {
                double h0 = axis == Direction.Axis.X ? pos.getX() : pos.getZ();
                double h1 = h0 + 1.0;
                double width = maxHorizontal - minHorizontal;
                double height = maxY - minY;
                float u0 = (float)((h0 - minHorizontal) / width);
                float u1 = (float)((h1 - minHorizontal) / width);
                float v0 = (float)((maxY - (pos.getY() + 1.0)) / height);
                float v1 = (float)((maxY - pos.getY()) / height);
                if (axis == Direction.Axis.X) renderX(pose, buffer, pos, u0, u1, v0, v1);
                else renderZ(pose, buffer, pos, u0, u1, v0, v1);
            }
        }

        private static void renderX(PoseStack.Pose pose, VertexConsumer b, BlockPos p,
                                    float u0, float u1, float v0, float v1) {
            float x0 = p.getX(), x1 = x0 + 1, y0 = p.getY(), y1 = y0 + 1, z = p.getZ() + 0.5F;
            vertex(b, pose, x0, y1, z, u0, v0, 0, 0, -1);
            vertex(b, pose, x0, y0, z, u0, v1, 0, 0, -1);
            vertex(b, pose, x1, y0, z, u1, v1, 0, 0, -1);
            vertex(b, pose, x1, y1, z, u1, v0, 0, 0, -1);
        }

        private static void renderZ(PoseStack.Pose pose, VertexConsumer b, BlockPos p,
                                    float u0, float u1, float v0, float v1) {
            float z0 = p.getZ(), z1 = z0 + 1, y0 = p.getY(), y1 = y0 + 1, x = p.getX() + 0.5F;
            vertex(b, pose, x, y1, z0, u0, v0, -1, 0, 0);
            vertex(b, pose, x, y0, z0, u0, v1, -1, 0, 0);
            vertex(b, pose, x, y0, z1, u1, v1, -1, 0, 0);
            vertex(b, pose, x, y1, z1, u1, v0, -1, 0, 0);
        }

        private static void vertex(VertexConsumer buffer, PoseStack.Pose pose,
                                   float x, float y, float z, float u, float v,
                                   float nx, float ny, float nz) {
            buffer.addVertex(pose, x, y, z)
                    .setColor(255, 255, 255, 255)
                    .setUv(u, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(15728880)
                    .setNormal(pose, nx, ny, nz);
        }
    }
}
