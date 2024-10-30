package reliquary.handler;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public interface IPlayerHurtHandler extends IPrioritizedHandler {
	boolean canApply(Player player, LivingIncomingDamageEvent event);

	boolean apply(Player player, LivingIncomingDamageEvent event);
}
