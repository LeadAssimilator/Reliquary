package reliquary.handler;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public interface IPlayerDeathHandler extends IPrioritizedHandler {
	boolean canApply(Player player, LivingDeathEvent event);
	boolean apply(Player player, LivingDeathEvent event);

}
