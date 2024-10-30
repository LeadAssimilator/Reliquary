package reliquary.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import reliquary.init.ModEntities;
import reliquary.init.ModItems;
import reliquary.reference.Config;

import java.util.List;
import java.util.Optional;

public class LyssaHook extends FishingHook {
	public LyssaHook(EntityType<LyssaHook> entityType, Level level) {
		this(entityType, level, 0, 0);
	}

	private LyssaHook(EntityType<LyssaHook> entityType, Level level, int luck, int lureSpeed) {
		super(entityType, level, luck, lureSpeed);
	}

	public LyssaHook(Level level, Player fishingPlayer, int lureSpeed, int luck) {
		this(ModEntities.LYSSA_HOOK.get(), level, luck, lureSpeed);
		setOwner(fishingPlayer);
		shoot(fishingPlayer);

		//Reliquary
		speedUp();
	}

	private void shoot(Player fishingPlayer) {
		float f = fishingPlayer.getXRot();
		float f1 = fishingPlayer.getYRot();
		float f2 = Mth.cos(-f1 * ((float) Math.PI / 180F) - (float) Math.PI);
		float f3 = Mth.sin(-f1 * ((float) Math.PI / 180F) - (float) Math.PI);
		float f4 = -Mth.cos(-f * ((float) Math.PI / 180F));
		float f5 = Mth.sin(-f * ((float) Math.PI / 180F));
		double d0 = fishingPlayer.getX() - f3 * 0.3D;
		double d1 = fishingPlayer.getEyeY();
		double d2 = fishingPlayer.getZ() - f2 * 0.3D;
		moveTo(d0, d1, d2, f1, f);
		Vec3 vec3 = new Vec3(-f3, Mth.clamp(-(f5 / f4), -5.0F, 5.0F), -f2);
		double d3 = vec3.length();
		vec3 = vec3.multiply(0.6D / d3 + 0.5D + random.nextGaussian() * 0.0045D, 0.6D / d3 + 0.5D + random.nextGaussian() * 0.0045D, 0.6D / d3 + 0.5D + random.nextGaussian() * 0.0045D);
		setDeltaMovement(vec3);
		setYRot((float) (Mth.atan2(vec3.x, vec3.z) * (180F / (float) Math.PI)));
		setXRot((float) (Mth.atan2(vec3.y, vec3.horizontalDistance()) * (180F / (float) Math.PI)));
		yRotO = getYRot();
		xRotO = getXRot();
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		//much higher visible range than regular hook
		return distance < 16384;
	}

	public Optional<Player> getFishingPlayer() {
		Entity entity = getOwner();
		return Optional.ofNullable(entity instanceof Player player ? player : null);
	}

	private void speedUp() {
		//faster speed of the hook except for casting down

		if (getDeltaMovement().y() >= 0) {
			setDeltaMovement(getDeltaMovement().multiply(2, 2, 2));
		}
	}

	@Override
	public void tick() {
		super.tick();

		pullItemEntitiesTowardsHook();
	}

	@Override
	protected boolean shouldStopFishing(Player fishingPlayer) {
		ItemStack itemstack = fishingPlayer.getMainHandItem();
		ItemStack itemstack1 = fishingPlayer.getOffhandItem();
		boolean flag = itemstack.getItem() == ModItems.ROD_OF_LYSSA.get();
		boolean flag1 = itemstack1.getItem() == ModItems.ROD_OF_LYSSA.get();
		if (fishingPlayer.isAlive() && (flag || flag1) && distanceToSqr(fishingPlayer) <= 10240.0D) {
			return false;
		} else {
			discard();
			return true;
		}
	}

