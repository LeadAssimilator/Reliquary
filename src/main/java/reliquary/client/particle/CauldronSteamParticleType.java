package reliquary.client.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import reliquary.client.init.ModParticles;

public class CauldronSteamParticleType extends ParticleType<ColorParticleOption> {
	public CauldronSteamParticleType() {
		super(false);
	}

	@Override
	public MapCodec<ColorParticleOption> codec() {
		return ColorParticleOption.codec(ModParticles.CAULDRON_STEAM.get());
	}

	@Override
	public StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec() {
		return ColorParticleOption.streamCodec(ModParticles.CAULDRON_STEAM.get());
	}
}
