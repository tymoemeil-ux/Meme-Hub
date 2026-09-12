package pl.memehub.modules.utility;

import net.minecraft.client.Minecraft;
import pl.memehub.core.Category;
import pl.memehub.core.Keybinds;
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
		// Keybind obsluguje NATYWNY KeyMapping Minecrafta (Keybinds.OPEN_GUI, domyslnie
		// prawy Shift) rejestrowany przez Fabric. key = 0, zeby wlasne odpytywanie
		// klawiszy w ModuleManager nie otwieralo panelu drugi raz - drugi toggle
		// natychmiast by go zamknal.
		this.key = 0;
		addSetting(panelWidth);
	}

	/** Nazwa klawisza z natywnego keybindu (widoczna tez w Opcje -> Sterowanie). */
	@Override
	public String keyName() {
		return Keybinds.openGuiKeyName();
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
