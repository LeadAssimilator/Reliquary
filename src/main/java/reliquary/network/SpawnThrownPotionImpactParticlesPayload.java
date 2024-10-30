package reliquary.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.RandomSource;
import reliquary.Reliquary;

public record SpawnThrownPotionImpactParticlesPayload(int color, double posX, double posY,
													  double posZ) implements CustomPacketPayload {
	public static final Type<SpawnThrownPotionImpactParticlesPayload> TYPE = new Type<>(Reliquary.getRL("thrown_potion_impact_particles"));
	public static final StreamCodec<FriendlyByteBuf, SpawnThrownPotionImpactParticlesPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT,
			SpawnThrownPotionImpactParticlesPayload::color,
			ByteBufCodecs.DOUBLE,
			SpawnThrownPotionImpactParticlesPayload::posX,
			ByteBufCodecs.DOUBLE,
			SpawnThrownPotionImpactParticlesPayload::posY,
			ByteBufCodecs.DOUBLE,
			SpawnThrownPotionImpactParticlesPayload::posZ,
			SpawnThrownPotionImpactParticlesPayload::new);

	public static void handlePayload(SpawnThrownPotionImpactParticlesPayload payload) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}

		RandomSource rand = mc.level.random;

		float red = (((payload.color >> 16) & 255) / 256F);
		float green = (((payload.color >> 8) & 255) / 256F);
		float blue = ((payload.color & 255) / 256F);

		for (int i = 0; i < 100; ++i) {
			double var39 = rand.nextDouble() * 4.0D;
			double angle = rand.nextDouble() * Math.PI * 2.0D;
			double xSpeed = Math.cos(angle) * var39;
			double ySpeed = 0.01D + rand.nextDouble() * 0.5D;
			double zSpeed = Math.sin(angle) * var39;

			Particle particle = mc.particleEngine.createParticle(ParticleTypes.EFFECT, payload.posX + xSpeed * 0.1D, payload.posY + 0.3D, payload.posZ + zSpeed * 0.1D, xSpeed, ySpeed, zSpeed);
			if (particle != null) {
				float var32 = 0.75F + rand.nextFloat() * 0.25F;
				particle.setColor(red * var32, green * var32, blue * var32);
				particle.setPower((float) var39);
			}
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
