package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.ascent.client.AscentTrimTextures;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.palette.PalettedTextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer$TrimTextureKey")
public abstract class AscentTrimTextureMixin {
 @Shadow @Final private ArmorTrim trim;
 @Shadow @Final private EquipmentClientInfo.LayerType layerType;
 @Shadow @Final private EquipmentClientInfo equipmentInfo;
 @Inject(method="getOrPrepareTexture",at=@At("HEAD"),cancellable=true)
 private void shamaschizm$animatedAscent(PalettedTextureManager palettes,CallbackInfoReturnable<PalettedTextureManager.Handle> cir) {
  if(!trim.pattern().value().assetId().equals(Identifier.fromNamespaceAndPath("shamaschizm","ascent")))return;
  Identifier palette=trim.material().value().paletteId();
  for(var override:equipmentInfo.trimOverrides()) {
   if(!override.predicate().matches(trim))continue;
   if(override.textureId().isPresent())return;
   palette=override.paletteId().orElse(palette);break;
  }
  String path=palette.getPath();String suffix=path.substring(path.lastIndexOf('/')+1);
  cir.setReturnValue(AscentTrimTextures.handle(Identifier.fromNamespaceAndPath("shamaschizm",layerType.trimAssetPrefix()+"/ascent_"+suffix)));
 }
}
