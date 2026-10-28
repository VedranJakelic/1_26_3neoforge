package net.beamex.shamaschizm.localportal;

import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/** A bounded, independent block/light snapshot, never installed into the main client world. */
public record PortalTerrain(Doorway source, Doorway target, BlockPos min, int sizeX, int sizeY, int sizeZ,
                            int[] states, byte[] lights, java.util.List<PortalBlockAppearance> blockEntities) implements CustomPacketPayload {
    public PortalTerrain(Doorway source,Doorway target,BlockPos min,int x,int y,int z,int[] states,byte[] lights) {
        this(source,target,min,x,y,z,states,lights,java.util.List.of());
    }
    public static final int MAX_CELLS = 40000;
    public static final Type<PortalTerrain> TYPE = new Type<>(Shamaschizm.id("portal_terrain"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PortalTerrain> CODEC = new StreamCodec<>() {
        public PortalTerrain decode(RegistryFriendlyByteBuf b) {
            Doorway source = readDoor(b), target = readDoor(b);
            BlockPos min = b.readBlockPos();
            int x=b.readVarInt(), y=b.readVarInt(), z=b.readVarInt();
            if (x<1 || y<1 || z<1 || x>48 || y>48 || z>48 || (long)x*y*z>MAX_CELLS)
                throw new IllegalArgumentException("Invalid portal terrain size");
            int count=x*y*z;
            int[] states=new int[count]; byte[] lights=new byte[count];
            int paletteSize=b.readVarInt();
            if (paletteSize<1 || paletteSize>count) throw new IllegalArgumentException("Invalid palette");
            int[] palette=new int[paletteSize]; for(int i=0;i<paletteSize;i++) palette[i]=b.readVarInt();
            for(int i=0;i<count;i++) {
                int index=b.readVarInt();
                if(index<0 || index>=paletteSize) throw new IllegalArgumentException("Invalid palette index");
                states[i]=palette[index];
            }
            b.readBytes(lights);
            var appearances=PortalBlockAppearance.read(b,count);
            return new PortalTerrain(source,target,min,x,y,z,states,lights,appearances);
        }
        public void encode(RegistryFriendlyByteBuf b, PortalTerrain p) {
            writeDoor(b,p.source);writeDoor(b,p.target);b.writeBlockPos(p.min);
            b.writeVarInt(p.sizeX);b.writeVarInt(p.sizeY);b.writeVarInt(p.sizeZ);
            var palette=new java.util.LinkedHashMap<Integer,Integer>();
            for(int state:p.states) palette.computeIfAbsent(state,k->palette.size());
            b.writeVarInt(palette.size());for(int state:palette.keySet()) b.writeVarInt(state);
            for(int state:p.states)b.writeVarInt(palette.get(state));b.writeBytes(p.lights);
            PortalBlockAppearance.write(b,p.blockEntities);
        }
    };
    public Type<PortalTerrain> type(){return TYPE;}
    public int index(int x,int y,int z){return (y*sizeZ+z)*sizeX+x;}
    public int index(BlockPos p){
        int x=p.getX()-min.getX(),y=p.getY()-min.getY(),z=p.getZ()-min.getZ();
        return x<0||y<0||z<0||x>=sizeX||y>=sizeY||z>=sizeZ ? -1 : index(x,y,z);
    }
    public static void writeDoor(RegistryFriendlyByteBuf b,Doorway d){
        b.writeUtf(d.name(),48);b.writeDouble(d.origin().x);b.writeDouble(d.origin().y);b.writeDouble(d.origin().z);
        b.writeEnum(d.facing());b.writeDouble(d.width());b.writeDouble(d.height());b.writeUtf(d.target(),48);b.writeBoolean(d.enabled());
    }
    public static Doorway readDoor(RegistryFriendlyByteBuf b){
        return new Doorway(b.readUtf(48),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readEnum(Doorway.Facing.class),
                b.readDouble(),b.readDouble(),b.readUtf(48),b.readBoolean());
    }
}
