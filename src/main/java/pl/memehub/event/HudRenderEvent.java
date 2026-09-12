package pl.memehub.event;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Zdarzenie renderowania HUD (wysylane z elementu HUD zarejestrowanego przez
 * Fabric API {@code HudElementRegistry}). Dzieki niemu moduly / HUD moga rysowac
 * nakladki bez bezposredniej zaleznosci od mixinow renderujacych.
 */
public final class HudRenderEvent extends Event {
	private final GuiGraphicsExtractor graphics;
	private final DeltaTracker deltaTracker;

	public HudRenderEvent(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		this.graphics = graphics;
		this.deltaTracker = deltaTracker;
	}

	public GuiGraphicsExtractor graphics() {
		return graphics;
	}

	public DeltaTracker deltaTracker() {
		return deltaTracker;
	}
}
