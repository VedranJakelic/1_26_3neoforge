package net.beamex.shamaschizm.localportal;
import java.util.*;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
/** Normal update tags only; bounded separately from the block palette. */
public record PortalBlockAppearance(int index,Identifier type,CompoundTag tag,float openness) {
    public static final int LIMIT=64,MAX_TAG_BYTES=8192;
    public static byte[] tagBytes(CompoundTag tag) {
        var buffer=new FriendlyByteBuf(Unpooled.buffer());
        try{buffer.writeNbt(tag);byte[] bytes=new byte[buffer.readableBytes()];buffer.readBytes(bytes);return bytes;}
        finally{buffer.release();}
    }
    static void write(RegistryFriendlyByteBuf b,List<PortalBlockAppearance> entries) {
        if(entries.size()>LIMIT)throw new IllegalArgumentException("Portal block entity count");
        b.writeVarInt(entries.size());
        for(var e:entries) {
            byte[] tag=tagBytes(e.tag);if(tag.length>MAX_TAG_BYTES)throw new IllegalArgumentException("Portal block entity tag size");
            b.writeVarInt(e.index);b.writeIdentifier(e.type);b.writeByteArray(tag);b.writeFloat(e.openness);
        }
    }
    static List<PortalBlockAppearance> read(RegistryFriendlyByteBuf b,int cells) {
        int count=b.readVarInt();if(count<0||count>LIMIT)throw new IllegalArgumentException("Portal block entity count");
        var entries=new ArrayList<PortalBlockAppearance>();var seen=new HashSet<Integer>();
        for(int i=0;i<count;i++) {
            int index=b.readVarInt();if(index<0||index>=cells||!seen.add(index))throw new IllegalArgumentException("Portal block entity index");
            var type=b.readIdentifier();var buffer=new FriendlyByteBuf(Unpooled.wrappedBuffer(b.readByteArray(MAX_TAG_BYTES)));
            CompoundTag tag;
            try{tag=buffer.readNbt();if(tag==null||buffer.isReadable())throw new IllegalArgumentException("Portal block entity tag");}
            finally{buffer.release();}
            float open=b.readFloat();if(!Float.isFinite(open)||open<0||open>1)throw new IllegalArgumentException("Portal lid state");
            entries.add(new PortalBlockAppearance(index,type,tag,open));
        }
        return List.copyOf(entries);
    }
}
