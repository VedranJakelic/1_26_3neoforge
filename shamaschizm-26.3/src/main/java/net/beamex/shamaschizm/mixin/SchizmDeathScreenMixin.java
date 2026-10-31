package net.beamex.shamaschizm.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DeathScreen.class)
public abstract class SchizmDeathScreenMixin {
    @ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"))
    private static MutableComponent shamaschizm$consumedTitle(MutableComponent original,
            @Local(argsOnly = true) LocalPlayer player) {
        // Use the screen's player, never Minecraft.level (which portal rendering can swap).
        return player.level().dimension().equals(Schizm.KEY) ? Component.literal("Consumed.") : original;
    }
}
