package reliquary.entities.shot;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import reliquary.entities.ConcussiveExplosion;
import reliquary.init.ModEntities;
import reliquary.reference.ClientReference;

public class SandShot extends ShotBase {
	public SandShot(EntityType<SandShot> entityType, Level level) {
		super(entityType, level);
	}

	public SandShot(Level level, Player player, InteractionHand hand) {
		super(ModEntities.SAND_SHOT.get(), level, player, hand);
	}

	@Override
	void doFiringEffects() {
		level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.5F, 0.5F, 0.5F), getX() + smallGauss(0.1D), getY() + smallGauss(0.1D), getZ() + smallGauss(0.1D), 0, 0, 0);
		spawnMotionBasedParticle(ParticleTypes.FLAME);
	}

	@Override
	void doFlightEffects() {
		if (ticksInAir % 3 == 0) {
			spawnHitParticles(1);
		}
	}

	@Override
	void doBurstEffect(Direction sideHit) {
		spawnHitParticles(8);
	}

	@Override
	void spawnHitParticles(int i) {
		for (int particles = 0; particles < i; particles++) {
			level().addParticle(DustParticleOptions.REDSTONE, getX(), getY(), getZ(), 0.7F, 0.7F, 0.3F);
		}
	}

	@Override
	int getDamageOfShot(LivingEntity e) {
		// creepers turn sand shots into straight explosions.
		if (e instanceof Creeper) {
			ConcussiveExplosion.customBusterExplosion(this, getX(), getY(), getZ(), 2.0F);
			getShooterPlayer().ifPresent(player -> e.hurt(damageSources().playerAttack(player), 20));
			return 0;
		}
		// it also causes blinding
		if (e instanceof Mob) {
			e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 1));
		}
		return (level().getLevelData().isRaining() ? 4 : 8) + d6();
	}

	@Override
	int getRicochetMax() {
		return 0;
	}

	@Override
	public ResourceLocation getShotTexture() {
		return ClientReference.SAND;
	}
}
