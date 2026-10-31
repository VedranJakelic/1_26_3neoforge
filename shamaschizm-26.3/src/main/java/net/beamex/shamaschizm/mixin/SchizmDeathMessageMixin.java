package net.beamex.shamaschizm.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Replaces the one vanilla message, retaining gamerule and team visibility handling. */
@Mixin(ServerPlayer.class)
public abstract class SchizmDeathMessageMixin {
    @ModifyExpressionValue(method = "die", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/damagesource/CombatTracker;getDeathMessage()Lnet/minecraft/network/chat/Component;"))
    private Component shamaschizm$consumedMessage(Component original) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!player.level().dimension().equals(Schizm.KEY)) return original;
        Component name = player.getDisplayName();
        // Choose once: the death packet and every eligible chat recipient use the same message.
        return switch (player.getRandom().nextInt(5)) {
            case 0 -> Component.empty().append(name).append(" was consumed by the Schizm. Their valor will be forgotten.");
            case 1 -> Component.empty().append(name).append(" was consumed by the Schizm. Their efforts will not bear fruit.");
            case 2 -> Component.literal("Death has extended her hand to ").append(name).append(". Their union was unceremonious.");
            case 3 -> Component.empty().append(name).append(" was unwise.");
            default -> Component.empty().append(name).append(" delved too deep. They have become one with the maggots.");
        };
    }
}
