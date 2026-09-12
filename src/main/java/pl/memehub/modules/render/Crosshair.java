package pl.memehub.modules.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.EnumSetting;
import pl.memehub.core.settings.NumberSetting;
import pl.memehub.event.HudRenderEvent;
import pl.memehub.event.Subscribe;
import pl.memehub.util.RenderUtil;

/**
 * Crosshair - wlasny celownik rysowany na HUD.
 */
public final class Crosshair extends Module {

	public enum Style {
		DOT, PLUS
	}

	private final EnumSetting<Style> style = new EnumSetting<>("Style", "Ksztalt celownika", Style.PLUS);
	private final NumberSetting size = new NumberSetting("Size", "Rozmiar celownika (px)", 5.0, 2.0, 15.0, 1.0);
	private final NumberSetting gap = new NumberSetting("Gap", "Odstep od srodka (px)", 2.0, 0.0, 8.0, 1.0);
	private final NumberSetting thickness = new NumberSetting("Thickness", "Grubosc linii (px)", 1.0, 1.0, 4.0, 1.0);

	public Crosshair() {
		super("Crosshair", "Wlasny celownik na srodku ekranu", Category.RENDER);
		addSetting(style);
		addSetting(size);
		addSetting(gap);
		addSetting(thickness);
	}

	@Subscribe
	public void onHudRender(HudRenderEvent event) {
		if (!isEnabled() || event.graphics() == null) {
			return;
		}
		var mc = mc();
		if (mc.getWindow() == null) {
			return;
		}
		GuiGraphicsExtractor g = event.graphics();
		int cx = mc.getWindow().getGuiScaledWidth() / 2;
		int cy = mc.getWindow().getGuiScaledHeight() / 2;
		int color = RenderUtil.rgb(255, 255, 255);
		int s = size.intValue();
		int gp = gap.intValue();
		int t = thickness.intValue();

		if (style.get() == Style.DOT) {
			RenderUtil.rect(g, cx - t / 2, cy - t / 2, t, t, color);
			return;
		}

		// PLUS: cztery kreski wokol srodka.
		int len = s;
		// gorna
		g.fill(cx - t / 2, cy - gp - len, t, len, color);
		// dolna
		g.fill(cx - t / 2, cy + gp, t, len, color);
		// lewa
		g.fill(cx - gp - len, cy - t / 2, len, t, color);
		// prawa
		g.fill(cx + gp, cy - t / 2, len, t, color);
	}

	@SuppressWarnings("unused")
	private void unusedGuard(Font font) {
		// Font nie jest potrzebny - celownik rysowany ksztaltami.
	}
}
