package net.beamex.shamaschizm.homunculus;

import java.util.*;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.*;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class HomunculusRegistration {
    public static EntityType<HomunculusEntity> HOMUNCULUS;
    public static Item SPAWN_EGG;
    private record Pending(ServerLevel level, UUID owner, Set<Long> chunks, long deadline) {}
    private static final List<Pending> PENDING = new ArrayList<>();
    public static final TicketController TICKETS = new TicketController(Shamaschizm.id("homunculus_sorting"), (level, helper) -> {
        // Entities are not all available during ticket restoration. Give them time to load, then discard orphan tickets.
        helper.getBlockTickets().keySet().forEach(helper::removeAllTickets);
        helper.getEntityTickets().forEach((id, set) -> {
            Set<Long> chunks = new HashSet<>(); set.normal().forEach((long n) -> chunks.add(n)); set.naturalSpawning().forEach((long n) -> chunks.add(n));
            if(chunks.size()>9) helper.removeAllTickets(id);
            else PENDING.add(new Pending(level,id,chunks,level.getGameTime()+200));
        });
    });
    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.ENTITY_TYPE, h -> {
            var id=Shamaschizm.id("homunculus");
            HOMUNCULUS=EntityType.Builder.<HomunculusEntity>of(HomunculusEntity::new,MobCategory.CREATURE)
                    .sized(.65F,.5F).clientTrackingRange(12).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE,id));
            h.register(id,HOMUNCULUS);
        });
        event.register(Registries.ITEM,h->{
            var id=Shamaschizm.id("homunculus_spawn_egg");
            SPAWN_EGG=new SpawnEggItem(new Item.Properties().spawnEgg(HOMUNCULUS).setId(ResourceKey.create(Registries.ITEM,id)));
            h.register(id,SPAWN_EGG);
        });
    }
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event) { event.put(HOMUNCULUS,HomunculusEntity.attributes().build()); }
    @SubscribeEvent public static void tabs(BuildCreativeModeTabContentsEvent event) { if(event.getTabKey().equals(ResourceKey.create(Registries.CREATIVE_MODE_TAB,net.minecraft.resources.Identifier.withDefaultNamespace("spawn_eggs"))))event.accept(SPAWN_EGG); }
    @SubscribeEvent public static void tickets(RegisterTicketControllersEvent event) { event.register(TICKETS); }
    @SubscribeEvent public static void chat(ServerChatEvent event) {
        HomunculusCommand cmd=HomunculusCommand.parse(event.getRawText()); if(cmd==null)return;
        var player=event.getPlayer();
        if(!player.level().getServer().isSameThread()){player.level().getServer().execute(()->commandNearby(player,cmd));return;}
        commandNearby(player,cmd);
    }
    private static void commandNearby(net.minecraft.server.level.ServerPlayer player,HomunculusCommand cmd){
        for(var mob:player.level().getEntitiesOfClass(HomunculusEntity.class,player.getBoundingBox().inflate(10),e->e.isAlive()&&e.distanceToSqr(player)<=100))mob.command(player,cmd);
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        Iterator<Pending> it=PENDING.iterator();
        while(it.hasNext()) {
            Pending p=it.next();
            Entity entity=p.level.getEntity(p.owner);
            if(entity instanceof HomunculusEntity h && h.isAlive() && (h.command()==HomunculusCommand.SORT || h.command()==HomunculusCommand.DESTROY)) {
                // Remove restored tickets, then let the entity recompute the bounded work area.
                for(long key:p.chunks){ChunkPos c=ChunkPos.unpack(key);TICKETS.forceChunk(p.level,p.owner,c.x(),c.z(),false,true);TICKETS.forceChunk(p.level,p.owner,c.x(),c.z(),false,false);}
                h.work.releaseTickets();h.work.updateTickets();it.remove();
            } else if(entity!=null || p.level.getGameTime()>=p.deadline) {
                for(long key:p.chunks){ChunkPos c=ChunkPos.unpack(key);TICKETS.forceChunk(p.level,p.owner,c.x(),c.z(),false,true);TICKETS.forceChunk(p.level,p.owner,c.x(),c.z(),false,false);}
                it.remove();
            }
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { PENDING.clear(); }
    private HomunculusRegistration() {}
}
