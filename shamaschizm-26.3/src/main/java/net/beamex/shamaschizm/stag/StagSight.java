package net.beamex.shamaschizm.stag;
import java.util.List;
import java.util.ArrayList;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class StagSight {
    public record Entry(int id,boolean visible){}
    public record Report(List<Entry> entries) implements CustomPacketPayload{
        public static final Type<Report> TYPE=new Type<>(Shamaschizm.id("stag_sight"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Report> CODEC=new StreamCodec<>(){
            public Report decode(RegistryFriendlyByteBuf b){int n=b.readVarInt();if(n<0||n>128)throw new IllegalArgumentException("Invalid stag count");List<Entry> e=new ArrayList<>();for(int i=0;i<n;i++)e.add(new Entry(b.readVarInt(),b.readBoolean()));return new Report(e);}
            public void encode(RegistryFriendlyByteBuf b,Report p){b.writeVarInt(p.entries.size());for(Entry e:p.entries){b.writeVarInt(e.id);b.writeBoolean(e.visible);}}
        };
        @Override public Type<Report> type(){return TYPE;}
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){
        e.registrar("1").playToServer(Report.TYPE,Report.CODEC,(p,c)->c.enqueueWork(()->{
            var player=c.player();
            for(Entry entry:p.entries)if(player.level().getEntity(entry.id) instanceof StagEntity stag
                &&stag.distanceToSqr(player)<=160*160)stag.observe(player,entry.visible);
        }));
    }
}
