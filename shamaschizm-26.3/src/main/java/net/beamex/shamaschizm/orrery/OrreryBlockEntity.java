package net.beamex.shamaschizm.orrery;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
public final class OrreryBlockEntity extends BlockEntity {
    private double phase,impulse;
    private long stamp,lastSqueak=Long.MIN_VALUE/2;
    private boolean powered,ready;
    public OrreryBlockEntity(BlockPos p,BlockState s){super(OrreryRegistration.TYPE,p,s);}
    public double visualPhase(float partial){return OrreryMotion.phase(phase,impulse,powered,level==null?0:Math.max(0,level.getGameTime()-stamp)+partial);}
    private void settle(long now){
        long elapsed=Math.max(0,now-stamp);phase=OrreryMotion.phase(phase,impulse,powered,elapsed);impulse=OrreryMotion.speed(impulse,elapsed);stamp=now;
    }
    public void tickServer(){
        if(!(level instanceof ServerLevel))return;
        long now=level.getGameTime();
        // Pause mechanics in unloaded chunks instead of simulating unknown redstone inputs.
        if(!ready){stamp=now;ready=true;powered=level.getBestNeighborSignal(worldPosition)>=10;sync();}
        boolean input=level.getBestNeighborSignal(worldPosition)>=10;
        if(input!=powered){settle(now);powered=input;sync();}
        else if(now-stamp>=20&&(powered||impulse>0)){settle(now);sync();}
        updateOutput(now);
    }
    private void updateOutput(long now){
        boolean output=OrreryMotion.nearEarth(OrreryMotion.phase(phase,impulse,powered,Math.max(0,now-stamp)));
        var state=getBlockState();
        if(output!=state.getValue(OrreryBlock.OUTPUT)){
            level.setBlock(worldPosition,state.setValue(OrreryBlock.OUTPUT,output),3);
            // Strongly-powered support blocks must notify their adjacent dust as well.
            for(var d:Direction.values())level.updateNeighborsAt(worldPosition.relative(d),OrreryRegistration.BLOCK,null);
        }
    }
    public void crank(){
        if(!(level instanceof ServerLevel))return;
        if(!ready)tickServer();
        long now=level.getGameTime();settle(now);
        // Add momentum only: never teleport the planets to a new phase on click.
        impulse=Math.min(OrreryMotion.MAX_SPEED,impulse+OrreryMotion.CLICK_IMPULSE);
        if(now-lastSqueak>=4){
            lastSqueak=now;
            level.playSound(null,worldPosition,OrreryRegistration.SQUEAK,SoundSource.BLOCKS,1.0F,.94F+level.getRandom().nextFloat()*.12F);
        }
        sync();updateOutput(now);
    }
    private void sync(){setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);
        // Write a settled snapshot. This also lets unticked portal replicas extrapolate correctly.
        long now=level==null?stamp:level.getGameTime();double elapsed=Math.max(0,now-stamp);
        out.putDouble("OrreryPhase",OrreryMotion.phase(phase,impulse,powered,elapsed));
        out.putDouble("OrreryImpulse",OrreryMotion.speed(impulse,elapsed));
        out.putLong("OrreryStamp",now);out.putBoolean("OrreryPowered",powered);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);
        double p=in.getDoubleOr("OrreryPhase",0),v=in.getDoubleOr("OrreryImpulse",0);
        phase=Double.isFinite(p)?Math.max(0,p):0;
        impulse=Double.isFinite(v)?Math.clamp(v,0,OrreryMotion.MAX_SPEED):0;
        stamp=in.getLongOr("OrreryStamp",0);powered=in.getBooleanOr("OrreryPowered",false);ready=false;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
}
