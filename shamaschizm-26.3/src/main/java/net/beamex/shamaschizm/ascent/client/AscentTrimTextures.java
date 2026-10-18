package net.beamex.shamaschizm.ascent.client;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.resources.palette.PalettedTextureManager;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterTextureAtlasesEvent;
/** Preserve Ascent animation frames through the 26.3 palette renderer. */
@EventBusSubscriber(modid="shamaschizm",value=Dist.CLIENT)
public final class AscentTrimTextures {
 private static final Identifier ATLAS_ID=Identifier.fromNamespaceAndPath("shamaschizm","ascent_trims");
 private static final Identifier ATLAS_TEXTURE=Identifier.fromNamespaceAndPath("shamaschizm","textures/atlas/ascent_trims.png");
 @SubscribeEvent public static void register(RegisterTextureAtlasesEvent event) {
  event.register(new AtlasManager.AtlasConfig(ATLAS_TEXTURE,ATLAS_ID,false));
 }
 public static PalettedTextureManager.Handle handle(Identifier spriteId) {
  return new AscentHandle(spriteId);
 }
 /** Also identifies the Ascent-only render pass; never changes the base armor. */
 public record AscentHandle(Identifier spriteId) implements PalettedTextureManager.Handle {
   private TextureAtlasSprite sprite() { return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(ATLAS_ID).getSprite(spriteId); }
   public Identifier textureLocation(){return ATLAS_TEXTURE;}
   public float getU(float u){return sprite().getU(u);}
   public float getV(float v){return sprite().getV(v);}
   @Override public VertexConsumer wrap(VertexConsumer buffer) {
    VertexConsumer mapped = new SpriteCoordinateExpander(buffer, this);
    return spriteId.getPath().startsWith("trims/entity/humanoid/")
        ? new AscentSleeveFilter(mapped) : mapped;
   }
 }
}
