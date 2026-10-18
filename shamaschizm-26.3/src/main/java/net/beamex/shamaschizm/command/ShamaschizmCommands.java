package net.beamex.shamaschizm.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.experimental.ExperimentalSettings;
import net.beamex.shamaschizm.world.dungeon.SchizmDungeonGenerator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class ShamaschizmCommands {
    private ShamaschizmCommands(){}
    @SubscribeEvent public static void register(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("shamaschizm")
                .executes(c->help(c.getSource()))
                .then(Commands.literal("help").executes(c->help(c.getSource())))
                .then(Commands.literal("experimental").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(c->status(c.getSource()))
                        .then(Commands.literal("arena2").executes(c->status(c.getSource()))
                                .then(Commands.argument("enabled",BoolArgumentType.bool()).executes(c->set(c.getSource(),BoolArgumentType.getBool(c,"enabled")))))));
    }
    private static int help(CommandSourceStack source){
        source.sendSuccess(()->Component.literal("Shamaschizm commands:\n/shamaschizm help - Show this command overview.\n/soul [get] [player] - Show your or an online player's soul balance.\n/soul add <player> <amount> - Add souls (operator).\n/soul remove <player> <amount> - Remove souls, stopping at zero (operator).\n/soul set <player> <amount> - Set a soul balance (operator).\n/checkgarden - Check whether your current Schizm dungeon has a recorded garden.\n/shamaschizm experimental - Show experimental settings (operator).\n/shamaschizm experimental arena2 <true|false> - Enable/disable entrance2 -> arena2 -> garden1 for newly generated dungeons (operator). Existing dungeons are unchanged."),false);
        // Inspect the final dispatcher at execution time, after all legacy handlers
        // have registered. This includes separate roots as well as merged subcommands.
        var dispatcher = source.getServer().getCommands().getDispatcher();
        for (var root : dispatcher.getRoot().getChildren()) {
            if (!root.getName().equals("shamaschizm") && !belongsToMod(root)) continue;
            if (!root.canUse(source)) continue;
            for (String usage : dispatcher.getAllUsage(root, source, true)) {
                String line = "/" + root.getName() + (usage.isEmpty() ? "" : " " + usage);
                source.sendSuccess(() -> Component.literal(line), false);
            }
        }
        return 1;
    }
    private static boolean belongsToMod(com.mojang.brigadier.tree.CommandNode<CommandSourceStack> node) {
        if (node.getCommand() != null && node.getCommand().getClass().getName()
                .startsWith("net.beamex.shamaschizm.")) return true;
        for (var child : node.getChildren()) if (belongsToMod(child)) return true;
        return false;
    }
    private static int status(CommandSourceStack source){
        boolean enabled=ExperimentalSettings.get(source.getLevel()).arenaGarden();
        source.sendSuccess(()->Component.literal("Experimental arena2 garden route: "+(enabled?"enabled":"disabled")+". Applies to newly generated dungeons only."),false);return 1;
    }
    private static int set(CommandSourceStack source,boolean enabled){
        if(enabled){
            String error=SchizmDungeonGenerator.validateArenaRoute(source.getLevel());
            if(error!=null){source.sendFailure(Component.literal("Cannot enable arena2 route: "+error));return 0;}
        }
        ExperimentalSettings.get(source.getLevel()).setArenaGarden(enabled);
        source.sendSuccess(()->Component.literal("Experimental arena2 garden route "+(enabled?"enabled":"disabled")+" for future dungeons. Existing arena door locks remain in effect."),true);return 1;
    }
}
