package reliquary.client.gui.components;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;

public abstract class Component {
	public int getPadding() {
		return 1;
	}

	public int getHeight() {
		return getHeightInternal() + getPadding() * 2;
	}

	public int getWidth() {
		return getWidthInternal() + getPadding() * 2;
	}

	public void render(GuiGraphics guiGraphics, int x, int y) {
		renderInternal(guiGraphics, x + getPadding(), y + getPadding());
	}

	public boolean shouldRender() {
		return true;
	}

	public abstract int getHeightInternal();

	public abstract int getWidthInternal();

	public abstract void renderInternal(GuiGraphics guiGraphics, int x, int y);

	@SuppressWarnings("java:S107")
	protected void blit(GuiGraphics guiGraphics, int x, int y, int textureX, int textureY, int width, int height, float textureWidth, float textureHeight) {
		float minU = textureX / textureWidth;
		float maxU = (textureX + width) / textureWidth;
		float minV = textureY / textureHeight;
		float maxV = (textureY + height) / textureHeight;

		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		Matrix4f matrix4f = guiGraphics.pose().last().pose();
		buffer.addVertex(matrix4f, x, y + height, 0).setUv(minU, maxV);
		buffer.addVertex(matrix4f, x + width, y + height, 0).setUv(maxU, maxV);
		buffer.addVertex(matrix4f, x + width, y, 0).setUv(maxU, minV);
		buffer.addVertex(matrix4f, x, y, 0).setUv(minU, minV);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}
}
