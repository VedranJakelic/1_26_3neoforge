package net.beamex.shamaschizm.eclipse;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.ModEntities;
import net.beamex.shamaschizm.entity.custom.GiantCentipedeEntity;
import net.beamex.shamaschizm.earthworm.Earthworms;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class EclipseEvents {
    private EclipseEvents() {}
    // Run before the shared depth/density guards: their FAIL result must still win.
    @SubscribeEvent(priority=EventPriority.HIGH)
    public static void placement(MobSpawnEvent.SpawnPlacementCheck e) {
        var level=e.getLevel().getLevel(); var pos=e.getPos(); var type=e.getEntityType();
        if (e.getSpawnType()!=EntitySpawnReason.NATURAL || !Eclipses.active(level,pos)
                || level.getDifficulty()==Difficulty.PEACEFUL
                || !level.getWorldBorder().isWithinBounds(pos)) return;
        boolean allowed=type==EntityTypes.ZOMBIE || type==EntityTypes.SKELETON || type==EntityTypes.SPIDER
                || type==EntityTypes.HUSK || type==EntityTypes.CREEPER || type==EntityTypes.WITHER_SKELETON
                || type==ModEntities.GIANT_CENTIPEDE.get() || type==ModEntities.ROACH.get()
                || type==ModEntities.BABY_ROACH.get() || type==Earthworms.TYPE;
        if (!allowed || !Mob.checkMobSpawnRules((net.minecraft.world.entity.EntityType<? extends Mob>)type, e.getLevel(), e.getSpawnType(),pos,level.getRandom())) return;
        if (type==ModEntities.GIANT_CENTIPEDE.get()) {
            long chains=level.getEntitiesOfClass(GiantCentipedeEntity.class,new AABB(pos).inflate(50,2048,50),
                    m->m.isAlive() && m.isDriver()).stream().map(GiantCentipedeEntity::getChainId).distinct().count();
            if (chains>=2) { e.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL); return; }
        }
        // NaturalSpawner still checks support, obstruction, player distances and its normal population caps.
        e.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.SUCCEED);
    }
    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("schizm_eclipse").executes(context->{
            long time=context.getSource().getLevel().getOverworldClockTime();
            var a=EclipseOrbit.next(time);
            context.getSource().sendSuccess(()->Component.literal(String.format(java.util.Locale.ROOT,
                    "%s conjunction: tick %d; 6,000-tick danger window %d–%d. Shadow at conjunction: X %.1f Z %.1f (Y=64); it moves during the event. Radius 250.",
                    a.active(time)?"Active":"Next",a.peak(),a.peak()-EclipseOrbit.HALF_WINDOW,
                    a.peak()+EclipseOrbit.HALF_WINDOW,a.x(),a.z())),false);
            return 1;
        }));
        e.getDispatcher().register(Commands.literal("schizm_sky").executes(context->{
            var source=context.getSource();
            if(!source.getLevel().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)){
                source.sendFailure(Component.literal("Sky observations require the Overworld."));return 0;
            }
            var player=source.getEntity() instanceof net.minecraft.world.entity.player.Player p?p:null;
            var pos=player!=null?player.getEyePosition():source.getPosition();
            long time=source.getLevel().getOverworldClockTime();
            var sky=EclipseOrbit.sky(time,pos.x,pos.y,pos.z);
            source.sendSuccess(()->Component.literal(String.format(java.util.Locale.ROOT,
                    "Observation tick %d; X %.2f Y %.2f Z %.2f. Azimuth: 0=south, 90=west. %s %s Separation %.4f degrees.",
                    time,pos.x,pos.y,pos.z,observation("Sun",sky.sun(),EclipseOrbit.SUN_RADIUS),
                    observation("Moon",sky.moon(),sky.moonRadius()),Math.toDegrees(sky.separation()))),false);
            return 1;
        }));
    }
    private static String observation(String name,EclipseOrbit.V direction,double radius){
        double azimuth=(Math.toDegrees(Math.atan2(-direction.x(),direction.z()))+360)%360;
        return String.format(java.util.Locale.ROOT,"%s: az %.4f, elevation %.4f, angular radius %.4f; direction (%.6f, %.6f, %.6f).",
                name,azimuth,Math.toDegrees(Math.asin(direction.y())),Math.toDegrees(radius),direction.x(),direction.y(),direction.z());
    }
}
