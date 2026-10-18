package net.beamex.shamaschizm.world.block;

import com.mojang.serialization.MapCodec;
import net.beamex.shamaschizm.event.PoisonTrapBootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

/** A redstone-powered, horizontally orientable poison-gas nozzle. */
public final class PoisonTrapBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final int TICK_INTERVAL = 2;
    private static final double RANGE = 5.5;
    private static final int POISON_DURATION = 100;

    public PoisonTrapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.SOUTH)
                .setValue(POWERED, false));
    }



    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(pos));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos,
                           BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // Re-evaluate after neighboring blocks have finished being placed.
        if (!level.isClientSide() && !level.getBlockTicks().hasScheduledTick(pos,this))
            level.scheduleTick(pos,this,1);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                                   @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide()) return;

        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            state = state.setValue(POWERED, powered);
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            if (powered) playActivationSound(level, pos);
        }

        if (powered && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    private static void playActivationSound(Level level, BlockPos pos) {
        level.playSound(null, pos, PoisonTrapBootstrap.ACTIVATION_SOUND,
                SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean powered=level.hasNeighborSignal(pos);
        if(powered!=state.getValue(POWERED)){
            state=state.setValue(POWERED,powered);
            level.setBlock(pos,state,Block.UPDATE_CLIENTS);
            if(powered)playActivationSound(level,pos);
        }
        if(!powered)return;

        Direction facing = state.getValue(FACING);
        Vec3 direction = new Vec3(facing.getStepX(), 0.0, facing.getStepZ());
        Vec3 nozzle = Vec3.atCenterOf(pos).add(direction.scale(0.63));

        emitGas(level, nozzle, direction, random);
        poisonEntities(level, nozzle, direction);

        level.scheduleTick(pos, this, TICK_INTERVAL);
    }

    private static void emitGas(ServerLevel level, Vec3 nozzle, Vec3 direction, RandomSource random) {
        Vec3 sideways = new Vec3(-direction.z, 0.0, direction.x);

        // Each particle receives its own forward velocity. The built-in noxious-gas
        // particle then slows, expands, collides with blocks, rises and fades.
        for (int i = 0; i < 4; i++) {
            double lateralOffset = random.nextGaussian() * 0.035;
            double verticalOffset = random.nextGaussian() * 0.025;
            Vec3 start = nozzle.add(sideways.scale(lateralOffset)).add(0.0, verticalOffset, 0.0);

            double forwardSpeed = 0.105 + random.nextDouble() * 0.045;
            double sidewaysSpeed = random.nextGaussian() * 0.012;
            double upwardSpeed = 0.002 + random.nextDouble() * 0.012;
            Vec3 velocity = direction.scale(forwardSpeed)
                    .add(sideways.scale(sidewaysSpeed))
                    .add(0.0, upwardSpeed, 0.0);

            level.sendParticles(ParticleTypes.NOXIOUS_GAS,
                    start.x, start.y, start.z, 0,
                    velocity.x, velocity.y, velocity.z, 1.0);
        }
    }

    private static void poisonEntities(ServerLevel level, Vec3 nozzle, Vec3 direction) {
        Vec3 end = nozzle.add(direction.scale(RANGE));
        AABB searchArea = new AABB(nozzle, end).inflate(2.15, 1.75, 2.15);

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> entity.isAlive() && !entity.isSpectator())) {
            Vec3 contact = findGasContact(entity.getBoundingBox(), nozzle, direction);
            if (contact == null || !hasLineOfSight(level, nozzle, contact)) continue;

            MobEffectInstance currentPoison = entity.getEffect(MobEffects.POISON);
            if (currentPoison == null || currentPoison.getDuration() < 60) {
                entity.addEffect(new MobEffectInstance(
                        MobEffects.POISON, POISON_DURATION, 0, false, true, true));
            }
        }
    }

    /**
     * Samples the gas cone against the entity's complete collision box. This
     * catches feet, legs and torso instead of requiring the entity's eyes to
     * happen to pass through a narrow waist-height plume.
     */
    private static @Nullable Vec3 findGasContact(AABB entityBox, Vec3 nozzle, Vec3 direction) {
        for (double forward = 0.2; forward <= RANGE; forward += 0.35) {
            Vec3 axisPoint = nozzle.add(direction.scale(forward));
            double radius = 0.65 + forward * 0.30;
            if (entityBox.distanceToSqr(axisPoint) <= radius * radius) {
                return new Vec3(
                        Mth.clamp(axisPoint.x, entityBox.minX, entityBox.maxX),
                        Mth.clamp(axisPoint.y, entityBox.minY, entityBox.maxY),
                        Mth.clamp(axisPoint.z, entityBox.minZ, entityBox.maxZ));
            }
        }
        return null;
    }

    private static boolean hasLineOfSight(Level level, Vec3 start, Vec3 end) {
        HitResult hit = level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return hit.getType() != HitResult.Type.BLOCK;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }
}
