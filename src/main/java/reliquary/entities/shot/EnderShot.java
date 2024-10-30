package reliquary.entities.shot;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import reliquary.init.ModEntities;
import reliquary.reference.ClientReference;

public class EnderShot extends ShotBase {
	public EnderShot(EntityType<EnderShot> entityType, Level level) {
		super(entityType, level);
	}

	public EnderShot(Level level, Player player, InteractionHand hand) {
		super(ModEntities.ENDER_SHOT.get(), level, player, hand);
	}

	private void doPortalExplosion() {
		for (int particles = 0; particles < 3; particles++) {
			spawnMotionBasedParticle(ParticleTypes.PORTAL, getY() - 1);
			level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0, 0);
		}
		discard();
	}

	@Override
	void doBurstEffect(Direction sideHit) {
		// there aren't any burst effects because ender shots pass through
		// everything.
	}

	@Override
	void doFiringEffects() {
		level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.5F, 0.5F, 0.5F), getX() + smallGauss(0.1D), getY() + smallGauss(0.1D), getZ() + smallGauss(0.1D), 0, 0, 0);
		spawnMotionBasedParticle(ParticleTypes.FLAME);
	}

	@Override
	protected void onHit(HitResult result) {
		if (result.getType() == HitResult.Type.ENTITY) {
			Entity entityHit = ((EntityHitResult) result).getEntity();
			if (entityHit == getOwner() || !(entityHit instanceof LivingEntity)) {
				return;
			}
			onImpact((LivingEntity) entityHit);
		}
		// note that there is no tile impact.
	}

	@Override
	protected void onImpact(LivingEntity livingEntity) {
		if (livingEntity != getOwner() || ticksInAir > 3) {
			doDamage(livingEntity);
		}
		spawnHitParticles(8);

		//continues after hit and thus no setting dead here
	}

	@Override
	protected void groundImpact(Direction sideHit) {
		//do absolutely nothing. this avoids a death sentence.
	}

	@Override
	void doFlightEffects() {
		if (ticksInAir % 3 == 0) {
			spawnMotionBasedParticle(ParticleTypes.PORTAL, getY() - 1);
		}

		// housed in the base class
		seekTarget();

		// these only last 5 seconds, to prevent them from killing too terribly
		// much.
		if (ticksInAir > 100) {
			doPortalExplosion();
		}
	}

	@Override
	void spawnHitParticles(int i) {
		for (int particles = 0; particles < i; particles++) {
			spawnMotionBasedParticle(ParticleTypes.WITCH);
		}
	}

	@Override
	int getRicochetMax() {
		return 0;
	}

	@Override
	int getDamageOfShot(LivingEntity mop) {
		return 16 + d12();
	}

	@Override
	public ResourceLocation getShotTexture() {
		return ClientReference.ENDER;
	}
}
