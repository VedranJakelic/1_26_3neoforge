package net.beamex.shamaschizm.localportal;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Locale;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = "shamaschizm")
public final class DoorwayCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        var root = Commands.literal("schizmportal")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> help(c.getSource()))
                .then(Commands.literal("help").executes(c -> help(c.getSource())))
                .then(Commands.literal("list").executes(c -> list(c.getSource())))
                .then(Commands.literal("light").then(Commands.argument("name", StringArgumentType.word())
                        .executes(c -> inSchizm(c.getSource()) ? message(c.getSource(), PortalLight.describe(
                                c.getSource().getLevel(), StringArgumentType.getString(c,"name"))) : 0)))
                .then(Commands.literal("set").then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("position", Vec3Argument.vec3(false))
                        .then(Commands.argument("facing", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"north", "east", "south", "west"}, b))
                        .then(Commands.argument("width", DoubleArgumentType.doubleArg(1, 8))
                        .then(Commands.argument("height", DoubleArgumentType.doubleArg(2, 8))
                        .executes(DoorwayCommands::set)))))))
                .then(Commands.literal("link").then(Commands.argument("first", StringArgumentType.word())
                        .then(Commands.argument("second", StringArgumentType.word()).executes(DoorwayCommands::link))))
                .then(Commands.literal("enable").then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(DoorwayCommands::enable))))
                .then(Commands.literal("remove").then(Commands.argument("name", StringArgumentType.word())
                        .executes(DoorwayCommands::remove)));
        event.getDispatcher().register(root);
    }

    private static boolean inSchizm(CommandSourceStack source) {
        if (source.getLevel().dimension().equals(Schizm.KEY)) return true;
        source.sendFailure(Component.literal("These doorways can only be edited inside the Schizm."));
        return false;
    }

    private static int message(CommandSourceStack s, String text) {
        s.sendSuccess(() -> Component.literal(text), false); return 1;
    }

    private static int error(CommandSourceStack s, String text) {
        s.sendFailure(Component.literal(text)); return 0;
    }

    private static int help(CommandSourceStack s) {
        return message(s, "Experimental local doorways with a 15-block terrain view.\n"
                + "/schizmportal set <name> <x y z> <north|east|south|west> <width> <height>\n"
                + "Position snaps to the containing block: X/Z centre, Y bottom. Facing points toward the approach room.\n"
                + "/schizmportal link <first> <second> - Link equal-sized doorways, initially disabled.\n"
                + "/schizmportal enable <name> <true|false> - Change both ends.\n"
                + "/schizmportal list | remove <name>\n"
                + "/schizmportal light <name> - Inspect light sent through this end.\n"
                + "Outlines: amber = disabled/unlinked; cyan = active. Allow a few seconds for the terrain view to load. Clear both openings before testing.");
    }

    private static int set(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        var s = c.getSource(); if (!inSchizm(s)) return 0;
        String name = StringArgumentType.getString(c, "name");
        if (!name.matches("[a-zA-Z0-9_-]{1,48}")) return error(s, "Use 1-48 letters, digits, underscores or hyphens for the name.");
        Doorway.Facing facing;
        try { facing = Doorway.Facing.valueOf(StringArgumentType.getString(c, "facing").toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return error(s, "Facing must be north, east, south or west."); }
        Vec3 p = Vec3Argument.getVec3(c, "position");
        double w = DoubleArgumentType.getDouble(c, "width"), h = DoubleArgumentType.getDouble(c, "height");
        Doorway next = new Doorway(name, p, facing, w, h, "", false).blockAligned();
        p = next.origin();
        for (double x : new double[]{-w / 2, w / 2}) {
            var corner = net.minecraft.core.BlockPos.containing(next.world(x, 0, 0));
            if (!s.getLevel().getWorldBorder().isWithinBounds(corner)) return error(s, "Doorway exceeds the world border.");
        }
        if (p.y < s.getLevel().getMinY() || p.y + h > s.getLevel().getMaxY()) return error(s, "Doorway exceeds the world's height limits.");
        var data = DoorwayData.get(s.getLevel());
        if (data.get(name) == null && data.all().size() >= Doorway.MAX_DOORWAYS) return error(s, "Prototype limit: 128 doorways.");
        data.unlink(name); data.put(next);
        return message(s, "Saved doorway " + name + " at " + next.origin() + ". It is disabled and unlinked.");
    }

    private static int link(CommandContext<CommandSourceStack> c) {
        var s = c.getSource(); if (!inSchizm(s)) return 0;
        var data = DoorwayData.get(s.getLevel());
        String a = StringArgumentType.getString(c, "first"), b = StringArgumentType.getString(c, "second");
        Doorway first = data.get(a), second = data.get(b);
        if (first == null || second == null || a.equals(b)) return error(s, "Specify two different existing doorways.");
        if (first.width() != second.width() || first.height() != second.height()) return error(s, "Doorways must have the same width and height; this system does not scale players.");
        if (first.origin().distanceToSqr(second.origin()) < 4) return error(s, "Place the two ends at least 2 blocks apart.");
        data.link(a, b);
        return message(s, "Linked " + a + " and " + b + ". Use /schizmportal enable " + a + " true when both openings are clear.");
    }

    private static int enable(CommandContext<CommandSourceStack> c) {
        var s = c.getSource(); if (!inSchizm(s)) return 0;
        var data = DoorwayData.get(s.getLevel());
        String name = StringArgumentType.getString(c, "name");
        Doorway a = data.get(name), b = a == null ? null : data.get(a.target());
        if (b == null || !b.target().equals(name)) return error(s, "This doorway has no reciprocal link.");
        boolean enabled = BoolArgumentType.getBool(c, "enabled");
        data.enable(name, enabled);
        return message(s, "Doorway pair " + (enabled ? "enabled." : "disabled."));
    }

    private static int remove(CommandContext<CommandSourceStack> c) {
        var s = c.getSource(); if (!inSchizm(s)) return 0;
        var data = DoorwayData.get(s.getLevel()); String name = StringArgumentType.getString(c, "name");
        if (data.get(name) == null) return error(s, "Unknown doorway.");
        data.remove(name); return message(s, "Removed " + name + " and unlinked its partner.");
    }

    private static int list(CommandSourceStack s) {
        if (!inSchizm(s)) return 0;
        var all = DoorwayData.get(s.getLevel()).all();
        if (all.isEmpty()) return message(s, "No experimental doorways have been created.");
        for (Doorway d : all) message(s, d.name() + " at " + d.origin() + " " + d.facing()
                + " " + d.width() + "x" + d.height() + " -> " + d.target() + " (" + (d.enabled() ? "enabled" : "disabled") + ")");
        return all.size();
    }
}
