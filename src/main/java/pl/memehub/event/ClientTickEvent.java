package pl.memehub.event;

/**
 * Zdarzenia ticku klienta.
 * <ul>
 *   <li>{@link Pre} - poczatek ticku (HEAD mixina {@code Minecraft#tick})</li>
 *   <li>{@link Post} - koniec ticku (TAIL mixina {@code Minecraft#tick})</li>
 * </ul>
 */
public abstract class ClientTickEvent extends Event {
	public static final class Pre extends ClientTickEvent {
	}

	public static final class Post extends ClientTickEvent {
	}
}
