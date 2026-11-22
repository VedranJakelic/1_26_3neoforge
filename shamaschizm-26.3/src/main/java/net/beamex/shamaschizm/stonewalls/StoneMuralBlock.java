package net.beamex.shamaschizm.stonewalls;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Nine ordinary model blocks, so the mural renders and collides normally through local portals. */
public final class StoneMuralBlock extends Block {
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty COLUMN=IntegerProperty.create("column",0,2), ROW=IntegerProperty.create("row",0,2);
    private boolean removing;
    public StoneMuralBlock(Properties properties){
        super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(COLUMN,1).setValue(ROW,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,COLUMN,ROW);}
    private static BlockPos part(BlockPos base,Direction facing,int column,int row){return base.relative(facing.getCounterClockWise(),column-1).above(row);}
    private static BlockPos base(BlockPos pos,BlockState state){return pos.relative(state.getValue(FACING).getCounterClockWise(),1-state.getValue(COLUMN)).below(state.getValue(ROW));}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        Direction facing=context.getHorizontalDirection().getOpposite();
        BlockState state=defaultBlockState().setValue(FACING,facing);
        Level level=context.getLevel();
        var collision=context.getPlayer()==null?CollisionContext.empty():CollisionContext.of(context.getPlayer());
        for(int row=0;row<3;row++)for(int column=0;column<3;column++){
            BlockPos pos=part(context.getClickedPos(),facing,column,row);
            if(pos.getY()<level.getMinY()||pos.getY()>level.getMaxY()||!level.getWorldBorder().isWithinBounds(pos)
                    ||!level.getBlockState(pos).canBeReplaced(context)||!level.isUnobstructed(state,pos,collision))return null;
        }
        return state;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
        super.setPlacedBy(level,pos,state,placer,stack);
        if(level.isClientSide())return;
        for(int row=0;row<3;row++)for(int column=0;column<3;column++){
            if(row==0&&column==1)continue;
            level.setBlock(part(pos,state.getValue(FACING),column,row),state.setValue(COLUMN,column).setValue(ROW,row),3);
        }
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state,ServerLevel level,BlockPos pos,boolean piston){
        super.affectNeighborsAfterRemoval(state,level,pos,piston);
        if(removing||level.getBlockState(pos).is(this))return;
        removing=true;
        try{
            BlockPos base=base(pos,state);Direction facing=state.getValue(FACING);
            for(int row=0;row<3;row++)for(int column=0;column<3;column++){
                BlockPos other=part(base,facing,column,row);BlockState found=level.getBlockState(other);
                if(found.is(this)&&found.getValue(FACING)==facing&&found.getValue(COLUMN)==column&&found.getValue(ROW)==row)
                    level.setBlock(other,Blocks.AIR.defaultBlockState(),3);
            }
        }finally{removing=false;}
    }
    @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState state,Mirror mirror){
        if(mirror==Mirror.NONE)return state;
        return rotate(state,mirror.getRotation(state.getValue(FACING))).setValue(COLUMN,2-state.getValue(COLUMN));
    }
}
