package net.beamex.shamaschizm.elf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** No combat or wander goals; the only movement targets are saved route waypoints. */
public final class ElfEntity extends PathfinderMob {
    private List<BlockPos> route=List.of();
    private List<BoundingBox> rooms=List.of();
    private int waypoint, direction=1, stalled;
    private boolean trailImported;
    public ElfEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type,level);setPersistenceRequired();setPermanentlyInvulnerable(true);setSilent(true);
    }
    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH,20)
                .add(Attributes.MOVEMENT_SPEED,0.22).add(Attributes.FOLLOW_RANGE,128)
                .add(Attributes.KNOCKBACK_RESISTANCE,1);
    }
    @Override protected void registerGoals() {}
    public void configure(List<BlockPos> points,List<BoundingBox> boxes,int start) {
        route=List.copyOf(points);rooms=List.copyOf(boxes);waypoint=start;
        direction=random.nextBoolean()?1:-1;
    }
    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        if (!trailImported) { ElfRouteData.get(level).add(route); trailImported=true; }
        if (ElfConversations.busy(this)) {getNavigation().stop();return;}
        if (route.isEmpty() || tickCount%20!=0) return;
        waypoint=Math.max(0,Math.min(route.size()-1,waypoint));
        BlockPos target=route.get(waypoint);
        if (distanceToSqr(target.getX()+0.5,target.getY(),target.getZ()+0.5)<2.25) {
            if (waypoint+direction<0 || waypoint+direction>=route.size()) direction=-direction;
            waypoint=Math.max(0,Math.min(route.size()-1,waypoint+direction));target=route.get(waypoint);
            stalled=0;
        }
        if (!getNavigation().isDone()) return;
        var path=getNavigation().createPath(target,0);
        boolean allowed=path!=null && path.canReach();
        if (allowed) for(int i=0;i<path.getNodeCount();i++) {
            BlockPos node=path.getNode(i).asBlockPos();
            if (rooms.stream().noneMatch(b->b.isInside(node))) {allowed=false;break;}
        }
        if (allowed) {getNavigation().moveTo(path,1);stalled=0;}
        else if (++stalled>=5) {direction=-direction;waypoint=Math.max(0,Math.min(route.size()-1,waypoint+direction));stalled=0;}
    }
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){return false;}
    // LivingEntity's own tick can push OTHER entities even when this elf is
    // unpushable. Disable that outgoing push path as well as incoming pushes.
    @Override protected void pushEntities(){}
    @Override protected void doPush(Entity entity){}
    @Override public boolean isPushable(){return false;}
    @Override public boolean isPickable(){return false;}
    @Override public boolean isAttackable(){return false;}
    @Override public boolean canBeCollidedWith(Entity entity){return false;}
    @Override public boolean canCollideWith(Entity entity){return false;}
    @Override public void push(Entity entity){}
    @Override public void push(double x,double y,double z){}
    @Override public boolean isIgnoringBlockTriggers(){return true;}
    @Override public net.minecraft.world.level.material.PushReaction getPistonPushReaction(){return net.minecraft.world.level.material.PushReaction.IGNORE_ENTITY;}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override protected void addAdditionalSaveData(ValueOutput out){
        super.addAdditionalSaveData(out);out.store("ElfRoute",BlockPos.CODEC.listOf(),route);
        out.store("ElfRooms",BoundingBox.CODEC.listOf(),rooms);out.putInt("ElfWaypoint",waypoint);out.putInt("ElfDirection",direction);
    }
    @Override protected void readAdditionalSaveData(ValueInput in){
        super.readAdditionalSaveData(in);route=in.read("ElfRoute",BlockPos.CODEC.listOf()).orElse(List.of());
        rooms=in.read("ElfRooms",BoundingBox.CODEC.listOf()).orElse(List.of());waypoint=in.getIntOr("ElfWaypoint",0);
        direction=in.getIntOr("ElfDirection",1)<0?-1:1;setPersistenceRequired();setSilent(true);setPermanentlyInvulnerable(true);
    }
}
