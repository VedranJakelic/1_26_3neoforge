// File: src/main/java/net/beamex/shamaschizm/registry/ModBlocks.java
package net.beamex.shamaschizm.registry;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/** Plain holder; registration is done in event/ModItemBootstrap. */
public final class ModBlocks {
    private ModBlocks() {}

    public static final Identifier SCHIZM_PORTAL_ID =
            Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "schizm_portal");
    public static final ResourceKey<Block> SCHIZM_PORTAL_KEY =
            ResourceKey.create(Registries.BLOCK, SCHIZM_PORTAL_ID);

    // Assigned during BLOCK register in ModItemBootstrap
    public static Block SCHIZM_PORTAL;
}
