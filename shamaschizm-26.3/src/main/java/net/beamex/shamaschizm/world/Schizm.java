package net.beamex.shamaschizm.world;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public final class Schizm {
    public static final ResourceKey<Level> KEY = ResourceKey.create(
            Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(Shamaschizm.MOD_ID, "schizm")
    );
    private Schizm() {}
}