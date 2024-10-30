package reliquary.crafting.conditions;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import reliquary.reference.Config;

public class PassivePedestalEnabledCondition implements ICondition {
	private static final PassivePedestalEnabledCondition INSTANCE = new PassivePedestalEnabledCondition();
	public static final MapCodec<PassivePedestalEnabledCondition> CODEC = MapCodec.unit(INSTANCE).stable();

	@Override
	public boolean test(IContext context) {
		return !Config.COMMON.disable.disablePassivePedestal.get();
	}

	@Override
	public MapCodec<? extends ICondition> codec() {
		return CODEC;
	}
}
