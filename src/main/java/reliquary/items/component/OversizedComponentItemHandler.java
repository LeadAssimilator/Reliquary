package reliquary.items.component;

import com.google.common.base.Preconditions;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.IntUnaryOperator;

public class OversizedComponentItemHandler implements IItemHandlerModifiable {
	protected final ItemStack parent;
	protected final DataComponentType<OversizedItemContainerContents> component;
	protected final int size;
	private final BiFunction<ItemStack, Integer, Integer> getSlotLimit;
	private final BiPredicate<Integer, ItemStack> isItemValid;
	private final IntUnaryOperator getSlotUnitWorth;

	public OversizedComponentItemHandler(ItemStack parent, DataComponentType<OversizedItemContainerContents> component, int size, BiFunction<ItemStack, Integer, Integer> getSlotLimit, BiPredicate<Integer, ItemStack> isItemValid, IntUnaryOperator getSlotUnitWorth) {
		this.parent = parent;
		this.component = component;
		this.size = size;
		this.getSlotLimit = getSlotLimit;
		this.isItemValid = isItemValid;
		this.getSlotUnitWorth = getSlotUnitWorth;
		Preconditions.checkArgument(size <= OversizedItemContainerContents.MAX_SIZE, "The max size of OversizedItemContainerContents is " + OversizedItemContainerContents.MAX_SIZE + " slots.");
	}

	@Override
	public int getSlots() {
		return this.size;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		OversizedItemContainerContents contents = this.getContents();
		return this.getStackFromContents(contents, slot);
	}

	public int getCountInSlot(int slot) {
		OversizedItemContainerContents contents = this.getContents();
		return contents.getSlots() > slot ? contents.getCountInSlot(slot) : 0;
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		this.validateSlotIndex(slot);
		if (!this.isItemValid(slot, stack)) {
			throw new RuntimeException("Invalid stack " + stack + " for slot " + slot + ")");
		} else {
			OversizedItemContainerContents contents = this.getContents();
			ItemStack existing = this.getStackFromContents(contents, slot);
			if (!ItemStack.matches(stack, existing)) {
				if (getSlotUnitWorth.applyAsInt(slot) != 1) {
					stack = stack.copy();
					stack.setCount(stack.getCount() * getSlotUnitWorth.applyAsInt(slot));
				}
				this.updateContents(contents, stack, slot);
			}
		}
	}

