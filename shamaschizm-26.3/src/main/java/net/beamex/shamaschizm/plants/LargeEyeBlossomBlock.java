package net.beamex.shamaschizm.plants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
/** Ordinary blocks; only flower tips poll for nearby players. */
public final class LargeEyeBlossomBlock extends Block {
 public static final BooleanProperty HANGING=BlockStateProperties.HANGING;
 public static final IntegerProperty STAGE=IntegerProperty.create("stage",0,3);
 public static final BooleanProperty OPENING=BooleanProperty.create("opening");
 public static final IntegerProperty ABSENT=IntegerProperty.create("absent",0,EyeBlossomTiming.ABSENCE_STEPS);
 private final boolean stalk;
 public LargeEyeBlossomBlock(Properties properties,boolean stalk){
  super(properties);this.stalk=stalk;
  registerDefaultState(stateDefinition.any().setValue(HANGING,false).setValue(STAGE,0).setValue(OPENING,false).setValue(ABSENT,0));
 }
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(HANGING,STAGE,OPENING,ABSENT);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext context){
  for(Direction direction:context.getNearestLookingDirections()){
   if(direction.getAxis()!=Direction.Axis.Y)continue;
   BlockState state=defaultBlockState().setValue(HANGING,direction==Direction.UP);
   if(state.canSurvive(context.getLevel(),context.getClickedPos()))return state;
  }return null;
 }
 @Override protected boolean canBeReplaced(BlockState state,BlockPlaceContext context){
  // A blossom placed against a stalk should extend the column, not replace it.
  if(context.getItemInHand().is(DecorativePlants.BLOSSOM_ITEM)||context.getItemInHand().is(DecorativePlants.STALK_ITEM))return false;
  return super.canBeReplaced(state,context);
 }
 private static Direction support(BlockState state){return state.getValue(HANGING)?Direction.UP:Direction.DOWN;}
 @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){
  Direction side=support(state);BlockPos anchor=pos.relative(side);BlockState base=level.getBlockState(anchor);
  return (base.getBlock() instanceof LargeEyeBlossomBlock && base.getValue(HANGING)==state.getValue(HANGING))
   ||base.isFaceSturdy(level,anchor,side.getOpposite());
 }
 @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
  return Block.box(2,0,2,14,16,14).move(state.getOffset(pos));
 }
 @Override protected BlockState updateShape(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,Direction side,BlockPos n,BlockState ns,RandomSource random){
  if(side==support(state)&&!state.canSurvive(level,pos))return Blocks.AIR.defaultBlockState();
  return super.updateShape(state,level,ticks,pos,side,n,ns,random);
 }
 @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved){
  super.onPlace(state,level,pos,old,moved);
  if(level instanceof ServerLevel server&&!old.is(this)){
   BlockPos anchor=pos.relative(support(state));BlockState base=level.getBlockState(anchor);
   if(!stalk&&base.is(DecorativePlants.BLOSSOM)&&base.getValue(HANGING)==state.getValue(HANGING))
    level.setBlock(anchor,DecorativePlants.STALK.defaultBlockState().setValue(HANGING,state.getValue(HANGING)),3);
   if(!stalk)server.scheduleTick(pos,this,1+server.getRandom().nextInt(2));
  }
 }
 @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
  if(!stalk&&!level.getBlockTicks().hasScheduledTick(pos,this))level.scheduleTick(pos,this,1+random.nextInt(2));
 }
 @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
  if(stalk)return;
  if(!state.canSurvive(level,pos)){level.removeBlock(pos,false);return;}
  boolean near=level.players().stream().anyMatch(p->p.isAlive()&&!p.isSpectator()
   &&p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=EyeBlossomTiming.RANGE_SQUARED);
  EyeBlossomTiming.Step step=EyeBlossomTiming.next(state.getValue(STAGE),state.getValue(OPENING),
   state.getValue(ABSENT),near,random.nextInt(2));
  BlockState updated=state.setValue(STAGE,step.stage()).setValue(OPENING,step.opening()).setValue(ABSENT,step.absent());
  if(updated!=state)level.setBlock(pos,updated,3);
  level.scheduleTick(pos,this,step.delay());
 }
}
