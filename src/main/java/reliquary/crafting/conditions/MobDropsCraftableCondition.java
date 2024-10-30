package reliquary.crafting.conditions;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import reliquary.reference.Config;

public class MobDropsCraftableCondition implements ICondition {
	private static final MobDropsCraftableCondition INSTANCE = new MobDropsCraftableCondition();
	public static final MapCodec<MobDropsCraftableCondition> CODEC = MapCodec.unit(INSTANCE).stable();

	@Override
	public boolean test(IContext context) {
		return Config.COMMON.dropCraftingRecipesEnabled.get();
	}

	@Override
	public MapCodec<? extends ICondition> codec() {
		return CODEC;
	}
}
