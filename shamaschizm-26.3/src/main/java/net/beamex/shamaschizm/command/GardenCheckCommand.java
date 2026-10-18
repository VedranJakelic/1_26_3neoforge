package net.beamex.shamaschizm.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.garden.GardenData;
import net.beamex.shamaschizm.world.Schizm;
import net.beamex.shamaschizm.world.PortalLinks;
import net.beamex.shamaschizm.world.dungeon.SchizmDungeonGenerator;
import net.minecraft.core.BlockPos;
import net.beamex.shamaschizm.world.dungeon.GardenZoneEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Player command for checking the persistent garden index of the current Schizm. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class GardenCheckCommand {
    private GardenCheckCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("checkgarden")
                .executes(context -> check(context.getSource())));
    }

    private static int check(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        ServerLevel level = player.level();
        if (!level.dimension().equals(Schizm.KEY)) {
            source.sendFailure(Component.literal(
                    "This command can only check the Schizm occupied by the player."));
            return 0;
        }

        PortalLinks links = PortalLinks.get(level);
        // Saved doorway is a safe arrival tile inside the spawn room, so allow
        // one template width beyond the generator's reservation radius.
        int radius = SchizmDungeonGenerator.RESERVATION_RADIUS
                + SchizmDungeonGenerator.MAX_ROOM_WIDTH;
        PortalLinks.Link dungeon = links.dungeonAt(player.blockPosition(), radius);
        if (dungeon == null) {
            source.sendFailure(Component.literal(
                    "Cannot identify one dungeon at your position. Move closer to its entrance and retry."));
            return 0;
        }
        GardenData gardens = GardenData.get(level);
        // Immediately migrate any old-format garden zones which are loaded now.
        for (var entity : level.getAllEntities()) {
            if (entity instanceof GardenZoneEntity zone) gardens.add(zone.structureBox());
        }

        int count = 0;
        for (var box : gardens.rooms) {
            BlockPos center = new BlockPos(box.minX() + (box.maxX() - box.minX()) / 2,
                    box.minY(), box.minZ() + (box.maxZ() - box.minZ()) / 2);
            if (dungeon.equals(links.dungeonAt(center, radius))) count++;
        }
        if (count == 0) {
            source.sendSuccess(() -> Component.literal(
                    "No garden1 is recorded for your current dungeon. Older unloaded gardens may not yet be indexed."), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "A generated garden1 is recorded for your current dungeon."), false);
        return count;
    }
}
