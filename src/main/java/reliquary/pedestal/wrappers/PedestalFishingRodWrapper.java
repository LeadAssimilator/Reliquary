package reliquary.pedestal.wrappers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import reliquary.api.IPedestal;
import reliquary.api.IPedestalActionItemWrapper;
import reliquary.entities.ReliquaryFakePlayer;
import reliquary.network.PedestalFishHookPayload;
import reliquary.reference.Config;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PedestalFishingRodWrapper implements IPedestalActionItemWrapper {
	private static final int RANGE = 4;
	private static final int NO_WATER_COOLDOWN = 100;
	private static final int BAD_THROW_TIMEOUT = 60;
	private static final int ABSOLUTE_TIMEOUT = 1200;

	private ReliquaryFakePlayer fakePlayer;
	private boolean badThrowChecked;
	private int ticksSinceLastThrow;
	private boolean retractFail = false;

	@Override
	public void update(ItemStack stack, Level level, IPedestal pedestal) {
		ticksSinceLastThrow++;

		if (fakePlayer != null && fakePlayer.fishing != null) {
			handleHookStates(stack, level, pedestal);
			syncHookData(fakePlayer.serverLevel(), pedestal);
		} else {
			setupFakePlayer(level, pedestal.getBlockPosition());
			Optional<BlockPos> p = getBestWaterBlock(level, pedestal);
			if (p.isPresent()) {
				updateHeldItem(stack);
				setPitchYaw(p.get());
				spawnFishHook(level, pedestal);
				badThrowChecked = false;
				ticksSinceLastThrow = 0;
			} else {
				pedestal.setActionCoolDown(NO_WATER_COOLDOWN);
			}
		}
	}

	private void handleHookStates(ItemStack stack, Level level, IPedestal pedestal) {
		if (retractFail) {
			//take care of failed retract
			if (fakePlayer != null && fakePlayer.fishing != null && fakePlayer.fishing.nibble == 0) {
				retractHook(pedestal, stack);
				retractFail = false;
			}
		} else if (!badThrowChecked && ticksSinceLastThrow > BAD_THROW_TIMEOUT) {
			//when hook doesn't land in water retract it after some time
			FishingHook fishingHook = fakePlayer.fishing;
			//noinspection ConstantConditions
			if (fishingHook.currentState != FishingHook.FishHookState.BOBBING) {
				retractHook(pedestal, stack);
			} else {
				badThrowChecked = true;
			}
		} else if (ticksSinceLastThrow > ABSOLUTE_TIMEOUT) {
			//sometimes hook can get stuck in a bad state so take care of that
			retractHook(pedestal, stack);
		} else //noinspection ConstantConditions
			if (fakePlayer.fishing.nibble > 0 || fakePlayer.fishing.getHookedIn() != null) {
				if (level.random.nextInt(100) <= Config.COMMON.blocks.pedestal.fishingWrapperSuccessRate.get()) {
					retractHook(pedestal, stack);
				} else {
					retractFail = true;
				}
			}
	}

	private void updateHeldItem(ItemStack fishingRod) {
		ItemStack heldItem = fakePlayer.getMainHandItem();
		if (heldItem.isEmpty()) {
			fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, fishingRod);
			return;
		}
		if (heldItem.getItem() != fishingRod.getItem()) {
			fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, fishingRod);
		}
	}

	private void retractHook(IPedestal pedestal, ItemStack stack) {
		//noinspection ConstantConditions
		int i = fakePlayer.fishing.retrieve(stack);
		fakePlayer.fishing = null;
		stack.hurtAndBreak(i, fakePlayer.serverLevel(), fakePlayer, item -> {});
		//destroy the item when it gets used up
		if (stack.getCount() == 0) {
			pedestal.destroyItem();
		}

		pedestal.setActionCoolDown(Config.COMMON.blocks.pedestal.fishingWrapperRetractDelay.get() * 20);
	}

	private Optional<BlockPos> getBestWaterBlock(Level level, IPedestal pedestal) {
		List<List<BlockPos>> connectedGroups = new ArrayList<>();
		List<BlockPos> visitedBlocks = new ArrayList<>();

		BlockPos pos = pedestal.getBlockPosition();
		int pedestalX = pos.getX();
		int pedestalY = pos.getY();
		int pedestalZ = pos.getZ();

		BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos(pedestalX, pedestalY, pedestalZ);
		for (int y = pedestalY - RANGE; y < pedestalY; y++) {
			checkPos.setY(y);
			for (int r = 1; r <= RANGE; r++) {
				int x = pedestalX - r;
				int z = pedestalZ - r;
				checkPos.setZ(z);
				while (x <= pedestalX + r) {
					checkPos.setX(x);
					checkForAndAddWaterBlocks(level, pedestal, visitedBlocks, connectedGroups, pos, checkPos);
					x++;
				}
				x--;

				while (z <= pedestalZ + r) {
					checkPos.setZ(z);
					checkForAndAddWaterBlocks(level, pedestal, visitedBlocks, connectedGroups, pos, checkPos);
					z++;
				}
				z--;

				while (x >= pedestalX - r) {
					checkPos.setX(x);
					checkForAndAddWaterBlocks(level, pedestal, visitedBlocks, connectedGroups, pos, checkPos);
					x--;
				}

				while (z >= pedestalZ - r) {
					checkPos.setZ(z);
					checkForAndAddWaterBlocks(level, pedestal, visitedBlocks, connectedGroups, pos, checkPos);
					z--;
				}
			}
		}

		return getClosestBlock(connectedGroups, pedestalX, pedestalY, pedestalZ);
	}

	private Optional<BlockPos> getClosestBlock(List<List<BlockPos>> connectedGroups, int pedestalX, int pedestalY, int pedestalZ) {
		BlockPos closestBlockInLargestGroup = null;
		int closestSqDistance = Integer.MAX_VALUE;
		int mostBlocks = 0;
		for (List<BlockPos> group : connectedGroups) {
			if (group.size() > mostBlocks) {
				mostBlocks = group.size();
				for (BlockPos waterPos : group) {
					int xDiff = waterPos.getX() - pedestalX;
					int yDiff = waterPos.getY() - pedestalY;
					int zDiff = waterPos.getZ() - pedestalZ;

					int sqDistance = xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;

					if (sqDistance < closestSqDistance) {
						closestSqDistance = sqDistance;
						closestBlockInLargestGroup = waterPos;
					}
				}
			}
		}
		return Optional.ofNullable(closestBlockInLargestGroup);
	}

	private void checkForAndAddWaterBlocks(Level level, IPedestal pedestal, List<BlockPos> visitedBlocks, List<List<BlockPos>> connectedGroups, BlockPos pedestalPos, BlockPos checkPos) {
		if (!visitedBlocks.contains(checkPos)) {
			List<BlockPos> group = new ArrayList<>();
			checkForWaterAndSearchNeighbors(level, pedestal, visitedBlocks, pedestalPos, checkPos, group);
			if (!group.isEmpty()) {
				connectedGroups.add(group);
			}
		}
	}

	private void checkForWaterAndSearchNeighbors(Level level, IPedestal pedestal, List<BlockPos> visitedBlocks, BlockPos pedestalPos, BlockPos blockPos, List<BlockPos> group) {
		visitedBlocks.add(blockPos.immutable());
		BlockState blockState = level.getBlockState(blockPos);
		if (blockState.getBlock() == Blocks.WATER) {
			int x = blockPos.getX();
			int y = blockPos.getY();
			int z = blockPos.getZ();

			double startX = fakePlayer.getX();
			double startY = fakePlayer.getY();
			double startZ = fakePlayer.getZ();

			//make sure that the fakePlayer can see the block
			BlockHitResult raytraceresult = level.clip(
					new ClipContext(new Vec3(startX, startY, startZ), new Vec3(x + 0.5D, y + 0.8D, z + 0.5D), ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY, fakePlayer));
			if (raytraceresult.getType() != HitResult.Type.MISS && raytraceresult.getBlockPos().equals(blockPos)) {
				group.add(blockPos);
				for (Direction direction : Direction.Plane.HORIZONTAL) {
					BlockPos neighborPos = blockPos.relative(direction);
					//no search outside of the range
					if (neighborPos.getX() <= pedestalPos.getX() + RANGE && neighborPos.getX() >= pedestalPos.getX() - RANGE && neighborPos.getY() <= pedestalPos.getY() + RANGE && neighborPos.getY() >= pedestalPos.getY() - RANGE) {
						addNeighboringWater(level, pedestal, visitedBlocks, group, pedestalPos, neighborPos);
					}
				}
			}
		}
	}

	private void addNeighboringWater(Level level, IPedestal pedestal, List<BlockPos> visitedBlocks, List<BlockPos> group, BlockPos pedestalPos, BlockPos blockPos) {
		if (!visitedBlocks.contains(blockPos)) {
			checkForWaterAndSearchNeighbors(level, pedestal, visitedBlocks, pedestalPos, blockPos, group);
		}
	}

	private void spawnFishHook(Level level, IPedestal pedestal) {
		level.playSound(null, pedestal.getBlockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (level.random.nextFloat() * 0.4F + 0.8F));

		level.addFreshEntity(new FishingHook(fakePlayer, level, 0, 0) {
			@Nullable
			@Override
			public Entity getOwner() {
				return fakePlayer;
			}

			@Override
			public boolean broadcastToPlayer(ServerPlayer player) {
				return false;
			}
		});
	}

	private void syncHookData(ServerLevel level, IPedestal pedestal) {
		FishingHook hook = fakePlayer.fishing;
		BlockPos pedestalPos = pedestal.getBlockPosition();

		if (hook == null) {
			PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pedestalPos),
					new PedestalFishHookPayload(pedestal.getBlockPosition(), -1, -1, -1));
		} else {
			PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pedestalPos),
					new PedestalFishHookPayload(pedestal.getBlockPosition(), hook.getX(), hook.getY(), hook.getZ()));
		}
	}

	@Override
	public void onRemoved(ItemStack stack, Level level, IPedestal pedestal) {
		if (fakePlayer != null && fakePlayer.fishing != null) {
			fakePlayer.fishing.discard();
		}
	}

	@Override
	public void stop(ItemStack stack, Level level, IPedestal pedestal) {
		if (fakePlayer != null && fakePlayer.fishing != null) {
			fakePlayer.fishing.discard();
			syncHookData(fakePlayer.serverLevel(), pedestal);
		}
	}

	private void setupFakePlayer(Level level, BlockPos pos) {
		if (fakePlayer == null) {
			fakePlayer = new ReliquaryFakePlayer((ServerLevel) level);
			fakePlayer.setPos(pos.getX() + 0.5, (double) pos.getY() + 2, pos.getZ() + 0.5);
		}
	}

	private void setPitchYaw(BlockPos pos) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();

		double degree = 180 / Math.PI;
		double dx = fakePlayer.getX() - (x + 0.5);
		double dy = fakePlayer.getY() - y;
		double dz = fakePlayer.getZ() - (z + 0.5);
		fakePlayer.setYRot((float) -((Math.atan2(dx, dz) * degree) + 180));
		fakePlayer.setXRot((float) (Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * degree));
	}

	@Override
	public Optional<Vec3> getRenderBoundingBoxOuterPosition() {
		return fakePlayer != null && fakePlayer.fishing != null ? Optional.of(fakePlayer.fishing.position()) : Optional.empty();
	}
}
