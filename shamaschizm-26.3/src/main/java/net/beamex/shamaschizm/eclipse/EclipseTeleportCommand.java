package net.beamex.shamaschizm.eclipse;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Operator travel aid; does not advance the world clock or alter the eclipse. */
@EventBusSubscriber(modid="shamaschizm")
public final class EclipseTeleportCommand {
    private EclipseTeleportCommand() {}
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("schizm_eclipse")
                .then(Commands.literal("tp")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> teleport(context.getSource()))));
    }
    private static int teleport(CommandSourceStack source) throws CommandSyntaxException {
        var player=source.getPlayerOrException();
        var level=source.getServer().overworld();
        long now=level.getOverworldClockTime();
        var alignment=EclipseOrbit.next(now);
        boolean active=alignment.active(now);
        var shadow=EclipseOrbit.shadow(active?now:alignment.peak());
        if(!shadow.aboveHorizon()) {
            source.sendFailure(Component.literal("The eclipse shadow is below the horizon."));return 0;
        }
        int x=(int)Math.floor(shadow.x()),z=(int)Math.floor(shadow.z());
        if(!level.getWorldBorder().isWithinBounds(new BlockPos(x,64,z))) {
            source.sendFailure(Component.literal("The eclipse centre is outside the Overworld border."));return 0;
        }
        // Load only the destination column, and land above its highest blocking surface.
        level.getChunkAt(new BlockPos(x,64,z));
        int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
        var destination=new Vec3(shadow.x(),y+0.1,shadow.z());
        var bounds=player.getBoundingBox().move(destination.subtract(player.position()));
        if(y<=level.getMinY() || y+player.getBbHeight()>=level.getMaxY()
                || level.getFluidState(new BlockPos(x,y-1,z)).is(FluidTags.LAVA)
                || !level.noCollision(bounds)) {
            source.sendFailure(Component.literal("No clear surface at the eclipse centre. Teleport cancelled."));return 0;
        }
        if(!player.teleportTo(level,destination.x,destination.y,destination.z,java.util.Set.of(),
                player.getYRot(),player.getXRot(),true))return 0;
        player.fallDistance=0;
        source.sendSuccess(()->Component.literal(String.format(java.util.Locale.ROOT,
                "Teleported to %s eclipse centre: X %.2f Y %.2f Z %.2f. %s",
                active?"the current":"the next",destination.x,destination.y,destination.z,
                active?"The shadow continues moving; run this command again to catch up.":
                        "Conjunction tick: "+alignment.peak()+". Time has not been changed.")),false);
        return 1;
    }
}
