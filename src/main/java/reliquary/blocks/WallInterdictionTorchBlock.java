package reliquary.blocks;

import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Map;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public class WallInterdictionTorchBlock extends InterdictionTorchBlock {
	private static final Map<Direction, VoxelShape> SHAPES = Maps.newEnumMap(Map.of(Direction.NORTH, Block.box(5.5D, 3.0D, 11.0D, 10.5D, 13.0D, 16.0D), Direction.SOUTH, Block.box(5.5D, 3.0D, 0.0D, 10.5D, 13.0D, 5.0D), Direction.WEST, Block.box(11.0D, 3.0D, 5.5D, 16.0D, 13.0D, 10.5D), Direction.EAST, Block.box(0.0D, 3.0D, 5.5D, 5.0D, 13.0D, 10.5D)));

	public WallInterdictionTorchBlock() {
		super();
		registerDefaultState(stateDefinition.any().setValue(HORIZONTAL_FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HORIZONTAL_FACING);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		double xOffset = pos.getX() + 0.5F;
		double yOffset = pos.getY() + 0.7F;
		double zOffset = pos.getZ() + 0.5F;

		double verticalModifier = 0.22D;
		double horizontalModifier = 0.27D;
		Direction oppositeFacing = state.getValue(HORIZONTAL_FACING).getOpposite();
		level.addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, FastColor.ARGB32.opaque( 0)), xOffset + horizontalModifier * oppositeFacing.getStepX(), yOffset + verticalModifier, zOffset + horizontalModifier * oppositeFacing.getStepZ(), 0.0D, 0.0D, 0.0D);
		level.addParticle(ParticleTypes.FLAME, xOffset + horizontalModifier * oppositeFacing.getStepX(), yOffset + verticalModifier, zOffset + horizontalModifier * oppositeFacing.getStepZ(), 0.0D, 0.0D, 0.0D);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(HORIZONTAL_FACING));
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction direction = state.getValue(HORIZONTAL_FACING);
		BlockPos blockpos = pos.relative(direction.getOpposite());
		BlockState blockstate = level.getBlockState(blockpos);
		return blockstate.isFaceSturdy(level, blockpos, direction);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState blockstate = defaultBlockState();
		Level level = context.getLevel();
		BlockPos blockpos = context.getClickedPos();
		Direction[] adirection = context.getNearestLookingDirections();

		for (Direction direction : adirection) {
			if (direction.getAxis().isHorizontal()) {
				Direction direction1 = direction.getOpposite();
				blockstate = blockstate.setValue(HORIZONTAL_FACING, direction1);
				if (blockstate.canSurvive(level, blockpos)) {
					return blockstate;
				}
			}
		}

		return null;
	}
}
