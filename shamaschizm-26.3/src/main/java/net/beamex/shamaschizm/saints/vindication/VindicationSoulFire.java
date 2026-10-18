package net.beamex.shamaschizm.saints.vindication;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoulFireBlock;
import net.minecraft.world.level.block.state.BlockState;
/** Soul fire supported by impacted arena surfaces, without replacing those blocks. */
public final class VindicationSoulFire extends SoulFireBlock {
 public VindicationSoulFire(Properties p){super(p);}

 @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){for(Direction d:Direction.values())if(level.getBlockState(pos.relative(d)).isFaceSturdy(level,pos.relative(d),d.getOpposite()))return true;return false;}
}
