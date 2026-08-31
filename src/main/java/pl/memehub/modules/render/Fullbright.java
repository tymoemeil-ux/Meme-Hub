package pl.memehub.modules.render;

import net.minecraft.client.player.LocalPlayer;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.NumberSetting;

/**
 * Fullbright - podbicie jasnosci gry (gamina do maksimum).
 */
public final class Fullbright extends Module {

	private final NumberSetting gamma = new NumberSetting("Gamma", "Jasnosc (domyslnie 16.0)", 16.0, 0.0, 16.0, 1.0);

	private double oldGamma = -1.0;

	public Fullbright() {
		super("Fullbright", "Podbija jasnosc ekranu do maksimum", Category.RENDER);
		addSetting(gamma);
	}

	@Override
	protected void onEnable() {
		var mc = mc();
		if (mc.options != null) {
			oldGamma = mc.options.gamma().get();
			mc.options.gamma().set(gamma.get());
		}
	}

	@Override
	protected void onDisable() {
		var mc = mc();
		if (mc.options != null && oldGamma >= 0.0) {
			mc.options.gamma().set(oldGamma);
		}
	}

	@SuppressWarnings("unused")
	private LocalPlayer player() {
		return mc().player;
	}
}
