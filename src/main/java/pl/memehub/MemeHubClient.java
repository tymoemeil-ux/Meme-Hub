package pl.memehub;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.memehub.config.Config;
import pl.memehub.core.Keybinds;
import pl.memehub.core.ModuleManager;
import pl.memehub.util.ChatUtil;
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

	/** Czy komunikat powitalny zostal juz wyslany na czat. */
	private boolean announced = false;

	@Override
	public void onInitializeClient() {
		ModuleManager.INSTANCE.init();
		Keybinds.register();
		Config.INSTANCE.load();

		// Tick klienta z Fabric API - sciezka NIEZALEZNA od mixinu Minecraft#tick
		// i od wlasnego event busa. Otwiera ClickGUI natywnym keybindem oraz
		// wysyla jednorazowy komunikat na czat, zeby bylo widac, ze mod dziala.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (Keybinds.OPEN_GUI != null) {
				while (Keybinds.OPEN_GUI.consumeClick()) {
					ClickGuiModule gui = ModuleManager.INSTANCE.get(ClickGuiModule.class);
					if (gui != null) {
						gui.toggle();
					}
				}
			}
			if (!announced && client.player != null) {
				announced = true;
				ChatUtil.info(ModuleManager.INSTANCE.getAll().size()
						+ " modulow zaladowanych. ClickGUI: " + Keybinds.openGuiKeyName()
						+ " (zmienisz w Opcje -> Sterowanie).");
			}
		});

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
