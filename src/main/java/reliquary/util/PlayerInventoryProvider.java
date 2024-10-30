package reliquary.util;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PlayerInventoryProvider {
	public static final String MAIN_INVENTORY = "main";
	public static final String OFFHAND_INVENTORY = "offhand";
	public static final String ARMOR_INVENTORY = "armor";

	private final Map<String, PlayerInventoryHandler> playerInventoryHandlers = new LinkedHashMap<>();
	private final List<String> renderedHandlers = new ArrayList<>();

	private static final PlayerInventoryProvider serverProvider = new PlayerInventoryProvider();
	private static final PlayerInventoryProvider clientProvider = new PlayerInventoryProvider();

	public static PlayerInventoryProvider get() {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			return clientProvider;
		} else {
			return serverProvider;
		}
	}

	private PlayerInventoryProvider() {
		addPlayerInventoryHandler(MAIN_INVENTORY, () -> PlayerInventoryHandler.SINGLE_IDENTIFIER, (player, identifier) -> player.getInventory().items.size(),
				(player, identifier, slot) -> player.getInventory().items.get(slot), false);
		addPlayerInventoryHandler(OFFHAND_INVENTORY, () -> PlayerInventoryHandler.SINGLE_IDENTIFIER, (player, identifier) -> player.getInventory().offhand.size(),
				(player, identifier, slot) -> player.getInventory().offhand.get(slot), false);
		addPlayerInventoryHandler(ARMOR_INVENTORY, () -> PlayerInventoryHandler.SINGLE_IDENTIFIER, (player, identifier) -> 1,
				(player, identifier, slot) -> player.getInventory().armor.get(EquipmentSlot.CHEST.getIndex()), true);
	}

	public void addPlayerInventoryHandler(String name, Supplier<Set<String>> identifiersGetter, PlayerInventoryHandler.SlotCountGetter slotCountGetter, PlayerInventoryHandler.SlotStackGetter slotStackGetter, boolean rendered) {
		Map<String, PlayerInventoryHandler> temp = new LinkedHashMap<>(playerInventoryHandlers);
		playerInventoryHandlers.clear();
		playerInventoryHandlers.put(name, new PlayerInventoryHandler(identifiersGetter, slotCountGetter, slotStackGetter));
		playerInventoryHandlers.putAll(temp);

		if (rendered) {
			ArrayList<String> tempRendered = new ArrayList<>(renderedHandlers);
			renderedHandlers.clear();
			renderedHandlers.add(name);
			renderedHandlers.addAll(tempRendered);
		}
	}

	private Map<String, PlayerInventoryHandler> getPlayerInventoryHandlers() {
		return playerInventoryHandlers;
	}

	public void runOnPlayerInventoryHandlers(Player player, Consumer<ItemStack> run) {
		getFromPlayerInventoryHandlers(player, (stack, result) -> {
			run.accept(stack);
			return result;
		}, result -> false, () -> true);
	}

	public <T> T getFromPlayerInventoryHandlers(Player player, BiFunction<ItemStack, T, T> get, Predicate<T> shouldExit, Supplier<T> defaultValue) {
		T result = defaultValue.get();
		for (var handler : playerInventoryHandlers.values()) {
			Set<String> identifiers = handler.getIdentifiers();
			for (String identifier : identifiers) {
				int slots = handler.getSlotCount(player, identifier);
				for (int slot = 0; slot < slots; slot++) {
					result = get.apply(handler.getStackInSlot(player, identifier, slot), result);
					if (shouldExit.test(result)) {
						return result;
					}
				}
			}
		}
		return result;
	}
}
