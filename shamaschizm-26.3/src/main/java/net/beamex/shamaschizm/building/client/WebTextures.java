package net.beamex.shamaschizm.building.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.mojang.blaze3d.platform.NativeImage;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/** Quantize once per encountered size, never once per frame. Nearest sampling keeps pixels crisp. */
final class WebTextures {
    private static final Identifier SOURCE = Shamaschizm.id("textures/entity/spanning_web.png");
    private static final Map<Integer, Identifier> CACHE = new HashMap<>();
    private static NativeImage source;
    static Identifier get(float width, float height) {
        int nx = Math.max(1, Math.min(80, Math.round(width * 16)));
        int ny = Math.max(1, Math.min(80, Math.round(height * 16)));
        int key = nx * 81 + ny;
        Identifier existing = CACHE.get(key);
        if (existing != null) return existing;
        var mc = Minecraft.getInstance();
        try {
            if (source == null) {
                try (var stream = mc.getResourceManager().getResourceOrThrow(SOURCE).open()) { source = NativeImage.read(stream); }
            }
            NativeImage result = new NativeImage(nx, ny, true);
            for (int y = 0; y < ny; y++) for (int x = 0; x < nx; x++) {
                // Area-weighted reduction preserves thin strands in the supplied image.
                double x0 = x * (double)source.getWidth() / nx, x1 = (x + 1) * (double)source.getWidth() / nx;
                double y0 = y * (double)source.getHeight() / ny, y1 = (y + 1) * (double)source.getHeight() / ny;
                double alpha = 0, red = 0, green = 0, blue = 0;
                for (int sy = (int)y0; sy < Math.ceil(y1); sy++) for (int sx = (int)x0; sx < Math.ceil(x1); sx++) {
                    double weight = (Math.min(x1, sx + 1) - Math.max(x0, sx)) * (Math.min(y1, sy + 1) - Math.max(y0, sy));
                    int color = source.getPixel(Math.min(sx, source.getWidth() - 1), Math.min(sy, source.getHeight() - 1));
                    double coverage = ((color >>> 24) & 255) * weight;
                    alpha += coverage; red += ((color >>> 16) & 255) * coverage;
                    green += ((color >>> 8) & 255) * coverage; blue += (color & 255) * coverage;
                }
                int color = alpha / ((x1 - x0) * (y1 - y0)) < 32 ? 0
                        : 0xFF000000 | (int)(red / alpha) << 16 | (int)(green / alpha) << 8 | (int)(blue / alpha);
                result.setPixel(x, y, color);
            }
            Identifier id = Shamaschizm.id("dynamic/web_" + nx + "_" + ny);
            mc.getTextureManager().register(id, new DynamicTexture(() -> "Spanning web " + nx + "x" + ny, result));
            CACHE.put(key, id); return id;
        } catch (IOException exception) {
            com.mojang.logging.LogUtils.getLogger().error("Unable to load spanning web texture", exception);
            CACHE.put(key, SOURCE); return SOURCE;
        }
    }
    static void clear() {
        var manager = Minecraft.getInstance().getTextureManager();
        for (Identifier id : CACHE.values()) if (!id.equals(SOURCE)) manager.release(id);
        CACHE.clear();
        if (source != null) { source.close(); source = null; }
    }
    private WebTextures() {}
}
