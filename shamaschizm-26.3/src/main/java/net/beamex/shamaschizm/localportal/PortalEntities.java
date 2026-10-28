package net.beamex.shamaschizm.localportal;

import java.util.*;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Complete, bounded visual snapshots; never registered as real client-world entities. */
public record PortalEntities(Doorway source,Doorway target,List<Entry> entries) implements CustomPacketPayload {
    public static final int LIMIT=48;
    public static final Type<PortalEntities> TYPE=new Type<>(Shamaschizm.id("portal_entities"));
    public record Entry(int id,UUID uuid,EntityType<?> type,String playerName,Vec3 position,
                        float yaw,float pitch,float bodyYaw,float headYaw,int age,int light,
                        int hurt,int death,float speed,int vehicleId,ClientboundSetEntityDataPacket data,List<ItemStack> equipment) {}
    public static final StreamCodec<RegistryFriendlyByteBuf,PortalEntities> CODEC=new StreamCodec<>() {
        public PortalEntities decode(RegistryFriendlyByteBuf b) {
            var source=PortalTerrain.readDoor(b);var target=PortalTerrain.readDoor(b);
            int count=b.readVarInt();if(count<0||count>LIMIT)throw new IllegalArgumentException("Portal entity count");
            var entries=new ArrayList<Entry>(count);var ids=new HashSet<Integer>();
            for(int i=0;i<count;i++) {
                int id=b.readVarInt();if(!ids.add(id))throw new IllegalArgumentException("Duplicate portal entity id");UUID uuid=b.readUUID();var type=EntityType.STREAM_CODEC.decode(b);String name=b.readUtf(64);
                Vec3 pos=new Vec3(b.readDouble(),b.readDouble(),b.readDouble());
                float yaw=b.readFloat(),pitch=b.readFloat(),body=b.readFloat(),head=b.readFloat();
                if(!Double.isFinite(pos.x)||!Double.isFinite(pos.y)||!Double.isFinite(pos.z)
                        ||!Float.isFinite(yaw)||!Float.isFinite(pitch)||!Float.isFinite(body)||!Float.isFinite(head))throw new IllegalArgumentException("Portal entity pose");
                int age=b.readVarInt(),light=b.readInt(),hurt=b.readVarInt(),death=b.readVarInt();float speed=b.readFloat();
                if(!Float.isFinite(speed)||speed<0)throw new IllegalArgumentException("Portal entity speed");
                int vehicleId=b.readVarInt();
                if(vehicleId< -1||vehicleId==id)throw new IllegalArgumentException("Portal vehicle id");
                var data=ClientboundSetEntityDataPacket.STREAM_CODEC.decode(b);
                if(data.id()!=id)throw new IllegalArgumentException("Portal entity metadata id");
                var equipment=new ArrayList<ItemStack>();
                for(var slot:EquipmentSlot.values())equipment.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(b));
                entries.add(new Entry(id,uuid,type,name,pos,yaw,pitch,body,head,age,light,hurt,death,speed,vehicleId,data,List.copyOf(equipment)));
            }
            return new PortalEntities(source,target,List.copyOf(entries));
        }
        public void encode(RegistryFriendlyByteBuf b,PortalEntities p) {
            if(p.entries.size()>LIMIT)throw new IllegalArgumentException("Portal entity count");
            PortalTerrain.writeDoor(b,p.source);PortalTerrain.writeDoor(b,p.target);b.writeVarInt(p.entries.size());
            for(var e:p.entries) {
                if(e.equipment.size()!=EquipmentSlot.values().length)throw new IllegalArgumentException("Portal equipment slots");
                b.writeVarInt(e.id);b.writeUUID(e.uuid);EntityType.STREAM_CODEC.encode(b,e.type);b.writeUtf(e.playerName,64);
                b.writeDouble(e.position.x);b.writeDouble(e.position.y);b.writeDouble(e.position.z);
                b.writeFloat(e.yaw);b.writeFloat(e.pitch);b.writeFloat(e.bodyYaw);b.writeFloat(e.headYaw);
                b.writeVarInt(e.age);b.writeInt(e.light);b.writeVarInt(e.hurt);b.writeVarInt(e.death);b.writeFloat(e.speed);b.writeVarInt(e.vehicleId);
                ClientboundSetEntityDataPacket.STREAM_CODEC.encode(b,e.data);
                for(var item:e.equipment)ItemStack.OPTIONAL_STREAM_CODEC.encode(b,item);
            }
        }
    };
    public Type<PortalEntities> type(){return TYPE;}
}
