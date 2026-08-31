package pl.memehub.core;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import pl.memehub.MemeHub;
import pl.memehub.core.settings.Setting;
import pl.memehub.util.ChatUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bazowa klasa kazdego modulu Meme Hub.
 *
 * <p>Modul posiada: nazwe, opis, kategorie, opcjonalny keybind (kod klawisza GLFW)
 * oraz liste ustawien. Cykl zycia modulu:
 * <ul>
 *   <li>{@link #onEnable()} / {@link #onDisable()} - uruchomienie / zatrzymanie,</li>
 *   <li>{@link #onTick()} - kazdy tick klienta, gdy modul jest wlaczony,</li>
 *   <li>{@link #onPreAttack(LocalPlayer, Entity)} - przed atakiem (gameMode.attack),</li>
 *   <li>{@link #onPacketSend(Packet)} - przy kazdym wysylanym pakiecie.</li>
 * </ul>
 */
public abstract class Module {
	protected final String name;
	protected final String description;
	protected final Category category;

	/** Kod klawisza GLFW (0 = brak keybindu). Ustawiany w ClickGUI lub konfiguracji. */
	public int key;

	protected boolean enabled;

	private final List<Setting<?>> settings = new ArrayList<>();

	protected Module(String name, String description, Category category) {
		this.name = name;
		this.description = description;
		this.category = category;
	}

	// ------------------------------------------------------------------
	// Podstawowe gettery / settery
	// ------------------------------------------------------------------

	public String name() {
		return name;
	}

	public String description() {
		return description;
	}

	public Category category() {
		return category;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}
		this.enabled = enabled;
		if (enabled) {
			onEnable();
		} else {
			onDisable();
		}
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	/** Nazwa keybindu do wyswietlenia (np. "RIGHT SHIFT") lub "NONE". */
	public String keyName() {
		if (key == 0) {
			return "NONE";
		}
		String name = InputConstants.Type.KEYSYM.getOrCreate(key).getName();
		return name.replace("key.keyboard.", "").toUpperCase();
	}

	// ------------------------------------------------------------------
	// Ustawienia
	// ------------------------------------------------------------------

	protected void addSetting(Setting<?> setting) {
		settings.add(setting);
	}

	public List<Setting<?>> settings() {
		return Collections.unmodifiableList(settings);
	}

	@SuppressWarnings("unchecked")
	public <T extends Setting<?>> T getSetting(String name) {
		for (Setting<?> setting : settings) {
			if (setting.name().equalsIgnoreCase(name)) {
				return (T) setting;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------
	// Cykl zycia (do nadpisania w modulach)
	// ------------------------------------------------------------------

	/** Wywolywane po wlaczeniu modulu. */
	protected void onEnable() {
	}

	/** Wywolywane po wylaczeniu modulu. */
	protected void onDisable() {
	}

	/** Wywolywane w kazdym ticku klienta, gdy modul jest wlaczony. */
	public void onTick() {
	}

	/**
	 * Wywolywane przed atakiem (MultiPlayerGameMode#attack).
	 *
	 * @return true, jesli modul "przejal" atak i mixin ma anulowac oryginalne wywolanie.
	 */
	public boolean onPreAttack(LocalPlayer player, Entity target) {
		return false;
	}

	/** Wywolywane przy kazdym pakiecie wysylanym przez klienta, gdy modul jest wlaczony. */
	public void onPacketSend(Packet<?> packet) {
	}

	// ------------------------------------------------------------------
	// Pomocnicze
	// ------------------------------------------------------------------

	protected Minecraft mc() {
		return Minecraft.getInstance();
	}

	protected LocalPlayer player() {
		return Minecraft.getInstance().player;
	}

	/** Wysyla wiadomosc o przelaczeniu modulu na czat klienta. */
	protected void sendToggleMessage() {
		String state = enabled ? "\u00a7aON" : "\u00a7cOFF";
		ChatUtil.sendMessage(MemeHub.PREFIX + "\u00a7f" + name + " \u00a77\u00bb\u00a7r " + state);
	}
}
