package net.beamex.shamaschizm.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.stew.PoppyStewNetwork;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** Client visual state. No MobEffect or inventory icon is involved. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class PoppyStewVisuals {
    private static int blurTicks, eyelidTicks;
    private static Object lastWorld, lastPlayer;
    private PoppyStewVisuals() {}

    @SubscribeEvent
    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(PoppyStewNetwork.State.TYPE,
                (packet, context) -> context.enqueueWork(() -> receive(packet)));
    }

    private static void receive(PoppyStewNetwork.State packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (blurTicks == 0 && packet.blurTicks() > 0)
            Shamaschizm.LOGGER.info("Poppy stew client received blur start: {} ticks", packet.blurTicks());
        if (eyelidTicks == 0 && packet.eyelidTicks() > 0)
            Shamaschizm.LOGGER.info("Poppy stew client received eyelid start: {} ticks", packet.eyelidTicks());
        lastPlayer = mc.player;
        lastWorld = mc.level;
        blurTicks = Math.min(1200, Math.max(0, packet.blurTicks()));
        eyelidTicks = Math.min(90, Math.max(0, packet.eyelidTicks()));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != lastWorld || mc.player != lastPlayer) {
            lastWorld = mc.level;
            lastPlayer = mc.player;
            blurTicks = eyelidTicks = 0;
        }
        if (mc.player == null || mc.level == null || !mc.player.isAlive()) {
            blurTicks = eyelidTicks = 0;
            return;
        }
        if (mc.isPaused()) return;
        if (blurTicks > 0) blurTicks--;
        if (eyelidTicks > 0) eyelidTicks--;
    }

    public static int blurTicks() { return blurTicks; }
    public static boolean blurActive() { return blurTicks > 0; }
    public static int eyelidTicks() { return eyelidTicks; }
}
