package reliquary.compat.jade.provider;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import reliquary.reference.Config;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

import java.util.List;

public abstract class CachedBodyDataProvider implements IBlockComponentProvider {

	private List<List<IElement>> cachedBody = null;
	private BlockPos cachedPosition = null;

	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig pluginConfig) {
		if (Boolean.TRUE.equals(Config.CLIENT.wailaShiftForInfo.get()) && !accessor.getPlayer().isCrouching()) {
			tooltip.add(Component.translatable("waila.reliquary.shift_for_more").withStyle(ChatFormatting.ITALIC));
			return;
		}

		IJadeDataChangeIndicator changeIndicator = (IJadeDataChangeIndicator) accessor.getBlockEntity();

		if (changeIndicator == null || cachedBody == null || cachedPosition == null || !cachedPosition.equals(accessor.getPosition()) || changeIndicator.getDataChanged()) {
			cachedBody = getWailaBodyToCache(IElementHelper.get(), accessor, pluginConfig);
			cachedPosition = accessor.getPosition();
		}

		cachedBody = updateCache(IElementHelper.get(), accessor, cachedBody);

		for (List<IElement> line : cachedBody) {
			tooltip.add(line);
		}
	}

	public abstract List<List<IElement>> getWailaBodyToCache(IElementHelper helper, BlockAccessor accessor, IPluginConfig config);

	@SuppressWarnings("unused") //parameters used in overrides
	public List<List<IElement>> updateCache(IElementHelper helper, BlockAccessor accessor, List<List<IElement>> cached) {
		return cached;
	}
}

