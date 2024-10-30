package reliquary.entities.shot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import reliquary.init.ModEntities;
import reliquary.reference.ClientReference;

public class StormShot extends ShotBase {
	public StormShot(EntityType<StormShot> entityType, Level level) {
		super(entityType, level);
	}

	public StormShot(Level level, Player player, InteractionHand hand) {
		super(ModEntities.STORM_SHOT.get(), level, player, hand);
	}

	@Override
	void doFiringEffects() {
		level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.5F, 0.5F, 0.5F), getX() + smallGauss(0.1D), getY() + smallGauss(0.1D), getZ() + smallGauss(0.1D), 0, 0, 0);
		spawnMotionBasedParticle(ParticleTypes.FLAME);
	}

	@Override
	void doFlightEffects() {
		// does nothing
	}

	@Override
	protected void onHit(HitResult result) {
		if (result.getType() == HitResult.Type.BLOCK) {
			BlockHitResult blockResult = (BlockHitResult) result;
			BlockPos pos = blockResult.getBlockPos().relative(blockResult.getDirection());
			if (level() instanceof ServerLevel && level().isRainingAt(pos) && level().getLevelData().isRaining() && level().getLevelData().isThundering()) {
				LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level());
				if (bolt != null) {
					bolt.moveTo(pos.getX(), pos.getY(), pos.getZ());
					level().addFreshEntity(bolt);
				}
			}
		}
		super.onHit(result);
	}

	@Override
	void doBurstEffect(Direction sideHit) {
		// does nothing
	}

	@Override
	void spawnHitParticles(int i) {
		Vec3 motion = getDeltaMovement();
		for (int particles = 0; particles < i; particles++) {
			level().addParticle(ParticleTypes.BUBBLE, getX(), getY(), getZ(), gaussian(motion.x()), random.nextFloat() + motion.y(), gaussian(motion.z()));
		}
	}

	@Override
	int getRicochetMax() {
		return 1;
	}

	@Override
	void doDamage(LivingEntity entity) {
		if (level() instanceof ServerLevel serverLevel && level().isRainingAt(entity.blockPosition()) && level().getLevelData().isRaining() && level().getLevelData().isThundering()) {
			LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level());
			if (bolt != null) {
				bolt.moveTo(entity.getX(), entity.getY(), entity.getZ());
				level().addFreshEntity(bolt);
				if (entity instanceof Creeper) {
					entity.thunderHit(serverLevel, bolt);
				}
			}
		}
		super.doDamage(entity);
	}

	@Override
	int getDamageOfShot(LivingEntity entity) {
		float f = 1F + (level().isRaining() ? 0.5F : 0F) + (level().isThundering() ? 0.5F : 0F);
		return Math.round(9F * f) + d6();
	}

	@Override
	public ResourceLocation getShotTexture() {
		return ClientReference.STORM;
	}
}
