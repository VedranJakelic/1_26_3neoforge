package net.beamex.shamaschizm.banner.client;

import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Banner cloth geometry derived from the uploaded 64x64 banner texture.
 * Each character represents one texel of the 20x40 original banner face.
 * Pixel strips are tessellated so BOTH the base colour and every vanilla
 * banner-pattern pass are physically missing at holes/frayed edges.
 *
 * The flag keeps vanilla 20x40 UVs, so Minecraft's sprite overlays align.
 */
public final class TatteredBannerGeometry {
    private static final String[] MASK = {
        "####################",
        "####################",
        "####################",
        "####################",
        "####################",
        "####################",
        "####################",
        "####.###############",
        "##..################",
        "##..################",
        "###.################",
        "####################",
        "####.###############",
        "####################",
        "###################.",
        "###################.",
        "###################.",
        "###################.",
        "###################.",
        ".##################.",
        ".#################..",
        ".#################..",
        ".#################..",
        ".#################..",
        ".################...",
        ".##############.#...",
        ".################...",
        ".#############.##...",
        ".################...",
        "..###############...",
        "...###############..",
        "...###############..",
        "...###############..",
        "...#.#############..",
        "...#.#############..",
        ".....############...",
        ".....###.######.....",
        ".....##########.....",
        "......#.######.#....",
        ".............#......"
    };

    private TatteredBannerGeometry() {}

    public static LayerDefinition createFlagLayer(boolean standing) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder strips = CubeListBuilder.create();
        Set<Direction> front = EnumSet.of(Direction.NORTH);
        Set<Direction> back = EnumSet.of(Direction.SOUTH);

        // Combine matching vertical spans to avoid excessive polygons.
        for (int y = 0; y < 40; y++) {
            int x = 0;
            while (x < 20) {
                if (MASK[y].charAt(x) == '.') { x++; continue; }
                int left = x;
                while (x < 20 && MASK[y].charAt(x) == '#') x++;
                int width = x - left;
                int height = 1; // one row; exact UV mapping, no duplicate faces

                // Vanilla cloth UV: north [1..21]x[1..41], south [22..42]x[1..41].
                // Tex offsets compensate for face-direction texture unwrap.
                strips.texOffs(left, y)
                      .addBox(-10 + left, y, -2, width, height, 1, front);
                strips.texOffs(40 - left - 2 * width, y)
                      .addBox(-10 + left, y, -2, width, height, 1, back);
                // No SIDE geometry on the strips: internal strips must not
                // create artificial seams on the original one-pixel cloth.
            }
        }

        root.addOrReplaceChild("flag", strips,
                PartPose.offset(0.0F, standing ? -44.0F : -20.5F,
                        standing ? 0.0F : 10.5F));
        return LayerDefinition.create(mesh, 64, 64);
    }

}
