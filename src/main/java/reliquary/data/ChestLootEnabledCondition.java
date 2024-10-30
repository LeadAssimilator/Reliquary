package reliquary.data;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import reliquary.init.ModItems;
import reliquary.reference.Config;

public class ChestLootEnabledCondition implements LootItemCondition {
	private static final ChestLootEnabledCondition INSTANCE = new ChestLootEnabledCondition();
	public static final MapCodec<ChestLootEnabledCondition> CODEC = MapCodec.unit(INSTANCE);

	private ChestLootEnabledCondition() {
	}

	@Override
	public LootItemConditionType getType() {
		return ModItems.CHEST_LOOT_ENABLED_CONDITION.get();
	}

	@Override
	public boolean test(LootContext lootContext) {
		return Boolean.TRUE.equals(Config.COMMON.chestLootEnabled.get());
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder implements LootItemCondition.Builder {
		@Override
		public LootItemCondition build() {
			return new ChestLootEnabledCondition();
		}
	}
}
