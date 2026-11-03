package net.beamex.shamaschizm.plants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
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
 public static final BooleanProperty HAS_DEW=BooleanProperty.create("has_dew");
 public OvertreeRootBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(HANGING,true).setValue(WATERLOGGED,false).setValue(HAS_DEW,false));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(HANGING,WATERLOGGED,HAS_DEW);}

 // The dew is harvested without removing its supporting root.
 private InteractionResult harvestDew(BlockState state, Level level, BlockPos pos) {
  if (!state.getValue(HAS_DEW)) return InteractionResult.PASS;
  if (level instanceof ServerLevel server) {
   Block.popResource(server,pos,new ItemStack(DecorativePlants.DEW_ITEM));
   server.setBlock(pos,state.setValue(HAS_DEW,false),Block.UPDATE_CLIENTS);
   server.playSound(null,pos,SoundEvents.CAVE_VINES_PICK_BERRIES,SoundSource.BLOCKS,
    1.0F,0.9F+server.getRandom().nextFloat()*0.2F);
   return InteractionResult.SUCCESS_SERVER;
  }
  return InteractionResult.SUCCESS;
 }
 @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,
   Player player,InteractionHand hand,BlockHitResult hit) {
  return state.getValue(HAS_DEW)?harvestDew(state,level,pos):InteractionResult.TRY_WITH_EMPTY_HAND;
 }
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,
   Player player,BlockHitResult hit) {
  return harvestDew(state,level,pos);
 }
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
  // Dungeon structures contain decorative roots whose anchor can be a non-sturdy
  // block or a neighboring root. Previously, a later neighbor update replaced
  // those roots with air, dropping their attached Dew as loot on the ground.
  // Keep already-placed roots in place instead of destroying them on updates.
  // getStateForPlacement still enforces support for player placement.
  return super.updateShape(s,l,ticks,p,d,n,ns,r);
 }
}
