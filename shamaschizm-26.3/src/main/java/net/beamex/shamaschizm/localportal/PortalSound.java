package net.beamex.shamaschizm.localportal;

import java.util.UUID;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public record PortalSound(String source,String target,Identifier sound,SoundSource category,Vec3 position,
                          float volume,float pitch,float range,long seed,int entityId,UUID entityUuid) implements CustomPacketPayload {
    public PortalSound {
        if(source.length()>48||target.length()>48||!PortalSoundPath.finite(position)
            ||!Float.isFinite(volume)||volume<0||volume>16||!Float.isFinite(pitch)||pitch<=0||pitch>4
            ||!Float.isFinite(range)||range<=0||range>64||entityId< -1||((entityId>=0)!=(entityUuid!=null)))
            throw new IllegalArgumentException("Invalid portal sound");
    }
    public static final Type<PortalSound> TYPE=new Type<>(Shamaschizm.id("portal_sound"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PortalSound> CODEC=new StreamCodec<>(){
        public PortalSound decode(RegistryFriendlyByteBuf b){
            String source=b.readUtf(48),target=b.readUtf(48);Identifier sound=b.readIdentifier();SoundSource category=b.readEnum(SoundSource.class);
            Vec3 pos=new Vec3(b.readDouble(),b.readDouble(),b.readDouble());
            float volume=b.readFloat(),pitch=b.readFloat(),range=b.readFloat();long seed=b.readLong();int id=b.readVarInt();
            return new PortalSound(source,target,sound,category,pos,volume,pitch,range,seed,id,id>=0?b.readUUID():null);
        }
        public void encode(RegistryFriendlyByteBuf b,PortalSound p){
            b.writeUtf(p.source,48);b.writeUtf(p.target,48);b.writeIdentifier(p.sound);b.writeEnum(p.category);
            b.writeDouble(p.position.x);b.writeDouble(p.position.y);b.writeDouble(p.position.z);
            b.writeFloat(p.volume);b.writeFloat(p.pitch);b.writeFloat(p.range);b.writeLong(p.seed);b.writeVarInt(p.entityId);
            if(p.entityId>=0)b.writeUUID(p.entityUuid);
        }
    };
    public Type<PortalSound> type(){return TYPE;}
}
