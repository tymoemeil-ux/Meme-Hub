package pl.memehub.modules.movement;

import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.NumberSetting;

/**
 * Spider - wspinanie sie po scianach (jak pajak).
 *
 * <p>Gdy gracz dotyka sciany (horizontalCollision) i nie stoi na ziemi,
 * modul nadaje predkosc pionowa w gore i zeruje licznik upadku.
 */
public final class Spider extends Module {

	private final NumberSetting climbSpeed = new NumberSetting("Climb Speed", "Predkosc wspinania (bloki/s)", 0.25, 0.05, 1.0, 0.05);
	private final BooleanSetting requireForward = new BooleanSetting("Require Forward", "Wspinaj tylko przy ruchu do przodu", true);

	public Spider() {
		super("Spider", "Wspinanie sie po scianach", Category.MOVEMENT);
		addSetting(climbSpeed);
		addSetting(requireForward);
	}

	@Override
	public void onTick() {
		var player = player();
		if (player == null || !player.horizontalCollision || player.onGround()) {
			return;
		}
		if (requireForward.get() && player.input.getMoveVector().y <= 0.01F) {
			return;
		}
		var motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x, climbSpeed.get(), motion.z);
		player.fallDistance = 0.0F;
	}
}
