package pl.memehub;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.memehub.config.Config;
import pl.memehub.core.ModuleManager;
import pl.memehub.event.EventBus;
import pl.memehub.event.HudRenderEvent;
import pl.memehub.modules.utility.ClickGuiModule;

/**
 * Glowny punkt wejscia moda (entrypoint "client" z fabric.mod.json).
 *
 * <p>Inicjalizuje ModuleManager, wczytuje konfiguracje i rejestruje element HUD
 * przez Fabric API (HudElementRegistry) - kazda klatka renderowania HUD wysyla
 * {@link HudRenderEvent} na wlasny event bus moda.
 */
public final class MemeHubClient implements ClientModInitializer {

	private static final Logger LOGGER = LoggerFactory.getLogger("MemeHub");

	@Override
	public void onInitializeClient() {
		ModuleManager.INSTANCE.init();
		Config.INSTANCE.load();

		ClickGuiModule clickGui = ModuleManager.INSTANCE.get(ClickGuiModule.class);
		LOGGER.info("[MemeHub] zainicjowano: {} modulow. ClickGUI = {} (kod {}). HUD i Watermark sa wlaczone domyslnie.",
				ModuleManager.INSTANCE.getAll().size(),
				clickGui != null ? clickGui.keyName() : "brak",
				clickGui != null ? clickGui.key : -1);

		// Wlasny element HUD - wysyla zdarzenie renderowania HUD na event bus moda.
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(MemeHub.MOD_ID, "hud"),
				(graphics, deltaTracker) -> EventBus.INSTANCE.post(new HudRenderEvent(graphics, deltaTracker))
		);
		LOGGER.info("[MemeHub] element HUD zarejestrowany (HudElementRegistry, przed VanillaHudElements.CHAT).");
	}
}
