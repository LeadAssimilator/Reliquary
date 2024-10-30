package reliquary.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.IShearable;
import reliquary.entities.ReliquaryFakePlayer;
import reliquary.util.FakePlayerFactory;
import reliquary.util.RandHelper;
import reliquary.util.TooltipBuilder;

import java.util.List;
import java.util.function.Consumer;

public class ShearsOfWinterItem extends ShearsItem implements ICreativeTabItemGenerator {
	public ShearsOfWinterItem() {
		super(new Properties().component(DataComponents.UNBREAKABLE, new Unbreakable(true)));
	}

	@Override
	public void addCreativeTabItems(Consumer<ItemStack> itemConsumer) {
		itemConsumer.accept(new ItemStack(this));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();
		if (block instanceof BeehiveBlock) {
			shearBeehive(level, pos, state, context.getClickLocation(), context.getClickedFace());
		}
		return super.useOn(context);
	}

	private void shearBeehive(Level level, BlockPos pos, BlockState state, Vec3 hitVec, Direction face) {
		if (!(level instanceof ServerLevel)) {
			return;
		}

		ItemStack fakeShears = new ItemStack(Items.SHEARS);
		ReliquaryFakePlayer fakePlayer = FakePlayerFactory.get((ServerLevel) level);
		fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, fakeShears);
		state.useItemOn(fakeShears, level, fakePlayer, InteractionHand.MAIN_HAND, new BlockHitResult(hitVec, face, pos, false));
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity livingEntity) {
		return 2500;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BLOCK;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		player.startUsingItem(hand);
		return new InteractionResultHolder<>(InteractionResult.SUCCESS, player.getItemInHand(hand));
	}

	@Override
	public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
		//start the blizzard after a short delay, this prevents some abuse.
		if (getUseDuration(stack, livingEntity) - remainingUseDuration <= 5 || !(livingEntity instanceof Player player)) {
			return;
		}

		Vec3 lookVector = player.getLookAngle();
		spawnBlizzardParticles(lookVector, player);

		if (livingEntity.level().isClientSide) {
			return;
		}

		doEntityShearableCheck(stack, player, lookVector);
		shearBlocks(player, lookVector);
	}

	private void shearBlocks(Player player, Vec3 lookVector) {
		Vec3 eyePosition = player.getEyePosition(1);
		BlockPos firstPos = BlockPos.containing(eyePosition);
		BlockPos secondPos = BlockPos.containing(eyePosition.add(lookVector.multiply(10, 10, 10)));
		if (firstPos.getX() == secondPos.getX()) {
			firstPos = firstPos.offset(-2, 0, 0);
			secondPos = secondPos.offset(2, 0, 0);
		}
		if (firstPos.getY() == secondPos.getY()) {
			firstPos = firstPos.offset(0, -2, 0);
			secondPos = secondPos.offset(0, 2, 0);
		}
		if (firstPos.getZ() == secondPos.getZ()) {
			firstPos = firstPos.offset(0, 0, -2);
			secondPos = secondPos.offset(0, 0, 2);
		}

		BlockPos.betweenClosedStream(firstPos, secondPos)
				.forEach(pos -> checkAndShearBlockAt(player, pos));
	}

	@Override
	public void appendHoverText(ItemStack shears, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		TooltipBuilder.of(tooltip, context).itemTooltip(this);
	}

	private void checkAndShearBlockAt(Player player, BlockPos pos) {
		int distance = (int) Math.sqrt(pos.distToLowCornerSqr(player.getX(), player.getY(), player.getZ()));
		int probabilityFactor = 5 + distance;
		//chance of block break diminishes over distance
		if (player.level().random.nextInt(probabilityFactor) == 0) {
			shearBlockAt(pos, player);
		}
	}

	private void shearBlockAt(BlockPos pos, Player player) {
		Level level = player.level();
		BlockState blockState = level.getBlockState(pos);
		Block block = blockState.getBlock();
		if (block instanceof IShearable target) {
			ItemStack dummyShears = new ItemStack(Items.SHEARS);
			if (target.isShearable(player, dummyShears, level, pos) && removeBlock(player, pos, blockState.canHarvestBlock(level, pos, player))) {
				player.awardStat(Stats.BLOCK_MINED.get(block));
				player.causeFoodExhaustion(0.01F);
				Block.dropResources(blockState, level, pos, null, player, dummyShears);
			}
		} else if (block instanceof BeehiveBlock) {
			shearBeehive(level, pos, blockState, Vec3.ZERO, Direction.UP);
		}
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity livingEntity) {
		if (livingEntity instanceof Player player) {
			shearBlockAt(pos, player);
		}
		return super.mineBlock(stack, level, state, pos, livingEntity);
	}

	private boolean removeBlock(Player player, BlockPos pos, boolean canHarvest) {
		BlockState state = player.level().getBlockState(pos);
		boolean removed = state.onDestroyedByPlayer(player.level(), pos, player, canHarvest, player.level().getFluidState(pos));
		if (removed) {
			state.getBlock().destroy(player.level(), pos, state);
		}
		return removed;
	}

	private void doEntityShearableCheck(ItemStack stack, Player player, Vec3 lookVector) {
		if (player.level().isClientSide) {
			return;
		}
		double lowerX = Math.min(player.getX(), player.getX() + lookVector.x * 10D);
		double lowerY = Math.min(player.getY() + player.getEyeHeight(), player.getY() + player.getEyeHeight() + lookVector.y * 10D);
		double lowerZ = Math.min(player.getZ(), player.getZ() + lookVector.z * 10D);
		double upperX = Math.max(player.getX(), player.getX() + lookVector.x * 10D);
		double upperY = Math.max(player.getY() + player.getEyeHeight(), player.getY() + player.getEyeHeight() + lookVector.y * 10D);
		double upperZ = Math.max(player.getZ(), player.getZ() + lookVector.z * 10D);
		List<Mob> eList = player.level().getEntitiesOfClass(Mob.class, new AABB(lowerX, lowerY, lowerZ, upperX, upperY, upperZ));
		RandomSource rand = player.level().random;
		for (Mob e : eList) {
			int distance = (int) player.distanceTo(e);
			int probabilityFactor = (distance - 3) / 2;
			if (probabilityFactor > 0 && player.level().random.nextInt(probabilityFactor) != 0) {
				continue;
			}
			if (!e.is(player)) {
				e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1));
			}
			if (e instanceof IShearable) {
				shearEntity(stack, player, rand, e);
			}
		}
	}

	private void shearEntity(ItemStack stack, Player player, RandomSource rand, Mob e) {
		IShearable target = (IShearable) e;
		BlockPos pos = e.blockPosition();
		if (target.isShearable(player, new ItemStack(Items.SHEARS), e.level(), pos)) {
			List<ItemStack> drops = target.onSheared(player, stack, e.level(), pos);
			drops.forEach(d -> {
				ItemEntity ent = e.spawnAtLocation(d, 1.0F);
				if (ent != null) {
					ent.setDeltaMovement(ent.getDeltaMovement().add(RandHelper.getRandomMinusOneToOne(rand) * 0.1F, rand.nextFloat() * 0.05F, RandHelper.getRandomMinusOneToOne(rand) * 0.1F));
				}
			});

			player.causeFoodExhaustion(0.01F);
		}
	}

	private void spawnBlizzardParticles(Vec3 lookVector, Player player) {
		BlockParticleOption blockParticleData = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.defaultBlockState());

		for (int i = 0; i < 16; ++i) {
			float randX = 10F * (player.level().random.nextFloat() - 0.5F);
			float randY = 10F * (player.level().random.nextFloat() - 0.5F);
			float randZ = 10F * (player.level().random.nextFloat() - 0.5F);

			player.level().addParticle(blockParticleData, player.getX() + randX, player.getY() + randY, player.getZ() + randZ, lookVector.x * 5, lookVector.y * 5, lookVector.z * 5);
		}
	}

}