	private void pullItemEntitiesTowardsHook() {
		if (isAlive() && getHookedIn() == null) {
			float f = 0.0F;
			BlockPos blockpos = blockPosition();

			FluidState fluidState = level().getFluidState(blockpos);
			if (fluidState.is(FluidTags.WATER)) {
				f = fluidState.getHeight(level(), blockpos);
			}

			if (f <= 0F) {
				List<ItemEntity> list = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().expandTowards(getDeltaMovement()).inflate(3.0D));

				for (ItemEntity e : list) {
					Vec3 pullVector = new Vec3(getX() - e.getX(), getY() - e.getY(), getZ() - e.getZ()).normalize();
					e.setDeltaMovement(pullVector.multiply(0.4D, 0.4D, 0.4D));
				}
			}
		}
	}

	public void handleHookRetraction(ItemStack stack) {
		if (!level().isClientSide) {
			Entity hookedIn = getHookedIn();
			if (hookedIn != null && getFishingPlayer().map(Entity::isCrouching).orElse(false) && canStealFromEntity()) {
				stealFromLivingEntity();
				discard();
			} else {
				if (!level().isClientSide && getFishingPlayer().isPresent() && hookedIn != null) {
					bringInHookedEntity();
					level().broadcastEntityEvent(this, (byte) 31);
					discard();
				} else {
					super.retrieve(stack);
				}
			}

			pullItemEntitiesWithHook();
		}

	}

	private void bringInHookedEntity() {
		Entity hookedIn = getHookedIn();
		if (hookedIn != null) {
			super.pullEntity(hookedIn);
		}

		if (hookedIn instanceof ItemEntity) {
			hookedIn.setDeltaMovement(hookedIn.getDeltaMovement().multiply(4D, 4D, 4D));
		} else if (hookedIn instanceof LivingEntity) {
			hookedIn.setDeltaMovement(hookedIn.getDeltaMovement().multiply(1, 1.5D, 1));
		}
	}

	private boolean canStealFromEntity() {
		Entity hookedIn = getHookedIn();
		return hookedIn instanceof LivingEntity && (Config.COMMON.items.rodOfLyssa.stealFromPlayers.get() || !(hookedIn instanceof Player)) && Config.COMMON.items.rodOfLyssa.canStealFromEntity(hookedIn);
	}

	private void pullItemEntitiesWithHook() {
		List<ItemEntity> pullingItemsList = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().expandTowards(getDeltaMovement()).inflate(1.0D, 1.0D, 1.0D));

		getFishingPlayer().ifPresent(p -> {
			for (ItemEntity e : pullingItemsList) {
				double d1 = p.getX() - getX();
				double d3 = p.getY() - getY();
				double d5 = p.getZ() - getZ();
				double d7 = Math.sqrt(d1 * d1 + d3 * d3 + d5 * d5);
				double d9 = 0.1D;
				e.setDeltaMovement(d1 * d9, d3 * d9 + Math.sqrt(d7) * 0.08D, d5 * d9);
			}
		});
	}

	private void stealFromLivingEntity() {
		if (!(getHookedIn() instanceof LivingEntity livingEntity)) {
			return;
		}
		EquipmentSlot slotBeingStolenFrom = EquipmentSlot.values()[level().random.nextInt(EquipmentSlot.values().length)];

		ItemStack stolenStack = livingEntity.getItemBySlot(slotBeingStolenFrom);
		if (stolenStack.isEmpty() && Boolean.TRUE.equals(Config.COMMON.items.rodOfLyssa.stealFromVacantSlots.get())) {
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				stolenStack = livingEntity.getItemBySlot(slot);
				if (!stolenStack.isEmpty() && canDropFromSlot(livingEntity, slot)) {
					slotBeingStolenFrom = slot;
					break;
				}
			}
		}

		float failProbabilityFactor;

		Optional<Player> p = getFishingPlayer();

		if (p.isEmpty()) {
			return;
		}

		Player fishingPlayer = p.get();

		if (Boolean.TRUE.equals(Config.COMMON.items.rodOfLyssa.useLeveledFailureRate.get())) {
			failProbabilityFactor = 1F / ((float) Math.sqrt(Math.max(1, Math.min(fishingPlayer.experienceLevel, Config.COMMON.items.rodOfLyssa.levelCapForLeveledFormula.get()))) * 2);
		} else {
			failProbabilityFactor = Config.COMMON.items.rodOfLyssa.flatStealFailurePercentRate.get() / 100F;
		}

		if ((random.nextFloat() <= failProbabilityFactor || (stolenStack.isEmpty() && Config.COMMON.items.rodOfLyssa.failStealFromVacantSlots.get())) && Boolean.TRUE.equals(Config.COMMON.items.rodOfLyssa.angerOnStealFailure.get())) {
			livingEntity.hurt(damageSources().playerAttack(fishingPlayer), 0.0F);
			return;
		}
		if (!stolenStack.isEmpty() && level() instanceof ServerLevel serverLevel) {
			int randomItemDamage = level().random.nextInt(3);
			stolenStack.hurtAndBreak(randomItemDamage, serverLevel, livingEntity, e -> {
			});
			if (!stolenStack.isEmpty()) {
				ItemEntity entityitem = new ItemEntity(level(), getX(), getY(), getZ(), stolenStack);
				entityitem.setPickUpDelay(5);
				double d1 = fishingPlayer.getX() - getX();
				double d3 = fishingPlayer.getY() - getY();
				double d5 = fishingPlayer.getZ() - getZ();
				double d7 = Math.sqrt(d1 * d1 + d3 * d3 + d5 * d5);
				double d9 = 0.1D;
				entityitem.setDeltaMovement(d1 * d9, d3 * d9 + Math.sqrt(d7) * 0.08D, d5 * d9);
				level().addFreshEntity(entityitem);
			}

			livingEntity.setItemSlot(slotBeingStolenFrom, ItemStack.EMPTY);
		}
	}

	private boolean canDropFromSlot(LivingEntity entity, EquipmentSlot slot) {
		if (!(entity instanceof Mob mob)) {
			return true;
		}

		if (slot.getType() == EquipmentSlot.Type.HAND) {
			return mob.handDropChances[slot.getIndex()] > -1;
		} else {
			return mob.armorDropChances[slot.getIndex()] > -1;
		}
	}
}
