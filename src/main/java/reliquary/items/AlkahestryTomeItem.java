package reliquary.items;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import reliquary.common.gui.AlkahestTomeMenu;
import reliquary.crafting.AlkahestryChargingRecipe;
import reliquary.crafting.AlkahestryRecipeRegistry;
import reliquary.init.ModDataComponents;
import reliquary.init.ModSounds;
import reliquary.reference.Config;
import reliquary.util.TooltipBuilder;

public class AlkahestryTomeItem extends ToggleableItem {
	public AlkahestryTomeItem() {
		super(new Properties().setNoRepair().rarity(Rarity.EPIC).stacksTo(1).durability(10).component(DataComponents.REPAIR_COST, Integer.MAX_VALUE), Config.COMMON.disable.disableAlkahestry);
	}

	@Override
	public int getMaxDamage(ItemStack stack) {
		return getChargeLimit() + 1;
	}

	@Override
	public int getDamage(ItemStack stack) {
		return getMaxDamage(stack) - getCharge(stack);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		ItemStack newStack = super.use(level, player, hand).getObject();
		if (player.isShiftKeyDown()) {
			return new InteractionResultHolder<>(InteractionResult.SUCCESS, newStack);
		}

		player.playSound(ModSounds.BOOK.get(), 1.0f, 1.0f);
		if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
			serverPlayer.openMenu(new SimpleMenuProvider((w, p, pl) -> new AlkahestTomeMenu(w), stack.getHoverName()));
		}
		return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
	}

	@Override
	public boolean isEnchantable(ItemStack stack) {
		return false;
	}

	@Override
	public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
		return false;
	}

	@Override
	public void inventoryTick(ItemStack tome, Level level, Entity entity, int itemSlot, boolean isSelected) {
		if (level.isClientSide || !(entity instanceof Player player) || player.isSpectator() || level.getGameTime() % 10 != 0 || !isEnabled(tome) || getCharge(tome) == getChargeLimit()) {
			return;
		}

		for (AlkahestryChargingRecipe recipe : AlkahestryRecipeRegistry.getChargingRecipes()) {
			consumeAndCharge(player, getChargeLimit() - getCharge(tome), recipe.getChargeToAdd(),
					stack -> recipe.getChargingIngredient().test(stack), 16, chargeToAdd -> addCharge(tome, chargeToAdd));
		}
	}

	@Override
	protected void addMoreInformation(ItemStack tome, @Nullable HolderLookup.Provider registries, TooltipBuilder tooltipBuilder) {
		if (registries == null) {
			return;
		}

		tooltipBuilder.charge(this, ".tooltip2", getCharge(tome), getChargeLimit());
		tooltipBuilder.description(this, ".tooltip3");

		if (isEnabled(tome)) {
			tooltipBuilder.absorbActive(AlkahestryRecipeRegistry.getDrainRecipe().map(r -> r.getResultItem(registries).getHoverName().getString()).orElse(""));
		} else {
			tooltipBuilder.absorb();
		}
	}

	@Override
	protected boolean hasMoreInformation(ItemStack stack) {
		return true;
	}

	public static int getChargeLimit() {
		return Config.getOrDefault(Config.COMMON.items.alkahestryTome.chargeLimit, Config.COMMON_SPEC);
	}

	public static ItemStack setCharge(ItemStack tome, int charge) {
		tome.set(ModDataComponents.CHARGE, charge);
		return tome;
	}

	public static int getCharge(ItemStack tome) {
		return tome.getOrDefault(ModDataComponents.CHARGE, 0);
	}

	public static void addCharge(ItemStack tome, int chageToAdd) {
		setCharge(tome, getCharge(tome) + chageToAdd);
	}

	public static void useCharge(ItemStack tome, int chargeToUse) {
		addCharge(tome, -chargeToUse);
	}
}
