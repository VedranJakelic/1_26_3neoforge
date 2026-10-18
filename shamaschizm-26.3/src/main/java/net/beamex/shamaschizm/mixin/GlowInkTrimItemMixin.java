package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.ascent.AscentGlow;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.TrimMaterialProperty;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reuse the vanilla quartz icon for this custom material, without changing the
 * saved trim, its worn appearance, or its fluorescent light behavior. */
@Mixin(TrimMaterialProperty.class)
public abstract class GlowInkTrimItemMixin {
    private static final ResourceKey<TrimMaterial> SHAMASCHIZM_QUARTZ_ICON =
            ResourceKey.create(Registries.TRIM_MATERIAL, Identifier.withDefaultNamespace("quartz"));

    @Inject(method = "get(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/entity/LivingEntity;ILnet/minecraft/world/item/ItemDisplayContext;)Lnet/minecraft/resources/ResourceKey;",
            at = @At("HEAD"), cancellable = true)
    private void shamaschizm$glowInkIcon(ItemStack stack, ClientLevel level,
            LivingEntity owner, int seed, ItemDisplayContext context,
            CallbackInfoReturnable<ResourceKey<TrimMaterial>> cir) {
        ArmorTrim trim = stack.get(DataComponents.TRIM);
        if (trim != null && trim.material().is(AscentGlow.MATERIAL)) {
            cir.setReturnValue(SHAMASCHIZM_QUARTZ_ICON);
        }
    }
}
