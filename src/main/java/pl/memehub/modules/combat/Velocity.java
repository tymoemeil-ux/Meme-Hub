package pl.memehub.modules.combat;

import net.minecraft.world.entity.MoverType;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.EnumSetting;
import pl.memehub.core.settings.NumberSetting;
import pl.memehub.event.EntityMoveEvent;
import pl.memehub.event.Subscribe;

/**
 * Velocity - redukcja knockbacku (odbicia po otrzymaniu obrazen).
 *
 * <p>Dziala przez modyfikacje wektora ruchu w {@link EntityMoveEvent}
 * (mixin {@code Entity#move}). Tryb HURT redukuje knockback tylko w momentach
 * po otrzymaniu obrazen (hurtTime &gt; 0); tryb ALWAYS skaluje kazdy ruch
 * gracza (testy fizyki).
 */
public final class Velocity extends Module {

	public enum Mode {
		HURT, ALWAYS
	}

	private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", "Kiedy redukowac ruch", Mode.HURT);
	private final NumberSetting horizontal = new NumberSetting("Horizontal", "Pozioma redukcja knockbacku (0-100%)", 0.0, 0.0, 100.0, 5.0);
	private final NumberSetting vertical = new NumberSetting("Vertical", "Pionowa redukcja knockbacku (0-100%)", 100.0, 0.0, 100.0, 5.0);

	public Velocity() {
		super("Velocity", "Redukcja knockbacku po otrzymaniu obrazen", Category.COMBAT);
		addSetting(mode);
		addSetting(horizontal);
		addSetting(vertical);
	}

	@Subscribe
	public void onMove(EntityMoveEvent event) {
		if (event.moverType() != MoverType.SELF) {
			return;
		}
		var player = player();
		if (player == null || event.entity() != player) {
			return;
		}
		if (mode.get() == Mode.HURT && player.hurtTime <= 0) {
			return;
		}
		double hx = event.modifiedMovement().x * (horizontal.get() / 100.0);
		double hz = event.modifiedMovement().z * (horizontal.get() / 100.0);
		double vy = event.modifiedMovement().y * (vertical.get() / 100.0);
		event.setMovement(hx, vy, hz);
	}
}
