package reliquary.util;

import net.minecraft.util.RandomSource;

public class RandHelper {
	private RandHelper() {}

	@SuppressWarnings("squid:S1764") // this actually isn't a case of identical values being used as both side are random float value thus -1 to 1 as a result
	public static float getRandomMinusOneToOne(RandomSource rand) {
		return rand.nextFloat() - rand.nextFloat();
	}
}
