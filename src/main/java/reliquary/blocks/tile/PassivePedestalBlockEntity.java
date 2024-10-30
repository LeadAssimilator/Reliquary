package reliquary.blocks.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import reliquary.init.ModBlocks;
import reliquary.util.InventoryHelper;
import reliquary.util.WorldHelper;

public class PassivePedestalBlockEntity extends BlockEntityBase implements Container {
	protected ItemStack item;

	private final IItemHandler inventoryWrapper = new InvWrapper(this);

	public ItemStack getItem() {
		return item;
	}

	public PassivePedestalBlockEntity(BlockPos pos, BlockState state) {
		this(ModBlocks.PASSIVE_PEDESTAL_TILE_TYPE.get(), pos, state);
	}

	PassivePedestalBlockEntity(BlockEntityType<?> tileEntityType, BlockPos pos, BlockState state) {
		super(tileEntityType, pos, state);
		item = ItemStack.EMPTY;
	}

	public IItemHandler getItemHandler() {
		return inventoryWrapper;
	}

	public void dropPedestalInventory(Level level) {
		if (!item.isEmpty()) {
			InventoryHelper.spawnItemStack(level, worldPosition, item);
		}
	}

	public void removeAndSpawnItem(Level level) {
		if (!item.isEmpty()) {
			if (!level.isClientSide) {
				setChanged();
				ItemEntity itemEntity = new ItemEntity(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 1D, worldPosition.getZ() + 0.5D, item);
				level.addFreshEntity(itemEntity);
				WorldHelper.notifyBlockUpdate(this);
			}
			item = ItemStack.EMPTY;
		}
	}

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public ItemStack getItem(int index) {
		return index == 0 ? item : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int index, int count) {
		if (index == 0) {
			return decrStackInInventory(count);
		}
		return ItemStack.EMPTY;
	}

	private ItemStack decrStackInInventory(int count) {
		if (!item.isEmpty()) {
			ItemStack stack = item.split(count);

			if (item.isEmpty()) {
				notifyBlock();
			}

			return stack;
		} else {
			return ItemStack.EMPTY;
		}
	}

	@Override
	public ItemStack removeItemNoUpdate(int index) {
		if (index == 0) {
			ItemStack stack = item;
			item = ItemStack.EMPTY;
			notifyBlock();
			return stack;
		}

		return ItemStack.EMPTY;
	}

	private void notifyBlock() {
		if (level == null) {
			return;
		}
		BlockState blockState = level.getBlockState(getBlockPos());
		level.sendBlockUpdated(getBlockPos(), blockState, blockState, 3);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		item = tag.contains("item") ? ItemStack.parse(registries, tag.getCompound("item")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
	}

	@Override
	public void saveAdditional(CompoundTag compound, HolderLookup.Provider registries) {
		super.saveAdditional(compound, registries);

		if (!item.isEmpty()) {
			compound.put("item", item.save(registries));
		}
	}

	@Override
	public void setItem(int index, ItemStack stack) {
		if (index == 0) {
			item = stack;
			notifyBlock();
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return false;
	}

	@Override
	public void startOpen(Player player) {
		//noop
	}

	@Override
	public void stopOpen(Player player) {
		//noop
	}

	@Override
	public boolean canPlaceItem(int index, ItemStack stack) {
		return index == 0;
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < getContainerSize(); i++) {
			setItem(i, ItemStack.EMPTY);
		}
	}

	@Override
	public boolean isEmpty() {
		return item.isEmpty();
	}
}
