package net.beamex.shamaschizm.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.registry.ModDataComponents;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jspecify.annotations.Nullable;

/** Stack-sensitive client rendering for Ancient Blueprint-upgraded netherite armor. */
public final class AncientArmorClient {
    private static final Identifier OUTER_TEXTURE = Identifier.fromNamespaceAndPath(
            Shamaschizm.MOD_ID, "textures/models/armor/ancient_armor_layer_1.png");
    private static final Identifier LEGGINGS_TEXTURE = Identifier.fromNamespaceAndPath(
            Shamaschizm.MOD_ID, "textures/models/armor/ancient_armor_layer_2.png");

    private static AncientArmorModel helmet;
    private static AncientArmorModel chestplate;
    private static AncientArmorModel leggings;
    private static AncientArmorModel boots;
    private static AncientArmorModel babyHelmet;
    private static AncientArmorModel babyChestplate;
    private static AncientArmorModel babyLeggings;
    private static AncientArmorModel babyBoots;

    private AncientArmorClient() {}

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public Model getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
                if (!hasAncientAppearance(stack)) {
                    return original;
                }
                AncientArmorModel replacement = modelFor(stack, layerType);
                return replacement == null ? original : replacement;
            }

            @Override
            public @Nullable Identifier getArmorTexture(
                    ItemStack stack,
                    EquipmentClientInfo.LayerType layerType,
                    EquipmentClientInfo.Layer layer,
                    Identifier original) {
                if (!hasAncientAppearance(stack)
                        || modelFor(stack, layerType) == null) {
                    return null;
                }
                return stack.is(Items.NETHERITE_LEGGINGS)
                        ? LEGGINGS_TEXTURE
                        : OUTER_TEXTURE;
            }
        }, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);
    }

    /** Re-bakes on renderer/resource reload, so stale model parts are never retained. */
    public static void bakeModels(EntityModelSet models) {
        helmet = new AncientArmorModel(models.bakeLayer(AncientArmorModel.HELMET_LAYER),EquipmentSlot.HEAD,false);
        chestplate = new AncientArmorModel(models.bakeLayer(AncientArmorModel.CHESTPLATE_LAYER),EquipmentSlot.CHEST,false);
        leggings = new AncientArmorModel(models.bakeLayer(AncientArmorModel.LEGGINGS_LAYER),EquipmentSlot.LEGS,false);
        boots = new AncientArmorModel(models.bakeLayer(AncientArmorModel.BOOTS_LAYER),EquipmentSlot.FEET,false);
        babyHelmet = new AncientArmorModel(models.bakeLayer(AncientArmorModel.BABY_HELMET_LAYER),EquipmentSlot.HEAD,true);
        babyChestplate = new AncientArmorModel(models.bakeLayer(AncientArmorModel.BABY_CHESTPLATE_LAYER),EquipmentSlot.CHEST,true);
        babyLeggings = new AncientArmorModel(models.bakeLayer(AncientArmorModel.BABY_LEGGINGS_LAYER),EquipmentSlot.LEGS,true);
        babyBoots = new AncientArmorModel(models.bakeLayer(AncientArmorModel.BABY_BOOTS_LAYER),EquipmentSlot.FEET,true);
    }

    private static boolean hasAncientAppearance(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ANCIENT_APPEARANCE, false);
    }

    private static @Nullable AncientArmorModel modelFor(
            ItemStack stack, EquipmentClientInfo.LayerType layerType) {
        boolean baby = layerType == EquipmentClientInfo.LayerType.HUMANOID_BABY;
        if (stack.is(Items.NETHERITE_HELMET)) return baby ? babyHelmet : helmet;
        if (stack.is(Items.NETHERITE_CHESTPLATE)) return baby ? babyChestplate : chestplate;
        if (stack.is(Items.NETHERITE_LEGGINGS)) return baby ? babyLeggings : leggings;
        if (stack.is(Items.NETHERITE_BOOTS)) return baby ? babyBoots : boots;
        return null;
    }
}
