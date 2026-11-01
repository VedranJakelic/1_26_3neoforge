package net.beamex.shamaschizm.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.worldgen.ShamaschizmStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.StructureSettings;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

import java.util.List;
import java.util.Optional;

/**
 * One starting chunk per dominant summit instead of generating in a random
 * starting chunk and moving the piece 80+ blocks away. The structure's X/Z
 * centre is ALWAYS the starting chunk centre, which is also its /locate point.
 *
 * The expensive summit check is performed only for candidate chunks whose
 * centre is already in an eligible mountain biome.
 */
public final class MountainPeakStructure extends Structure {
    private static final TagKey<Biome> MOUNTAIN_BIOMES = TagKey.create(
            Registries.BIOME, Shamaschizm.id("has_structure/agartha4"));
    private static final Heightmap.Types SURFACE = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
    private static final int TEMPLATE_ANCHOR_Y = 4;
    private static final int TEMPLATE_HIGHEST_Y = 27;
    // 41x35 template around its (20,4,17) anchor. Covers all four rotations,
    // plus a small buffer, even when the exact peak is near a chunk edge.
    private static final int FOOTPRINT_HALF_EXTENT = 24;

    public static final MapCodec<MountainPeakStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    settingsCodec(instance),
                    StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
                    Identifier.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
                    Codec.intRange(0, 20).fieldOf("size").forGetter(s -> s.maxDepth),
                    HeightProvider.CODEC.fieldOf("start_height").forGetter(s -> s.startHeight),
                    Codec.BOOL.fieldOf("use_expansion_hack").forGetter(s -> s.useExpansionHack),
                    Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap")
                            .forGetter(s -> s.projectStartToHeightmap),
                    Codec.intRange(1, 128).fieldOf("max_distance_from_center")
                            .forGetter(s -> s.maxDistanceFromCenter),
                    Codec.intRange(32, 512).optionalFieldOf("peak_search_radius", 256)
                            .forGetter(s -> s.peakSearchRadius),
                    Codec.intRange(1, 320).optionalFieldOf("min_peak_height", 100)
                            .forGetter(s -> s.minPeakHeight),
                    Codec.intRange(0, 32).optionalFieldOf("min_relief", 8)
                            .forGetter(s -> s.minRelief)
            ).apply(instance, MountainPeakStructure::new));

    private final Holder<StructureTemplatePool> startPool;
    private final Optional<Identifier> startJigsawName;
    private final int maxDepth;
    private final HeightProvider startHeight;
    private final boolean useExpansionHack;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;
    private final int peakSearchRadius;
    private final int minPeakHeight;
    private final int minRelief;

    public MountainPeakStructure(
            StructureSettings settings,
            Holder<StructureTemplatePool> startPool,
            Optional<Identifier> startJigsawName,
            int maxDepth,
            HeightProvider startHeight,
            boolean useExpansionHack,
            Optional<Heightmap.Types> projectStartToHeightmap,
            int maxDistanceFromCenter,
            int peakSearchRadius,
            int minPeakHeight,
            int minRelief) {
        super(settings);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.maxDepth = maxDepth;
        this.startHeight = startHeight;
        this.useExpansionHack = useExpansionHack;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.peakSearchRadius = peakSearchRadius;
        this.minPeakHeight = minPeakHeight;
        this.minRelief = minRelief;
    }

    @Override
    public StructureType<?> type() {
        return ShamaschizmStructures.MOUNTAIN_PEAK_STRUCTURE.get();
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkGenerator generator = context.chunkGenerator();
        BiomeResolver biomeResolver = context.biomeResolver();
        int cx = context.chunkPos().getMiddleBlockX();
        int cz = context.chunkPos().getMiddleBlockZ();
        int centralY = surfaceY(generator, context, cx, cz);
        if (centralY < minPeakHeight || !mountain(biomeResolver, cx, centralY, cz)) {
            return Optional.empty();
        }

        // Find the best peak inside *this* 16x16 starting chunk, rather than
        // selecting another chunk's summit and moving the structure start.
        int chunkMinX = context.chunkPos().getMinBlockX();
        int chunkMinZ = context.chunkPos().getMinBlockZ();
        int peakX = cx;
        int peakZ = cz;
        int peakY = centralY;
        for (int x = chunkMinX; x < chunkMinX + 16; x += 2) {
            for (int z = chunkMinZ; z < chunkMinZ + 16; z += 2) {
                int h = surfaceY(generator, context, x, z);
                if (h > peakY && mountain(biomeResolver, x, h, z)) {
                    peakX = x;
                    peakZ = z;
                    peakY = h;
                }
            }
        }
        // Refine at 1-block resolution so the best position inside this
        // chunk cannot be a sub-grid miss on a narrow rocky spike.
        for (int x = chunkMinX; x < chunkMinX + 16; x++) {
            for (int z = chunkMinZ; z < chunkMinZ + 16; z++) {
                int h = surfaceY(generator, context, x, z);
                if (h > peakY && mountain(biomeResolver, x, h, z)) {
                    peakX = x;
                    peakZ = z;
                    peakY = h;
                }
            }
        }

        // Discard ordinary mountain sides: if ANY sampled point in the
        // surrounding 512x512 square is taller, this is not the dominant summit.
        // Search the nearest columns first for cheap rejection on slopes.
        for (int radius = 8; radius <= peakSearchRadius; radius += 8) {
            for (int dx = -radius; dx <= radius; dx += 8) {
                if (hasBetterPeak(generator, context, biomeResolver,
                        peakX + dx, peakZ - radius, peakY)) return Optional.empty();
                if (hasBetterPeak(generator, context, biomeResolver,
                        peakX + dx, peakZ + radius, peakY)) return Optional.empty();
            }
            for (int dz = -radius + 8; dz < radius; dz += 8) {
                if (hasBetterPeak(generator, context, biomeResolver,
                        peakX - radius, peakZ + dz, peakY)) return Optional.empty();
                if (hasBetterPeak(generator, context, biomeResolver,
                        peakX + radius, peakZ + dz, peakY)) return Optional.empty();
            }
        }

        // Require a prominent summit, not a flat alpine shelf. The heightmap
        // below counts surface blocks, not the eventual building itself.
        int minimumAround = peakY;
        for (int dx = -64; dx <= 64; dx += 64) {
            for (int dz = -64; dz <= 64; dz += 64) {
                if (dx == 0 && dz == 0) continue;
                minimumAround = Math.min(minimumAround,
                        surfaceY(generator, context, peakX + dx, peakZ + dz));
            }
        }
        if (peakY - minimumAround < minRelief) return Optional.empty();

        // EXACT (one-block resolution) terrain maximum under the entire
        // rotated 41x35 template footprint, plus a safety margin.
        int highestGround = peakY;
        for (int dx = -FOOTPRINT_HALF_EXTENT; dx <= FOOTPRINT_HALF_EXTENT; dx++) {
            for (int dz = -FOOTPRINT_HALF_EXTENT; dz <= FOOTPRINT_HALF_EXTENT; dz++) {
                highestGround = Math.max(highestGround,
                        surfaceY(generator, context, cx + dx, cz + dz));
            }
        }
        int clearance = startHeight.sample(context.random(),
                new WorldGenerationContext(generator, context.heightAccessor()));
        if (clearance < 0) return Optional.empty();
        int anchorY = highestGround + clearance;
        if (anchorY - TEMPLATE_ANCHOR_Y < context.heightAccessor().getMinY()
                || anchorY + TEMPLATE_HIGHEST_Y - TEMPLATE_ANCHOR_Y >= context.heightAccessor().getMaxY()) {
            return Optional.empty();
        }

        // The NBT has a single named start jigsaw at local (20,4,17).
        // We explicitly DISABLE heightmap reprojection so that the anchor's
        // world Y is precisely highestGround + clearance. No double offset.
        BlockPos anchor = new BlockPos(cx, anchorY, cz);
        return JigsawPlacement.addPieces(context, startPool, startJigsawName, maxDepth,
                anchor, useExpansionHack, Optional.empty(),
                new JigsawStructure.MaxDistance(maxDistanceFromCenter, maxDistanceFromCenter),
                PoolAliasLookup.create(List.of(), anchor, context.seed()),
                JigsawStructure.DEFAULT_DIMENSION_PADDING,
                JigsawStructure.DEFAULT_LIQUID_SETTINGS);
    }

    private static boolean hasBetterPeak(ChunkGenerator generator, GenerationContext context,
                                         BiomeResolver biomes, int x, int z, int peakY) {
        int h = surfaceY(generator, context, x, z);
        return h > peakY && mountain(biomes, x, h, z);
    }

    private static boolean mountain(BiomeResolver biomes, int x, int y, int z) {
        return biomes.getNoiseBiome(x >> 2, (y - 1) >> 2, z >> 2).is(MOUNTAIN_BIOMES);
    }

    private static int surfaceY(ChunkGenerator generator, GenerationContext context,
                                int x, int z) {
        return generator.getFirstFreeHeight(x, z, SURFACE,
                context.heightAccessor(), context.randomState());
    }
}
