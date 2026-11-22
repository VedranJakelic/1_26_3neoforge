package net.beamex.shamaschizm.eclipse;
import net.minecraft.client.Minecraft;
public final class EclipseClient {
    private EclipseClient() {}
    public static boolean muteMusic() {
        var mc=Minecraft.getInstance();
        return mc.level!=null && mc.player!=null && Eclipses.active(mc.level,mc.player.blockPosition());
    }
}
