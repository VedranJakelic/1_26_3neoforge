package net.beamex.shamaschizm.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.SoulData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Commands use the same persistent balance as trading, death and regeneration. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class SoulCommand {
    private SoulCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("soul")
                .executes(c -> show(c.getSource(), c.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> show(c.getSource(), EntityArgument.getPlayer(c, "player"))))
                .then(Commands.literal("get")
                        .executes(c -> show(c.getSource(), c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> show(c.getSource(), EntityArgument.getPlayer(c, "player")))))
                .then(change("add"))
                .then(change("remove"))
                .then(change("set")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> change(String operation) {
        return Commands.literal(operation)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(operation.equals("set") ? 0 : 1))
                                .executes(c -> change(c.getSource(), EntityArgument.getPlayer(c, "player"),
                                        operation, IntegerArgumentType.getInteger(c, "amount")))));
    }

    private static int show(CommandSourceStack source, ServerPlayer player) {
        int souls = SoulData.getSouls(player);
        source.sendSuccess(() -> Component.literal(player.getName().getString()
                + " has " + souls + " soul(s)."), false);
        return souls;
    }

    private static int change(CommandSourceStack source, ServerPlayer player, String operation, int amount) {
        int before = SoulData.getSouls(player);
        long result = switch (operation) {
            case "add" -> (long) before + amount;
            case "remove" -> Math.max(0L, (long) before - amount);
            default -> amount;
        };
        if (result > Integer.MAX_VALUE) {
            source.sendFailure(Component.literal("Soul balance cannot exceed " + Integer.MAX_VALUE + "."));
            return 0;
        }
        int after = (int) result;
        SoulData.setSouls(player, after);
        source.sendSuccess(() -> Component.literal(player.getName().getString()
                + ": " + before + " -> " + after + " soul(s)."), true);
        return 1;
    }
}
