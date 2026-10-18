package net.beamex.shamaschizm.elf;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class ElfNetwork {
    // Client state only; avoids referring to client classes in common packet registration.
    public static int lockedElf=-1;
    public record Lock(int id) implements CustomPacketPayload {
        public static final Type<Lock> TYPE=new Type<>(Shamaschizm.id("elf_gaze"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Lock> CODEC=new StreamCodec<>(){
            public Lock decode(RegistryFriendlyByteBuf b){return new Lock(b.readInt());}
            public void encode(RegistryFriendlyByteBuf b,Lock p){b.writeInt(p.id());}
        };
        @Override public Type<Lock> type(){return TYPE;}
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){
        event.registrar("1").playToClient(Lock.TYPE,Lock.CODEC,(p,c)->lockedElf=p.id());
    }
}
