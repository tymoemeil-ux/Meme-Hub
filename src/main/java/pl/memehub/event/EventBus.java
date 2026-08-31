package pl.memehub.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prosty, wlasny Event Bus - serce architektury Meme Hub.
 *
 * <p>Dzialanie:
 * <ol>
 *   <li>Klasa rejestruje sie przez {@link #register(Object)}.</li>
 *   <li>Metody oznaczone {@link Subscribe} i przyjmujace dokladnie jeden argument
 *       (podtyp {@link Event}) sa podpinane pod ten typ zdarzenia.</li>
 *   <li>{@link #post(Event)} wywoluje wszystkie podpiete obserwatory.</li>
 * </ol>
 *
 * <p>Z mixinow zdarzenia sa wysylane np.:
 * <ul>
 *   <li>{@code Minecraft#tick} -&gt; {@link ClientTickEvent}</li>
 *   <li>{@code Connection#send} -&gt; {@link PacketSendEvent}</li>
 *   <li>{@code Entity#move} -&gt; {@link EntityMoveEvent}</li>
 * </ul>
 */
public final class EventBus {
	private static final Logger LOGGER = LoggerFactory.getLogger("MemeHub/EventBus");

	public static final EventBus INSTANCE = new EventBus();

	private final Map<Class<?>, List<Listener>> listeners = new ConcurrentHashMap<>();

	private EventBus() {
	}

	/** Rejestruje obserwatora (obiekt z metodami @Subscribe). */
	public void register(Object owner) {
		for (Method method : owner.getClass().getDeclaredMethods()) {
			if (!method.isAnnotationPresent(Subscribe.class)) {
				continue;
			}
			Class<?>[] params = method.getParameterTypes();
			if (params.length != 1 || !Event.class.isAssignableFrom(params[0])) {
				LOGGER.warn("Ignoruje @Subscribe {}#{}(...): metoda musi przyjmowac dokladnie jeden argument typu Event",
						owner.getClass().getSimpleName(), method.getName());
				continue;
			}
			listeners.computeIfAbsent(params[0], k -> new ArrayList<>()).add(new Listener(owner, method));
		}
	}

	/** Wysyla zdarzenie do wszystkich obserwatorow. Zwraca to samo zdarzenie (po ew. anulowaniu). */
	@SuppressWarnings("unchecked")
	public <T extends Event> T post(T event) {
		List<Listener> list = listeners.get(event.getClass());
		if (list == null) {
			return event;
		}
		for (Listener listener : new ArrayList<>(list)) {
			try {
				listener.method().invoke(listener.owner(), event);
			} catch (ReflectiveOperationException e) {
				LOGGER.error("Blad podczas wywolywania obserwatora {}#{} dla zdarzenia {}",
						listener.owner().getClass().getSimpleName(), listener.method().getName(),
						event.getClass().getSimpleName(), e);
			}
		}
		return event;
	}

	private record Listener(Object owner, Method method) {
	}
}
