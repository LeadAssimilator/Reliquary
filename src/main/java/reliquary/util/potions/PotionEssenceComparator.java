package reliquary.util.potions;

import net.minecraft.world.effect.MobEffectInstance;
import org.apache.commons.lang3.stream.Streams;

import java.util.Comparator;
import java.util.List;

public class PotionEssenceComparator implements Comparator<PotionEssence> {
	@Override
	public int compare(PotionEssence potionEssence1, PotionEssence potionEssence2) {

		int ret = 0;

		List<MobEffectInstance> effects1 = Streams.of(potionEssence1.getPotionContents().getAllEffects()).toList();
		List<MobEffectInstance> effects2 = Streams.of(potionEssence2.getPotionContents().getAllEffects()).toList();

		for (int i = 0; i < Math.min(effects1.size(), effects2.size()); i++) {
			ret = new EffectComparator().compare(effects1.get(i), effects2.get(i));
			if (ret != 0) {
				break;
			}
		}

		if (ret == 0) {
			ret = Integer.compare(effects1.size(), effects2.size());
		}

		return ret;
	}
}
