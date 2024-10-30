package reliquary.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ColorParticleOption;

import javax.annotation.Nullable;

public class CauldronBubbleParticle extends TextureSheetParticle {
	private static final int FRAMES_OF_EACH_POP_STAGE = 2;
	private static final int POP_STAGES = 3;
	private final SpriteSet spriteSet;

	private CauldronBubbleParticle(ClientLevel level, ColorParticleOption particleOption, double x, double y, double z, SpriteSet spriteSet) {
		super(level, x, y, z, 0D, 0D, 0D);
		this.spriteSet = spriteSet;
		setSize(0.02F, 0.02F);
		quadSize = 0.5F + (level.random.nextFloat() - 0.5F) * 0.4F;
		xd = 0D;
		yd = 0D;
		zd = 0D;
		rCol = particleOption.getRed();
		gCol = particleOption.getGreen();
		bCol = particleOption.getBlue();
		lifetime = 20;
		age = 0;
	}

	@Override
	public void tick() {
		xo = x;
		yo = y;
		zo = z;
		setAlpha((float) age / (float) lifetime);

		if (age++ >= lifetime) {
			remove();
		} else {
			setSpriteFromAge(spriteSet);
		}
	}

	@Override
	public float getQuadSize(float partialTicks) {
		return 0.1F * quadSize * (1.0F + age / 20F);
	}

	@Override
	public void setSpriteFromAge(SpriteSet animatedSprite) {
		int ageFromDeath = lifetime - age;
		if (ageFromDeath <= FRAMES_OF_EACH_POP_STAGE * POP_STAGES) {
			setSprite(animatedSprite.get(4 - Math.max(ageFromDeath / FRAMES_OF_EACH_POP_STAGE, 1), POP_STAGES));
		} else {
			setSprite(animatedSprite.get(0, lifetime));
		}
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
	}

	public static class Provider implements ParticleProvider<ColorParticleOption> {
		private final SpriteSet spriteSet;

		public Provider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}

		@Nullable
		@Override
		public Particle createParticle(ColorParticleOption particleData, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			CauldronBubbleParticle particle = new CauldronBubbleParticle(level, particleData, x, y, z, spriteSet);
			particle.setSprite(spriteSet.get(particle.age, particle.lifetime));
			return particle;
		}
	}
}
