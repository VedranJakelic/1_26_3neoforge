package net.beamex.shamaschizm.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.beamex.shamaschizm.worldgen.ShamaschizmStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.StructureSettings;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

import java.util.Optional;


public final class PondStructure extends Structure {


    public static final MapCodec<PondStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    // vanilla Structure settings (biomes, step, terrain_adaptation, spawn_overrides)
                    settingsCodec(instance),

                    // Jigsaw core fields
                    StructureTemplatePool.CODEC
                            .fieldOf("start_pool")
                            .forGetter(s -> s.startPool),
                    Identifier.CODEC
                            .optionalFieldOf("start_jigsaw_name")
                            .forGetter(s -> s.startJigsawName),
                    Codec.intRange(0, 20)
                            .fieldOf("size")
                            .forGetter(s -> s.maxDepth),
                    HeightProvider.CODEC
                            .fieldOf("start_height")
                            .forGetter(s -> s.startHeight),
                    Codec.BOOL
                            .fieldOf("use_expansion_hack")
                            .forGetter(s -> s.useExpansionHack),
                    Heightmap.Types.CODEC
                            .optionalFieldOf("project_start_to_heightmap")
                            .forGetter(s -> s.projectStartToHeightmap),
                    Codec.intRange(1, 128)
                            .fieldOf("max_distance_from_center")
                            .forGetter(s -> s.maxDistanceFromCenter),

                    // Extra terrain filter fields
                    Codec.INT.optionalFieldOf("radius", 6)
                            .forGetter(s -> s.radius),
                    Codec.INT.optionalFieldOf("max_slope", 2)
                            .forGetter(s -> s.maxSlope),
                    Codec.BOOL.optionalFieldOf("avoid_water", true)
                            .forGetter(s -> s.avoidWater)
            ).apply(instance, PondStructure::new)
    );

    // =========== Stored config ===========

    private final Holder<StructureTemplatePool> startPool;
    private final Optional<Identifier> startJigsawName;
    private final int maxDepth;
    private final HeightProvider startHeight;
    private final boolean useExpansionHack;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;

    private final int radius;
    private final int maxSlope;
    private final boolean avoidWater;


    private final JigsawStructure backingJigsaw;

    public PondStructure(StructureSettings settings,
                         Holder<StructureTemplatePool> startPool,
                         Optional<Identifier> startJigsawName,
                         int maxDepth,
                         HeightProvider startHeight,
                         boolean useExpansionHack,
                         Optional<Heightmap.Types> projectStartToHeightmap,
                         int maxDistanceFromCenter,
                         int radius,
                         int maxSlope,
                         boolean avoidWater) {
        super(settings);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.maxDepth = maxDepth;
        this.startHeight = startHeight;
        this.useExpansionHack = useExpansionHack;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;

        this.radius = Math.max(1, radius);
        this.maxSlope = Math.max(0, maxSlope);
        this.avoidWater = avoidWater;

        // Build a vanilla JigsawStructure with defaults for aliases/padding/liquids
        this.backingJigsaw = new JigsawStructure(
                settings,
                startPool,
                startJigsawName,
                maxDepth,
                startHeight,
                useExpansionHack,
                projectStartToHeightmap,
                new JigsawStructure.MaxDistance(maxDistanceFromCenter, maxDistanceFromCenter),
                java.util.List.of(),
                JigsawStructure.DEFAULT_DIMENSION_PADDING,
                JigsawStructure.DEFAULT_LIQUID_SETTINGS
        );
    }

    // Codec field accessors
    private int radius() {
        return radius;
    }

    private int maxSlope() {
        return maxSlope;
    }

    private boolean avoidWater() {
        return avoidWater;
    }

    @Override
    public StructureType<?> type() {
        return ShamaschizmStructures.POND_STRUCTURE.get();
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        Heightmap.Types heightmapType = projectStartToHeightmap
                .orElse(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);

        boolean agartha=startPool.unwrapKey().map(k->k.identifier().getNamespace().equals("shamaschizm")
                && java.util.Set.of("agartha1_pool","agartha2_pool","agartha3_pool").contains(k.identifier().getPath())).orElse(false);
        int x=context.chunkPos().getMiddleBlockX(),z=context.chunkPos().getMiddleBlockZ();
        int y=context.chunkGenerator().getFirstOccupiedHeight(x,z,heightmapType,context.heightAccessor(),context.randomState());
        var biome=context.biomeSource().createUncachedResolver(context.randomState()).getNoiseBiome(x>>2,y>>2,z>>2);
        boolean extra=agartha && biome.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BIOME,
                net.beamex.shamaschizm.Shamaschizm.id("agartha_extra_attempts")));
        for(int attempt=0;attempt<(extra?5:1);attempt++){
            Optional<GenerationStub> stub;
            if(attempt==0)stub=backingJigsaw.findGenerationPoint(context);
            else {
                // Independent nearby positions inside this same reserved start chunk.
                int height=startHeight.sample(context.random(),new net.minecraft.world.level.levelgen.WorldGenerationContext(context.chunkGenerator(),context.heightAccessor()));
                BlockPos start=new BlockPos(context.chunkPos().getMinBlockX()+context.random().nextInt(16),height,
                        context.chunkPos().getMinBlockZ()+context.random().nextInt(16));
                stub=net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement.addPieces(context,startPool,startJigsawName,
                        maxDepth,start,useExpansionHack,projectStartToHeightmap,
                        new JigsawStructure.MaxDistance(maxDistanceFromCenter,maxDistanceFromCenter),
                        net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup.create(java.util.List.of(),start,context.seed()),
                        JigsawStructure.DEFAULT_DIMENSION_PADDING,JigsawStructure.DEFAULT_LIQUID_SETTINGS);
            }
            if(stub.isEmpty())continue;
            BlockPos anchor=stub.get().position();
            if(!isAreaFlatEnough(context,heightmapType,anchor))continue;
            if(avoidWater&&isAreaWatery(context,heightmapType,anchor))continue;
            return stub;
        }
        return Optional.empty();
    }

    // ===================== Terrain checks =====================

    private boolean isAreaFlatEnough(GenerationContext context, Heightmap.Types heightmapType, BlockPos anchor) {
        ChunkPos chunkPos = context.chunkPos();
        int centerX = anchor.getX();
        int centerZ = anchor.getZ();

        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor heightAccessor = context.heightAccessor();
        RandomState randomState = context.randomState();

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;

                int y = generator.getFirstOccupiedHeight(
                        x,
                        z,
                        heightmapType,
                        heightAccessor,
                        randomState
                );

                if (y < minY) {
                    minY = y;
                }
                if (y > maxY) {
                    maxY = y;
                }
                if (maxY - minY > maxSlope) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isAreaWatery(GenerationContext context, Heightmap.Types heightmapType, BlockPos anchor) {
        ChunkPos chunkPos = context.chunkPos();
        int centerX = anchor.getX();
        int centerZ = anchor.getZ();

        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor heightAccessor = context.heightAccessor();
        RandomState randomState = context.randomState();
        RandomSource random = context.random();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;

                int surfaceY = generator.getFirstOccupiedHeight(
                        x,
                        z,
                        heightmapType,
                        heightAccessor,
                        randomState
                );
                int checkY = surfaceY;

                NoiseColumn column = generator.getBaseColumn(x, z, heightAccessor, randomState);
                BlockState belowSurface = column.getBlock(checkY);

                if (!belowSurface.getFluidState().isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }
}
