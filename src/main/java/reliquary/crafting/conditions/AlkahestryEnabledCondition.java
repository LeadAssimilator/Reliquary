package reliquary.crafting.conditions;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import reliquary.reference.Config;

public class AlkahestryEnabledCondition implements ICondition {
	private static final AlkahestryEnabledCondition INSTANCE = new AlkahestryEnabledCondition();
	public static final MapCodec<AlkahestryEnabledCondition> CODEC = MapCodec.unit(INSTANCE).stable();

	@Override
	public boolean test(IContext context) {
		return !Config.COMMON.disable.disableAlkahestry.get();
	}

	@Override
	public MapCodec<? extends ICondition> codec() {
		return CODEC;
	}

	@Override
	public String toString() {
		return "alkahestry_enabled";
	}
}
