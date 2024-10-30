package reliquary.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

public class PlayerInventoryHandler {
	public static final Set<String> SINGLE_IDENTIFIER = Collections.singleton("");
	private final Supplier<Set<String>> identifiersGetter;
	private final SlotCountGetter slotCountGetter;
	private final SlotStackGetter slotStackGetter;

	public PlayerInventoryHandler(Supplier<Set<String>> identifiersGetter, SlotCountGetter slotCountGetter, SlotStackGetter slotStackGetter) {
		this.identifiersGetter = identifiersGetter;
		this.slotCountGetter = slotCountGetter;
		this.slotStackGetter = slotStackGetter;
	}

	public int getSlotCount(Player player, String identifier) {
		return slotCountGetter.getSlotCount(player, identifier);
	}

	public ItemStack getStackInSlot(Player player, String identifier, int slot) {
		return slotStackGetter.getStackInSlot(player, identifier, slot);
	}

	public Set<String> getIdentifiers() {
		return identifiersGetter.get();
	}

	public interface SlotCountGetter {
		int getSlotCount(Player player, String identifier);
	}

	public interface SlotStackGetter {
		ItemStack getStackInSlot(Player player, String identifier, int slot);
	}

}
