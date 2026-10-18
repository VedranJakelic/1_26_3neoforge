package net.beamex.shamaschizm.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Saved per dimension, not inferred from room blocks that players may edit. */
public final class PortalLinks extends SavedData {
    public record Link(BlockPos source, BlockPos doorway) {
        static final Codec<Link> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("source").forGetter(Link::source),
                BlockPos.CODEC.fieldOf("doorway").forGetter(Link::doorway)
        ).apply(i, Link::new));
    }
    private final List<Link> links;
    private static final Codec<PortalLinks> CODEC = Link.CODEC.listOf().xmap(PortalLinks::new, d -> d.links);
    public static final SavedDataType<PortalLinks> TYPE = new SavedDataType<>(
            net.beamex.shamaschizm.Shamaschizm.id("portal_links"), () -> new PortalLinks(List.of()), CODEC, null);
    public PortalLinks(List<Link> links) { this.links = new ArrayList<>(links); }
    public static PortalLinks get(ServerLevel schizm) { return schizm.getDataStorage().computeIfAbsent(TYPE); }
    public Link source(BlockPos source) { return links.stream().filter(l -> l.source.equals(source)).findFirst().orElse(null); }
    public Link room(BlockPos portal, int radius) {
        return links.stream().filter(l -> Math.abs(l.doorway.getX() - portal.getX()) <= radius
                && Math.abs(l.doorway.getY() - portal.getY()) <= radius
                && Math.abs(l.doorway.getZ() - portal.getZ()) <= radius)
                .min(java.util.Comparator.comparingDouble(l -> l.doorway.distSqr(portal))).orElse(null);
    }
    public boolean reserved(BlockPos doorway, int separation) {
        return links.stream().anyMatch(l -> Math.abs((long) l.doorway.getX() - doorway.getX()) < separation
                && Math.abs((long) l.doorway.getZ() - doorway.getZ()) < separation);
    }
    public void add(Link link) { links.add(link); setDirty(); }

    /** Source positions are used to seed the atmosphere index for portals
     * created before that index was introduced. */
    public List<BlockPos> sourcePositions() {
        return links.stream().map(Link::source).toList();
    }

    /** Resolve one reserved dungeon horizontally, regardless of floor height.
     * Refuse ambiguous boundary positions rather than select another dungeon. */
    public Link dungeonAt(BlockPos position, int radius) {
        Link result = null;
        for (Link link : links) {
            if (Math.abs((long) link.doorway.getX() - position.getX()) > radius
                    || Math.abs((long) link.doorway.getZ() - position.getZ()) > radius) continue;
            if (result != null) return null;
            result = link;
        }
        return result;
    }
}
