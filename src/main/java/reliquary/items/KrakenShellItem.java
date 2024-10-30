package reliquary.items;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import reliquary.handler.CommonEventHandler;
import reliquary.handler.HandlerPriority;
import reliquary.handler.IPlayerHurtHandler;
import reliquary.init.ModItems;
import reliquary.reference.Config;
import reliquary.util.InventoryHelper;

public class KrakenShellItem extends ItemBase {
	public KrakenShellItem() {
		super(new Properties().stacksTo(1));

		CommonEventHandler.registerPlayerHurtHandler(new IPlayerHurtHandler() {
			@Override
			public boolean canApply(Player player, LivingIncomingDamageEvent event) {
				return event.getSource() == player.damageSources().drown()
						&& player.getFoodData().getFoodLevel() > 0
						&& InventoryHelper.playerHasItem(player, ModItems.KRAKEN_SHELL.get());
			}

			@Override
			public boolean apply(Player player, LivingIncomingDamageEvent event) {
				float hungerDamage = event.getAmount() * ((float) Config.COMMON.items.krakenShell.hungerCostPercent.get() / 100F);
				player.causeFoodExhaustion(hungerDamage);
				return true;
			}

			@Override
			public HandlerPriority getPriority() {
				return HandlerPriority.HIGH;
			}
		});
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	// checks to see if the player is in water. If so, give them some minor
	// buffs.
	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int itemSlot, boolean isSelected) {
		if (level.getGameTime() % 3 != 0) {
			return;
		}

		if (entity instanceof Player player && player.isInWater()) {
			player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 5, 0, true, false));
			player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 5, 0, true, false));
			player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false));
		}
	}
}
