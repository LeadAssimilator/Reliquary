package reliquary.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import reliquary.blocks.tile.ApothecaryCauldronBlockEntity;
import reliquary.init.ModBlocks;
import reliquary.items.ICreativeTabItemGenerator;
import reliquary.reference.Config;
import reliquary.util.BlockEntityHelper;
import reliquary.util.WorldHelper;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ApothecaryCauldronBlock extends Block implements EntityBlock, ICreativeTabItemGenerator {

	public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 3);
	private static final VoxelShape INSIDE = box(2.0D, 4.0D, 2.0D, 14.0D, 16.0D, 14.0D);
	private static final VoxelShape SHAPE = Stream.of(
			Block.box(1, 1, 1, 15, 2, 15),
			Block.box(2, 0, 2, 4, 1, 4),
			Block.box(12, 0, 2, 14, 1, 4),
			Block.box(12, 0, 12, 14, 1, 14),
			Block.box(2, 0, 12, 4, 1, 14),
			Block.box(0, 2, 0, 16, 13, 2),
			Block.box(0, 2, 14, 16, 13, 16),
			Block.box(14, 2, 2, 16, 13, 14),
			Block.box(0, 2, 2, 2, 13, 14),
			Block.box(1, 13, 2, 2, 14, 14),
			Block.box(1, 13, 1, 15, 14, 2),
			Block.box(1, 13, 14, 15, 14, 15),
			Block.box(14, 13, 2, 15, 14, 14),
			Block.box(0, 14, 2, 2, 16, 14),
			Block.box(0, 14, 0, 16, 16, 2),
			Block.box(14, 14, 2, 16, 16, 14),
			Block.box(0, 14, 14, 16, 16, 16)
	).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

	public ApothecaryCauldronBlock() {
		super(Properties.of().mapColor(MapColor.METAL).strength(1.5F, 5.0F).noOcclusion());
		registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
	}

	@Override
	public void addCreativeTabItems(Consumer<ItemStack> itemConsumer) {
		if (Boolean.TRUE.equals(Config.COMMON.disable.disablePotions.get())) {
			return;
		}
		itemConsumer.accept(new ItemStack(this));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LEVEL);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return INSIDE;
	}

	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (!level.isClientSide) {
			ApothecaryCauldronBlockEntity cauldron = (ApothecaryCauldronBlockEntity) level.getBlockEntity(pos);
			if (cauldron != null) {
				cauldron.handleCollidingEntity(level, pos, entity);
			}
		}
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (level.isClientSide) {
			return !heldItem.isEmpty() ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
		} else {
			if (heldItem.isEmpty()) {
				return ItemInteractionResult.CONSUME;
			} else {
				ApothecaryCauldronBlockEntity cauldron = (ApothecaryCauldronBlockEntity) level.getBlockEntity(pos);

				if (cauldron != null) {
					return cauldron.handleBlockActivation(level, player, hand, pos);
				}
			}
		}
		return ItemInteractionResult.CONSUME;
	}

	@Override
	public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
		if (precipitation == Biome.Precipitation.RAIN && level.getRandom().nextFloat() < 0.05F) {
			WorldHelper.getBlockEntity(level, pos, ApothecaryCauldronBlockEntity.class).ifPresent(ApothecaryCauldronBlockEntity::fillWithRain);
		}
	}

	/**
	 * If this returns true, then comparators facing away from this block will
	 * use the value from getComparatorInputOverride instead of the actual
	 * redstone signal strength.
	 */
	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/**
	 * If hasComparatorInputOverride returns true, the return value from this is
	 * used instead of the redstone signal strength when this block inputs to a
	 * comparator.
	 */
	@Override
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		ApothecaryCauldronBlockEntity cauldron = (ApothecaryCauldronBlockEntity) level.getBlockEntity(pos);
		if (cauldron != null) {
			return cauldron.getLiquidLevel();
		}
		return 0;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ApothecaryCauldronBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
		return BlockEntityHelper.createTickerHelper(blockEntityType, ModBlocks.APOTHECARY_CAULDRON_TILE_TYPE.get(), (l, p, s, be) -> be.serverTick(l, p));
	}
}
