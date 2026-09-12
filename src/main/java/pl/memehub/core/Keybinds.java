package pl.memehub.core;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import pl.memehub.MemeHub;

/**
 * Natywne keybindy Minecrafta (nie wlasne odpytywanie GLFW).
 *
 * <p>Wczesniej ClickGUI byl otwierany przez wlasne odpytywanie klawiszy w
 * {@code ModuleManager#handleKeybinds}, zalezne od mixinu {@code Minecraft#tick}
 * i wlasnego event busa. Teraz uzywamy {@link KeyMapping} zarejestrowanego przez
 * Fabric, czyli tej samej sciezki co klawisze vanilla:
 * <ul>
 *   <li>klawisz widoczny i zmienialny w <b>Opcje -&gt; Sterowanie</b> (kategoria "Meme Hub"),
 *   <li>gra sama dba o stan klawisza i wykrywanie klikniec ({@code consumeClick()}),
 *   <li>dziala niezaleznie od mixinow i event busa moda.</li>
 * </ul>
 *
 * <p>UWAGA: w Minecraft 26.2 modul Fabric nazywa sie
 * {@code fabric-key-mapping-api-v1} (stary {@code fabric-key-binding-api-v1}
 * zostal usuniety), a {@link KeyMapping} przyjmuje {@link KeyMapping.Category}
 * zamiast Stringa.
 */
public final class Keybinds {

	/** Otwiera ClickGUI. Domyslnie prawy Shift. */
	public static KeyMapping OPEN_GUI;

	private Keybinds() {
	}

	/** Rejestruje keybindy. Musi byc wywolane w {@code onInitializeClient}. */
	public static void register() {
		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(MemeHub.MOD_ID, "keys"));
		OPEN_GUI = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.memehub.open_gui",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				category));
	}

	/** Czytelna nazwa klawisza przypisanego do otwierania ClickGUI (np. "RIGHT SHIFT"). */
	public static String openGuiKeyName() {
		if (OPEN_GUI == null) {
			return "NONE";
		}
		return KeyMappingHelper.getBoundKeyOf(OPEN_GUI).getName()
				.replace("key.keyboard.", "").toUpperCase();
	}
}
