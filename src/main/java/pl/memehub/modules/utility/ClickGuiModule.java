package pl.memehub.modules.utility;

import net.minecraft.client.Minecraft;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.NumberSetting;
import pl.memehub.gui.ClickGuiScreen;
import pl.memehub.util.ChatUtil;

/**
 * ClickGUI - otwiera graficzny panel ustawien modulow.
 * Domyslny keybind: PRAWY SHIFT (moze byc zmieniony w konfiguracji / w GUI).
 */
public final class ClickGuiModule extends Module {

	private final NumberSetting panelWidth = new NumberSetting("Panel Width", "Szerokosc paneli (px)", 110.0, 80.0, 220.0, 5.0);

	public ClickGuiModule() {
		super("ClickGUI", "Graficzny panel zarzadzania modulami", Category.UTILITY);
		this.key = 340; // GLFW_KEY_RIGHT_SHIFT
		addSetting(panelWidth);
	}

	@Override
	protected void onEnable() {
		Minecraft mc = mc();
		if (mc == null || mc.gui == null) {
			this.setEnabled(false);
			return;
		}
		if (mc.gui.screen() instanceof ClickGuiScreen) {
			this.setEnabled(false);
			return;
		}
		mc.gui.setScreen(new ClickGuiScreen(mc, panelWidth.intValue()));
		this.setEnabled(false); // modul nie "trzyma" stanu - to jednorazowa akcja
	}

	/** Log uzyteczny przy testach. */
	@SuppressWarnings("unused")
	private void logInfo() {
		ChatUtil.info("ClickGUI: " + (mc().gui.screen() != null ? mc().gui.screen().getClass().getSimpleName() : "brak"));
	}
}
