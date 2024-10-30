package reliquary.items.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

public interface IPotionItem {
	PotionContents getPotionContents(ItemStack stack);
}
