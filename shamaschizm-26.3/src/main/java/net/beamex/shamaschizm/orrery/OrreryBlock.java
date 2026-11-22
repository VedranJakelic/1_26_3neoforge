package net.beamex.shamaschizm.orrery;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
public final class OrreryBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty HANGING=BlockStateProperties.HANGING;
    public static final BooleanProperty OUTPUT=BooleanProperty.create("output");
    private static final VoxelShape SHAPE=Block.box(4,0,4,12,13,12), HANGING_SHAPE=Block.box(4,3,4,12,16,12);
    public OrreryBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(OUTPUT,false).setValue(HANGING,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,OUTPUT,HANGING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite()).setValue(HANGING,c.getClickedFace()==Direction.DOWN);}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(HANGING)?HANGING_SHAPE:SHAPE;}
    @Override protected boolean isSignalSource(BlockState s){return true;}
    @Override protected int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return s.getValue(OUTPUT)?1:0;}
    @Override protected int getDirectSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return getSignal(s,l,p,d);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(!l.isClientSide()&&l.getBlockEntity(p) instanceof OrreryBlockEntity be)be.crank();
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        return useWithoutItem(s,l,p,player,hit);
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,boolean moved){
        super.affectNeighborsAfterRemoval(state,level,pos,moved);
        if(state.getValue(OUTPUT))for(var d:Direction.values())level.updateNeighborsAt(pos.relative(d),this,null);
    }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new OrreryBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){
        return l.isClientSide()||type!=OrreryRegistration.TYPE?null:(world,pos,state,be)->((OrreryBlockEntity)be).tickServer();
    }
}