	public ItemStack insertItemOrAddIntoNewSlotIfNoStackMatches(ItemStack stack) {
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		for (int slot = 0; slot < getSlots(); slot++) {
			if (ItemStack.isSameItemSameComponents(stack, getStackInSlot(slot)) || (isItemValid(slot, stack) && getStackInSlot(slot).isEmpty())) {
				return insertItem(slot, stack, false);
			}
		}

		OversizedItemContainerContents contents = this.getContents();
		int newSlot = this.getSlots();
		NonNullList<ItemStack> list = NonNullList.withSize(Math.max(contents.getSlots(), this.getSlots() + 1), ItemStack.EMPTY);
		if (!this.isItemValid(newSlot, stack)) {
			return stack;
		}
		contents.copyInto(list);
		int countToAdd = Math.min(stack.getCount(), getSlotLimit(list.size()));
		ItemStack stackToAdd = stack.copyWithCount(countToAdd);
		if (getSlotUnitWorth.applyAsInt(newSlot) != 1) {
			stackToAdd = stackToAdd.copy();
			stackToAdd.setCount(stackToAdd.getCount() * getSlotUnitWorth.applyAsInt(newSlot));
		}
		list.set(newSlot, stackToAdd);
		this.parent.set(this.component, OversizedItemContainerContents.fromItems(list));
		this.onContentsChanged(newSlot, ItemStack.EMPTY, stackToAdd);
		return countToAdd == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - countToAdd);
	}

	public ItemStack extractItemAndRemoveSlotIfEmpty(int slot, int amount, boolean simulate) {
		return extractItemAndRemoveSlotIfEmpty(slot, amount, () -> {
		}, simulate);
	}

	public ItemStack extractItemAndRemoveSlotIfEmpty(int slot, int amount, Runnable onSlotRemoved, boolean simulate) {
		ItemStack stack = this.extractItem(slot, amount, simulate);
		if (!simulate && getStackInSlot(slot).isEmpty()) {
			OversizedItemContainerContents contents = this.getContents();
			NonNullList<ItemStack> list = NonNullList.withSize(Math.max(contents.getSlots(), this.getSlots()), ItemStack.EMPTY);
			for (int i = 0; i < slot; i++) {
				list.set(i, contents.getSlots() > i ? contents.getStackInSlot(i) : ItemStack.EMPTY);
			}
			for (int i = slot; i < list.size(); i++) {
				list.set(i, contents.getSlots() > i + 1 ? contents.getStackInSlot(i + 1) : ItemStack.EMPTY);
			}
			this.parent.set(this.component, OversizedItemContainerContents.fromItems(list));
			this.onContentsChanged(slot, stack, ItemStack.EMPTY);
			onSlotRemoved.run();
		}
		return stack;

	}

	@Override
	public ItemStack insertItem(int slot, ItemStack toInsert, boolean simulate) {
		this.validateSlotIndex(slot);
		if (toInsert.isEmpty()) {
			return ItemStack.EMPTY;
		} else if (!this.isItemValid(slot, toInsert)) {
			return toInsert;
		} else {
			OversizedItemContainerContents contents = this.getContents();
			ItemStack existing = this.getStackFromContents(contents, slot);
			int insertLimit = this.getSlotLimit(slot) / getSlotUnitWorth.applyAsInt(slot);
			if (!existing.isEmpty()) {
				if (!ItemStack.isSameItemSameComponents(toInsert, existing)) {
					return toInsert;
				}

				insertLimit -= existing.getCount();
			}

			if (insertLimit <= 0) {
				return toInsert;
			} else {
				int inserted = Math.min(insertLimit, toInsert.getCount());
				if (!simulate) {
					this.updateContents(contents, toInsert.copyWithCount(existing.getCount() + inserted * getSlotUnitWorth.applyAsInt(slot)), slot);
				}

				return toInsert.copyWithCount(toInsert.getCount() - inserted);
			}
		}
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		this.validateSlotIndex(slot);
		if (amount == 0) {
			return ItemStack.EMPTY;
		} else {
			OversizedItemContainerContents contents = this.getContents();
			ItemStack existing = this.getStackFromContents(contents, slot);
			if (existing.isEmpty()) {
				return ItemStack.EMPTY;
			} else {
				int toExtract = Math.min(Math.min(amount, existing.getMaxStackSize()), existing.getCount() / getSlotUnitWorth.applyAsInt(slot));
				if (!simulate) {
					this.updateContents(contents, existing.copyWithCount(existing.getCount() - toExtract * getSlotUnitWorth.applyAsInt(slot)), slot);
				}

				return existing.copyWithCount(toExtract);
			}
		}
	}

	@Override
	public int getSlotLimit(int slot) {
		return getSlotLimit.apply(parent, slot);
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return isItemValid.test(slot, stack) && stack.getItem().canFitInsideContainerItems();
	}

	protected void onContentsChanged(int slot, ItemStack oldStack, ItemStack newStack) {
	}

	protected OversizedItemContainerContents getContents() {
		if (this.parent.has(this.component)) {
			return this.parent.get(this.component);
		}

		return OversizedItemContainerContents.fromSize(size);
	}

	protected ItemStack getStackFromContents(OversizedItemContainerContents contents, int slot) {
		this.validateSlotIndex(slot);
		return contents.getSlots() <= slot ? ItemStack.EMPTY : contents.getStackInSlot(slot);
	}

	protected void updateContents(OversizedItemContainerContents contents, ItemStack stack, int slot) {
		this.validateSlotIndex(slot);
		NonNullList<ItemStack> list = NonNullList.withSize(Math.max(contents.getSlots(), this.getSlots()), ItemStack.EMPTY);
		contents.copyInto(list);
		ItemStack oldStack = list.get(slot);
		list.set(slot, stack);
		this.parent.set(this.component, OversizedItemContainerContents.fromItems(list));
		this.onContentsChanged(slot, oldStack, stack);
	}

	protected final void validateSlotIndex(int slot) {
		if (slot < 0 || slot >= this.getSlots()) {
			throw new RuntimeException("Slot " + slot + " not in valid range - [0," + this.getSlots() + ")");
		}
	}
}
