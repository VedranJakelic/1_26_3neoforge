package net.beamex.shamaschizm.building;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;

/** Saved geometry only. This block entity has NO ticker. */
public final class WebBlockEntity extends BlockEntity {
    private UUID group;
    private Vec3 offset = Vec3.ZERO, ownerOffset = Vec3.ZERO;
    private double width=1, height=1, yaw;
    private boolean flipped;
    private BlockState cachedState;
    private VoxelShape cachedShape;
    public WebBlockEntity(BlockPos pos, BlockState state) { super(BuildingRegistration.WEB_BLOCK_ENTITY, pos, state); }
    public boolean configured() { return group != null; }
    public UUID group() { return group; }
    public void clearConfiguration(){group=null;cachedShape=null;setChanged();}
    public boolean flipped() { return flipped; }
    public Vec3 transform(Vec3 v) {
        BlockState state=getBlockState();
        double x=state.getValue(SpanningWebBlock.MIRRORED)?-v.x:v.x, z=v.z;
        return switch(state.getValue(SpanningWebBlock.FACING)) {
            case EAST -> new Vec3(-z,v.y,x); case SOUTH -> new Vec3(-x,v.y,-z);
            case WEST -> new Vec3(z,v.y,-x); default -> new Vec3(x,v.y,z);
        };
    }
    public BlockPos owner() {
        Vec3 v=transform(ownerOffset);
        return getBlockPos().offset((int)Math.round(v.x),(int)Math.round(v.y),(int)Math.round(v.z));
    }
    public boolean master() { return configured() && owner().equals(getBlockPos()); }
    public WebRectangle rectangle() {
        Vec3 direction=transform(new Vec3(Math.cos(Math.toRadians(yaw)),0,Math.sin(Math.toRadians(yaw))));
        return new WebRectangle(Vec3.atBottomCenterOf(getBlockPos()).add(transform(offset)),width,height,
                Math.toDegrees(Math.atan2(direction.z,direction.x)));
    }
    public void configure(UUID id, WebRectangle rectangle, boolean flip, BlockPos master) {
        group=id; offset=rectangle.bottom().subtract(Vec3.atBottomCenterOf(getBlockPos()));
        ownerOffset=new Vec3(master.getX()-getBlockPos().getX(),master.getY()-getBlockPos().getY(),master.getZ()-getBlockPos().getZ());
        width=rectangle.width();height=rectangle.height();yaw=rectangle.yaw();flipped=flip;
        cachedShape=null;setChanged();
        if(level!=null)level.sendBlockUpdated(getBlockPos(),getBlockState(),getBlockState(),2);
    }
    public VoxelShape shape() {
        if(!configured())return Shapes.empty();
        if(cachedShape==null || cachedState!=getBlockState()) {
            cachedState=getBlockState();cachedShape=WebBlocks.shape(rectangle(),getBlockPos());
        }
        return cachedShape;
    }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if(configured() && level instanceof ServerLevel server)WebBlocks.remove(server,this);
        super.preRemoveSideEffects(pos,state);
    }
    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if(group==null)return;
        out.putString("WebGroup",group.toString());out.putDouble("DX",offset.x);out.putDouble("DY",offset.y);out.putDouble("DZ",offset.z);
        out.putDouble("OwnerX",ownerOffset.x);out.putDouble("OwnerY",ownerOffset.y);out.putDouble("OwnerZ",ownerOffset.z);
        out.putDouble("Width",width);out.putDouble("Height",height);out.putDouble("Yaw",yaw);out.putBoolean("Flipped",flipped);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        try{group=UUID.fromString(in.getStringOr("WebGroup",""));}catch(IllegalArgumentException e){group=null;}
        offset=new Vec3(in.getDoubleOr("DX",0),in.getDoubleOr("DY",0),in.getDoubleOr("DZ",0));
        ownerOffset=new Vec3(in.getDoubleOr("OwnerX",0),in.getDoubleOr("OwnerY",0),in.getDoubleOr("OwnerZ",0));
        width=Math.max(.0625,Math.min(5,in.getDoubleOr("Width",1)));height=Math.max(.0625,Math.min(5,in.getDoubleOr("Height",1)));
        yaw=in.getDoubleOr("Yaw",0);flipped=in.getBooleanOr("Flipped",false);cachedShape=null;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
}
