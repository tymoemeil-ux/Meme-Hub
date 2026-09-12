package pl.memehub.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Pomocnik do wysylania wiadomosci na czat klienta.
 *
 * <p>Uwaga (26.1): {@code ChatComponent#addMessage} jest prywatne - do wiadomosci
 * pochodzacych od klienta uzywamy publicznego {@code addClientSystemMessage}.
 * Czat jest dostepny przez {@code Minecraft#gui.hud.getChat()} (reorganizacja Gui/Hud w 26.2).
 */
public final class ChatUtil {
	private ChatUtil() {
	}

	public static void sendMessage(String message) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.gui == null) {
			return;
		}
		mc.gui.hud.getChat().addClientSystemMessage(Component.literal(message));
	}

	public static void info(String message) {
		sendMessage("\u00a79[MemeHub\u00a7r\u00a79]\u00a7r " + message);
	}

	public static void warn(String message) {
		sendMessage("\u00a79[MemeHub\u00a7r\u00a79]\u00a7c " + message);
	}
}
