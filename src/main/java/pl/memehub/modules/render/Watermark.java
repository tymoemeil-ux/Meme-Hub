package pl.memehub.modules.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import pl.memehub.MemeHub;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.NumberSetting;
import pl.memehub.event.HudRenderEvent;
import pl.memehub.event.Subscribe;
import pl.memehub.util.RenderUtil;

/**
 * Watermark - logo moda w lewym gornym rogu ekranu.
 */
public final class Watermark extends Module {

	private final NumberSetting y = new NumberSetting("Y", "Pozycja pionowa (px)", 2.0, 0.0, 100.0, 1.0);

	public Watermark() {
		super("Watermark", "Rysuje logo Meme Hub na HUD", Category.RENDER, true);
		addSetting(y);
	}

	@Subscribe
	public void onHudRender(HudRenderEvent event) {
		if (!isEnabled() || event.graphics() == null) {
			return;
		}
		GuiGraphicsExtractor graphics = event.graphics();
		Font font = mc().font;
		if (font == null) {
			return;
		}
		String text = "\u00a79Meme\u00a7r \u00a7fHub\u00a7r \u00a77v" + MemeHub.VERSION;
		RenderUtil.textShadow(graphics, font, text, 2, y.intValue(), RenderUtil.rgb(255, 255, 255));
	}
}
