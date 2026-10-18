package net.beamex.shamaschizm.plants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.shapes.*;
/** Waterloggable hanging roots, also attachable to a solid upper surface. */
public final class OvertreeRootBlock extends Block implements SimpleWaterloggedBlock {
 public static final BooleanProperty HANGING=BlockStateProperties.HANGING;
 public static final BooleanProperty WATERLOGGED=BlockStateProperties.WATERLOGGED;
 public OvertreeRootBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(HANGING,true).setValue(WATERLOGGED,false));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(HANGING,WATERLOGGED);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){
  for(Direction d:c.getNearestLookingDirections()){
   if(d.getAxis()!=Direction.Axis.Y)continue;
   BlockState state=defaultBlockState().setValue(HANGING,d==Direction.UP)
    .setValue(WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).is(Fluids.WATER));
   if(state.canSurvive(c.getLevel(),c.getClickedPos()))return state;
  }return null;
 }
 @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){
  Direction d=s.getValue(HANGING)?Direction.UP:Direction.DOWN;BlockPos a=p.relative(d);
  return l.getBlockState(a).isFaceSturdy(l,a,d.getOpposite());
 }
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(2,0,2,14,16,14);}
 @Override protected FluidState getFluidState(BlockState s){return s.getValue(WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(s);}
 @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos n,BlockState ns,RandomSource r){
  if(s.getValue(WATERLOGGED))ticks.scheduleTick(p,Fluids.WATER,Fluids.WATER.getTickDelay(l));
  if(!s.canSurvive(l,p))return s.getValue(WATERLOGGED)?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState();
  return super.updateShape(s,l,ticks,p,d,n,ns,r);
 }
}
