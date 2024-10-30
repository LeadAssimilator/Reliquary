package reliquary.data;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import reliquary.init.ModItems;
import reliquary.reference.Config;

public class EntityLootEnabledCondition implements LootItemCondition {
	private static final EntityLootEnabledCondition INSTANCE = new EntityLootEnabledCondition();
	public static final MapCodec<EntityLootEnabledCondition> CODEC = MapCodec.unit(() -> INSTANCE);

	private EntityLootEnabledCondition() {
	}

	@Override
	public LootItemConditionType getType() {
		return ModItems.ENTITY_LOOT_ENABLED_CONDITION.get();
	}

	@Override
	public boolean test(LootContext lootContext) {
		return Boolean.TRUE.equals(Config.COMMON.mobDropsEnabled.get());
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder implements LootItemCondition.Builder {
		@Override
		public LootItemCondition build() {
			return new EntityLootEnabledCondition();
		}
	}
}
