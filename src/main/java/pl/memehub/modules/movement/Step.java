package pl.memehub.modules.movement;

import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.NumberSetting;

/**
 * Step - automatyczne przeskakiwanie blokow o 1 wysokosci.
 *
 * <p>Gdy gracz uderza w sciane na ziemi i porusza sie do przodu, modul
 * wykonuje skok (jak auto-step z modow "legit").
 */
public final class Step extends Module {

	private final NumberSetting minSpeed = new NumberSetting("Min Speed", "Min. predkosc pozioma do skoku (bloki/s)", 0.15, 0.0, 1.0, 0.05);

	public Step() {
		super("Step", "Automatyczne przeskakiwanie blokow o 1 wysokosci", Category.MOVEMENT);
		addSetting(minSpeed);
	}

	@Override
	public void onTick() {
		var player = player();
		if (player == null || !player.onGround() || !player.horizontalCollision) {
			return;
		}
		if (player.input.jumping) {
			return; // gracz juz skacze sam
		}
		var motion = player.getDeltaMovement();
		if (Math.hypot(motion.x, motion.z) < minSpeed.get()) {
			return;
		}
		player.jumpFromGround();
	}
}
