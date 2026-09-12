package pl.memehub.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import pl.memehub.core.Module;
import pl.memehub.core.ModuleManager;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.EnumSetting;
import pl.memehub.core.settings.NumberSetting;
import pl.memehub.core.settings.Setting;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Zapis / odczyt konfiguracji (wlaczone moduly, keybindy, wartosci ustawien)
 * do pliku JSON w katalogu config: {@code config/memehub/config.json}.
 */
public final class Config {
	public static final Config INSTANCE = new Config();

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private Path path;

	private Config() {
	}

	private Path path() {
		if (path == null) {
			path = FabricLoader.getInstance().getConfigDir().resolve("memehub").resolve("config.json");
		}
		return path;
	}

	/** Wczytuje konfiguracje i aplikuje ja na modulach. */
	public void load() {
		Path file = path();
		if (!Files.exists(file)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(file)) {
			JsonObject root = gson.fromJson(reader, JsonObject.class);
			if (root == null) {
				return;
			}
			applyModules(root.getAsJsonObject("modules"));
		} catch (IOException | RuntimeException e) {
			System.err.println("[MemeHub] Nie udalo sie wczytac konfiguracji: " + e.getMessage());
		}
	}

	private void applyModules(JsonObject modules) {
		if (modules == null) {
			return;
		}
		for (Module module : ModuleManager.INSTANCE.getAll()) {
			JsonObject entry = modules.getAsJsonObject(module.name());
			if (entry == null) {
				continue;
			}
			module.key = migrateKey(module.name(), entry.get("key").getAsInt());
			if (entry.get("enabled").getAsBoolean()) {
				module.setEnabled(true);
			}
			JsonObject settings = entry.getAsJsonObject("settings");
			if (settings == null) {
				continue;
			}
			for (Setting<?> setting : module.settings()) {
				applySetting(setting, settings.get(setting.name()));
			}
		}
	}

	/**
	 * Poprawia keybindy zapisane przez starsze wersje moda.
	 *
	 * <p>ClickGUI mial domyslnie klawisz 340 ({@code GLFW_KEY_LEFT_SHIFT}) z blednym
	 * komentarzem, ze to prawy Shift. 340 to domyslny klawisz skradania sie, wiec
	 * panel otwieral sie przy kazdym skradaniu. Zapisany 340 podmieniamy na 344
	 * ({@code GLFW_KEY_RIGHT_SHIFT}); kazda inna wartosc to swiadomy wybor gracza.
	 */
	private static int migrateKey(String moduleName, int savedKey) {
		if ("ClickGUI".equals(moduleName) && savedKey == 340) {
			return 344;
		}
		return savedKey;
	}

	private void applySetting(Setting<?> setting, JsonElement element) {
		if (element == null) {
			return;
		}
		if (setting instanceof BooleanSetting booleanSetting) {
			booleanSetting.set(element.getAsBoolean());
		} else if (setting instanceof NumberSetting numberSetting) {
			numberSetting.set(element.getAsDouble());
		} else if (setting instanceof EnumSetting<?> enumSetting) {
			applyEnum(enumSetting, element.getAsString());
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void applyEnum(EnumSetting enumSetting, String name) {
		enumSetting.setByName(name);
	}

	/** Zapisuje biezaca konfiguracje do pliku. */
	public void save() {
		try {
			Files.createDirectories(path().getParent());
		} catch (IOException e) {
			System.err.println("[MemeHub] Nie mozna utworzyc katalogu config: " + e.getMessage());
			return;
		}
		JsonObject root = new JsonObject();
		JsonObject modules = new JsonObject();
		for (Module module : ModuleManager.INSTANCE.getAll()) {
			JsonObject entry = new JsonObject();
			entry.addProperty("enabled", module.isEnabled());
			entry.addProperty("key", module.key);
			JsonObject settings = new JsonObject();
			for (Setting<?> setting : module.settings()) {
				if (setting instanceof BooleanSetting booleanSetting) {
					settings.addProperty(setting.name(), booleanSetting.get());
				} else if (setting instanceof NumberSetting numberSetting) {
					settings.addProperty(setting.name(), numberSetting.get());
				} else if (setting instanceof EnumSetting<?> enumSetting) {
					settings.addProperty(setting.name(), enumSetting.get().name());
				}
			}
			entry.add("settings", settings);
			modules.add(module.name(), entry);
		}
		root.add("modules", modules);
		try (Writer writer = Files.newBufferedWriter(path())) {
			gson.toJson(root, writer);
		} catch (IOException e) {
			System.err.println("[MemeHub] Nie udalo sie zapisac konfiguracji: " + e.getMessage());
		}
	}
}
