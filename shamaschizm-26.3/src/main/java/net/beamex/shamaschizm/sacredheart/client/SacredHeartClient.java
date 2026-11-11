package net.beamex.shamaschizm.sacredheart.client;

import com.mojang.serialization.MapCodec;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class SacredHeartClient {
    private SacredHeartClient() {}
    @SubscribeEvent
    public static void properties(RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(Shamaschizm.id("sacred_heartbeat"), Heartbeat.MAP_CODEC);
    }

    public record Heartbeat() implements RangeSelectItemModelProperty {
        public static final MapCodec<Heartbeat> MAP_CODEC = MapCodec.unit(new Heartbeat());
        @Override
        public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
            var mc = Minecraft.getInstance();
            if (level == null) level = mc.level;
            if (level == null) return 0F;
            // Bounded world time avoids precision loss and keeps portal render passes in phase.
            float ticks = Math.floorMod(level.getGameTime(), 20L)
                    + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            return HeartbeatMotion.amount(ticks);
        }
        @Override
        public MapCodec<? extends RangeSelectItemModelProperty> type() { return MAP_CODEC; }
    }
}
