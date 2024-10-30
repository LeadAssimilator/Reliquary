package reliquary.util.potions;

import net.minecraft.world.item.alchemy.PotionContents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PotionEssence {

	private final List<PotionIngredient> ingredients;

	private int redstoneCount;
	private int glowstoneCount;
	private PotionContents potionContents;

	public int getRedstoneCount() {
		return redstoneCount;
	}

	private PotionEssence(List<PotionIngredient> ingredients, PotionContents potionContents, int redstoneCount, int glowstoneCount) {
		this.ingredients = ingredients;
		this.potionContents = potionContents;
		this.redstoneCount = redstoneCount;
		this.glowstoneCount = glowstoneCount;
	}

	@SuppressWarnings("SameParameterValue")
	public void setRedstoneCount(int redstoneCount) {
		this.redstoneCount = redstoneCount;
	}

	public int getGlowstoneCount() {
		return glowstoneCount;
	}

	@SuppressWarnings("SameParameterValue")
	public void setGlowstoneCount(int glowstoneCount) {
		this.glowstoneCount = glowstoneCount;
	}

	public PotionContents getPotionContents() {
		return potionContents;
	}

	public PotionEssence copy() {
		return new Builder().setIngredients(ingredients).setPotionContents(potionContents).build();
	}

	public List<PotionIngredient> getIngredients() {
		return ingredients;
	}

	public void setPotionContents(PotionContents potionContents) {
		this.potionContents = potionContents;
	}

	public static class Builder {
		private List<PotionIngredient> ingredients = new ArrayList<>();
		private PotionContents potionContents = PotionContents.EMPTY;

		public Builder setIngredients(PotionIngredient... ingredients) {
			this.ingredients.addAll(Arrays.asList(ingredients));
			return this;
		}

		public Builder setIngredients(List<PotionIngredient> ingredients) {
			this.ingredients = ingredients;
			return this;
		}

		public Builder setPotionContents(PotionContents potionContents) {
			this.potionContents = potionContents;
			return this;
		}

		public PotionEssence build() {
			return new PotionEssence(ingredients, potionContents, 0, 0);
		}
	}
}
