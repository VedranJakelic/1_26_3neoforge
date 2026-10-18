package net.beamex.shamaschizm.world.teleport;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.registry.ModBlocks;
import net.beamex.shamaschizm.world.PortalLinks;
import net.beamex.shamaschizm.world.Schizm;
import net.beamex.shamaschizm.world.dungeon.SchizmDungeonGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.Vec3;
import net.beamex.shamaschizm.world.block.SchizmStoneRegistration;

public final class SchizmTeleporter {
    private static final int SCALE = 8;
    private SchizmTeleporter() {}

    private static BlockPos canonical(ServerLevel level, BlockPos touched) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(touched.immutable()); queue.add(touched.immutable());
        BlockPos first = touched;
        while (!queue.isEmpty() && seen.size() < 512) {
            BlockPos p = queue.remove();
            if (p.compareTo(first) < 0) first = p;
            for (Direction direction : Direction.values()) {
                BlockPos next = p.relative(direction);
                if (level.getBlockState(next).is(ModBlocks.SCHIZM_PORTAL) && seen.add(next)) queue.add(next);
            }
        }
        return first.immutable();
    }

    public static void teleportOverworldToSchizm(ServerPlayer player, BlockPos touched) {
        ServerLevel target = player.level().getServer().getLevel(Schizm.KEY);
        if (target == null) return;
        BlockPos source = canonical((ServerLevel) player.level(), touched);
        PortalLinks data = PortalLinks.get(target);
        PortalLinks.Link link = data.source(source);
        if (link == null) {
            int y = target.getMaxY() - SchizmDungeonGenerator.MAX_ROOM_HEIGHT;
            var prepared = SchizmDungeonGenerator.prepare(target, y - 1,
                    target.getSeed() ^ source.asLong());
            if (prepared == null) return;
            var template = prepared.startTemplate();
            var size = template.getSize();
            int separation = SchizmDungeonGenerator.RESERVATION_RADIUS * 2 + 16;
            long x = (long) source.getX() * SCALE;
            long z = (long) source.getZ() * SCALE;
            // Stay inside the world border including the whole template footprint.
            int max = (int) Math.min(29_999_000, target.getWorldBorder().getSize() / 2 - separation);
            int baseX = (int) Math.clamp(x, -max, max);
            int baseZ = (int) Math.clamp(z, -max, max);
            BlockPos doorway = new BlockPos(baseX, y, baseZ);
            // Nearby source portals must not overwrite an existing room. Allocate the nearest free grid cell.
            boolean free = !data.reserved(doorway, separation) && canUseRoom(target, template, doorway, prepared.startExit());
            for (int ring = 1; !free && ring <= 128; ring++) {
                for (int dx = -ring; dx <= ring && !free; dx++) for (int dz = -ring; dz <= ring && !free; dz++) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) continue;
                    BlockPos candidate = new BlockPos(baseX + dx * separation, y, baseZ + dz * separation);
                    if (Math.abs(candidate.getX()) > max || Math.abs(candidate.getZ()) > max) continue;
                    if (!data.reserved(candidate, separation) && canUseRoom(target, template, candidate, prepared.startExit())) { doorway = candidate; free = true; }
                }
            }
            if (!free) return;
            BlockPos origin = doorway.subtract(prepared.startExit());
            if (origin.getY() < target.getMinY() || origin.getY() + size.getY() > target.getMaxY() + 1) return;
            var generated = SchizmDungeonGenerator.generate(target, origin, prepared);
            if (!generated.placed()) return;
            // Prefer a safe tile beside the actual return portal in the new template.
            BlockPos arrival = null;
            var portals = template.filterBlocks(origin, new StructurePlaceSettings(), ModBlocks.SCHIZM_PORTAL);
            for (var portal : portals) {
                arrival = safeNear(target, portal.pos());
                if (arrival != null) break;
            }
            if (arrival == null) {
                for (BlockPos p : BlockPos.betweenClosed(origin, origin.offset(size.getX()-1, size.getY()-1, size.getZ()-1))) {
                    if (safeTile(target, p)) { arrival = p.immutable(); break; }
                }
            }
            // Save even when the template has no usable arrival tile, so retries never regenerate it.
            link = new PortalLinks.Link(source, arrival != null ? arrival : doorway);
            data.add(link);
            if (arrival == null) {
                Shamaschizm.LOGGER.error("{} has no safe player arrival tile; check its saved air and floor", prepared.startName());
                return;
            }
        }
        BlockPos safe = safeNear(target, link.doorway());
        if (safe != null) move(player, target, safe);
    }

    private static boolean canUseRoom(ServerLevel level,
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate template,
            BlockPos doorway, BlockPos marker) {
        BlockPos origin = doorway.subtract(marker);
        var size = template.getSize();
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(size.getX()-1, size.getY()-1, size.getZ()-1))) {
            // Preserve excavations, structures, and player edits in worlds created by the old version.
            var state = level.getBlockState(pos);
            if (!state.is(net.minecraft.world.level.block.Blocks.STONE)
                    && !state.is(SchizmStoneRegistration.SCHIZM_STONE)) {
                return false;
            }
        }
        return true;
    }

    public static void teleportSchizmToOverworld(ServerPlayer player, BlockPos touched) {
        ServerLevel schizm = (ServerLevel) player.level();
        ServerLevel target = schizm.getServer().getLevel(Level.OVERWORLD);
        if (target == null) return;
        PortalLinks.Link link = PortalLinks.get(schizm).room(touched, 32);
        BlockPos wanted = link != null ? link.source() : new BlockPos(
                Math.floorDiv(touched.getX(), SCALE), 0, Math.floorDiv(touched.getZ(), SCALE));
        target.getChunkAt(wanted);
        BlockPos safe = link != null ? safeNear(target, wanted) : null;
        if (safe == null) safe = safeNear(target, new BlockPos(wanted.getX(),
                target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wanted.getX(), wanted.getZ()), wanted.getZ()));
        if (safe != null) move(player, target, safe);
    }

    private static BlockPos safeNear(ServerLevel level, BlockPos center) {
        for (int r = 0; r <= 8; r++) for (int dy = -2; dy <= 2; dy++)
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                BlockPos p = center.offset(dx, dy, dz);
                if (safeTile(level, p)) return p;
            }
        return null;
    }
    private static boolean safeTile(ServerLevel level, BlockPos p) {
        return level.getWorldBorder().isWithinBounds(p) && p.getY() > level.getMinY() && p.getY() + 1 <= level.getMaxY()
                && level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                        && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                        && level.getFluidState(p).isEmpty() && level.getFluidState(p.above()).isEmpty()
                        && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)
                        && !level.getBlockState(p).is(ModBlocks.SCHIZM_PORTAL);
    }
    private static void move(ServerPlayer player, ServerLevel target, BlockPos position) {
        player.setPortalCooldown();
        player.teleportTo(target, position.getX() + 0.5, position.getY(), position.getZ() + 0.5,
                Set.of(), player.getYRot(), player.getXRot(), true);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
    }
}
