package net.beamex.shamaschizm.livingsmoke;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Small bounded 3D A* for the hovering smoke; never moves through solid walls. */
final class SmokeFlightPath {
    private static final int[][] STEP = {
            {1,0,0}, {-1,0,0}, {0,0,1}, {0,0,-1}, {0,1,0}, {0,-1,0}
    };
    private record Candidate(BlockPos pos, double g, double f) {}
    private SmokeFlightPath() {}

    static boolean clearLine(LivingSmoke mob, ServerLevel level, Vec3 target) {
        Vec3 start = mob.position().add(0, mob.getBbHeight() * .5, 0);
        Vec3 end = target.add(0, mob.getBbHeight() * .5, 0);
        return level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, mob)).getType() == HitResult.Type.MISS;
    }

    static boolean free(LivingSmoke mob, ServerLevel level, Vec3 position) {
        BlockPos block = BlockPos.containing(position);
        if (!level.hasChunkAt(block) || !level.getWorldBorder().isWithinBounds(block)) return false;
        AABB box = mob.getBoundingBox().move(position.subtract(mob.position()));
        return level.noCollision(mob, box) && !level.containsAnyLiquid(box);
    }

    /** Return reachable centers, including a useful partial route if the final goal is obstructed. */
    static List<Vec3> plan(LivingSmoke mob, ServerLevel level, Vec3 target) {
        BlockPos start = BlockPos.containing(mob.position());
        PriorityQueue<Candidate> open = new PriorityQueue<>(Comparator.comparingDouble(Candidate::f));
        Map<BlockPos, Double> costs = new HashMap<>();
        Map<BlockPos, BlockPos> parents = new HashMap<>();
        Set<BlockPos> visited = new HashSet<>();
        open.add(new Candidate(start, 0, heuristic(start, target)));
        costs.put(start, 0.0);
        BlockPos closest = start;
        double best = heuristic(start, target);
        int expanded = 0;
        while (!open.isEmpty() && expanded < 420) {
            Candidate current = open.poll();
            if (!visited.add(current.pos())) continue;
            expanded++;
            double closeness = heuristic(current.pos(), target);
            if (closeness < best) { best = closeness; closest = current.pos(); }
            if (closeness < 1.1) { closest = current.pos(); break; }
            for (int[] step : STEP) {
                BlockPos next = current.pos().offset(step[0], step[1], step[2]);
                if (visited.contains(next) || Math.abs(next.getX()-start.getX()) > 18
                        || Math.abs(next.getZ()-start.getZ()) > 18
                        || Math.abs(next.getY()-start.getY()) > 7) continue;
                Vec3 candidate = center(next);
                if (!free(mob, level, candidate)) continue;
                Vec3 from = current.pos().equals(start) ? mob.position() : center(current.pos());
                AABB fromBox = mob.getBoundingBox().move(from.subtract(mob.position()));
                if (!level.noCollision(mob, fromBox.expandTowards(candidate.subtract(from)))) continue;
                double newCost = current.g() + (step[1] == 0 ? 1.0 : 1.3);
                if (newCost >= costs.getOrDefault(next, Double.POSITIVE_INFINITY)) continue;
                parents.put(next, current.pos());
                costs.put(next, newCost);
                open.add(new Candidate(next, newCost, newCost + heuristic(next, target)));
            }
        }
        if (closest.equals(start)) return List.of();
        List<Vec3> reversed = new ArrayList<>();
        BlockPos node = closest;
        for (int depth = 0; depth < 64 && !node.equals(start); depth++) {
            reversed.add(center(node));
            node = parents.get(node);
            if (node == null) return List.of();
        }
        Collections.reverse(reversed);
        return reversed;
    }
    private static Vec3 center(BlockPos pos) {
        return new Vec3(pos.getX() + .5, pos.getY() + .12, pos.getZ() + .5);
    }
    private static double heuristic(BlockPos p, Vec3 goal) {
        double dx = (p.getX()+.5)-goal.x, dy = (p.getY()+.12)-goal.y, dz = (p.getZ()+.5)-goal.z;
        return Math.sqrt(dx*dx + dy*dy*1.6 + dz*dz);
    }
}
