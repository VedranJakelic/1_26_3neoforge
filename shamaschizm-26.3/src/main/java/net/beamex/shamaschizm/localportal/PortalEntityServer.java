package net.beamex.shamaschizm.localportal;

import java.util.*;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="shamaschizm")
public final class PortalEntityServer {
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer player)||!player.level().dimension().equals(Schizm.KEY)
                ||player.tickCount%4!=0)return;
        var level=player.level();var data=DoorwayData.get(level);
        var selected=data.all().stream().filter(d->d.enabled()&&d.distance(player.getEyePosition())>-.5
                &&d.origin().distanceToSqr(player.position())<24*24)
                .sorted(Comparator.comparingDouble(d->d.origin().distanceToSqr(player.position()))).limit(2).toList();
        for(var source:selected) {
            var target=data.get(source.target());if(target==null||!target.enabled()||!target.target().equals(source.name()))continue;
            double side=target.width()/2+16;
            var a=target.world(-side,-16,-2);var b=target.world(side,target.height()+16,17);
            var box=new AABB(Math.min(a.x,b.x),Math.min(a.y,b.y),Math.min(a.z,b.z),Math.max(a.x,b.x),Math.max(a.y,b.y),Math.max(a.z,b.z));
            var entities=level.getEntities((Entity)null,box,e->(e instanceof LivingEntity||e instanceof ItemEntity||e instanceof net.minecraft.world.entity.projectile.Projectile||e instanceof net.minecraft.world.entity.vehicle.VehicleEntity)
                    &&!e.isRemoved()&&!e.isSpectator()&&!e.isInvisibleTo(player));
            entities.sort(Comparator.comparingDouble(e->e.position().distanceToSqr(target.origin())));
            var entries=new ArrayList<PortalEntities.Entry>();
            for(var e:entities) {
                double depth=target.distance(e.position());
                if(depth+e.getBbWidth()<0||depth-e.getBbWidth()>Doorway.VIEW_DEPTH)continue;
                var living=e instanceof LivingEntity l?l:null;
                var metadata=e.getEntityData().getNonDefaultValues();
                var equipment=new ArrayList<ItemStack>();
                for(var slot:EquipmentSlot.values())equipment.add(living==null?ItemStack.EMPTY:living.getItemBySlot(slot).copy());
                var lightPos=BlockPos.containing(e.getEyePosition());
                int block=e.isOnFire()?15:level.getBrightness(LightLayer.BLOCK,lightPos);
                int light=(block<<4)|(level.getBrightness(LightLayer.SKY,lightPos)<<20);
                entries.add(new PortalEntities.Entry(e.getId(),e.getUUID(),e.getType(),
                        e instanceof ServerPlayer p?p.getGameProfile().name():"",e.position(),e.getYRot(),e.getXRot(),
                        living==null?e.getYRot():living.yBodyRot,living==null?e.getYRot():living.yHeadRot,
                        e.tickCount,light,living==null?0:living.hurtTime,living==null?0:living.deathTime,
                        living==null?0:living.walkAnimation.speed(),e.getVehicle()==null?-1:e.getVehicle().getId(),
                        new ClientboundSetEntityDataPacket(e.getId(),metadata==null?List.of():metadata),List.copyOf(equipment)));
                if(entries.size()>=PortalEntities.LIMIT)break;
            }
            PacketDistributor.sendToPlayer(player,new PortalEntities(source,target,List.copyOf(entries)));
        }
    }
}
