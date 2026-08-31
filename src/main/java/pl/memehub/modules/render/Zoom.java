package pl.memehub.modules.render;

import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.NumberSetting;

/**
 * Zoom - zmiana kata widzenia (FOV). Przydatne do testow celowania.
 */
public final class Zoom extends Module {

	private final NumberSetting fov = new NumberSetting("FOV", "Kat widzenia podczas aktywnosci", 30.0, 10.0, 120.0, 5.0);

	private double oldFov = -1.0;

	public Zoom() {
		super("Zoom", "Zmniejsza FOV (zoom przy celowaniu)", Category.RENDER);
		addSetting(fov);
	}

	@Override
	protected void onEnable() {
		var mc = mc();
		if (mc.options != null) {
			oldFov = mc.options.fov().get();
			mc.options.fov().set((int) fov.get());
		}
	}

	@Override
	protected void onDisable() {
		var mc = mc();
		if (mc.options != null && oldFov >= 0.0) {
			mc.options.fov().set((int) oldFov);
		}
	}
}
