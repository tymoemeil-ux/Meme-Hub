package pl.memehub.modules.movement;

import net.minecraft.world.phys.Vec3;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.NumberSetting;

/**
 * Speed - zwiekszenie predkosci poziomej na ziemi (strafe boost).
 *
 * <p>Mnozy pozioma skladowa predkosci gracza przez wspolczynnik (do maksymalnej
 * predkosci {@code Max Speed}). Eksperyment z fizyka silnika - w trybie
 * pojedynczego swiata serwer zintegrowany moze korygowac pozycje gracza.
 */
public final class Speed extends Module {

	private final NumberSetting multiplier = new NumberSetting("Multiplier", "Wspolczynnik predkosci (1.0 = brak)", 1.5, 1.0, 3.0, 0.05);
	private final NumberSetting maxSpeed = new NumberSetting("Max Speed", "Limit predkosci (bloki/s)", 1.2, 0.3, 5.0, 0.05);
	private final BooleanSetting onlySprint = new BooleanSetting("Only Sprint", "Dzialaj tylko podczas sprintu", false);
	private final BooleanSetting requireInput = new BooleanSetting("Require Input", "Dzialaj tylko przy wcisnietym ruchu", true);

	public Speed() {
		super("Speed", "Zwieksza predkosc pozioma gracza", Category.MOVEMENT);
		addSetting(multiplier);
		addSetting(maxSpeed);
		addSetting(onlySprint);
		addSetting(requireInput);
	}

	@Override
	public void onTick() {
		var player = player();
		if (player == null || !player.onGround()) {
			return;
		}
		if (onlySprint.get() && !player.isSprinting()) {
			return;
		}
		boolean moving = player.input.keyPresses.forward() || player.input.keyPresses.backward()
				|| player.input.keyPresses.left() || player.input.keyPresses.right();
		if (requireInput.get() && !moving) {
			return;
		}

		Vec3 motion = player.getDeltaMovement();
		double horizontal = Math.hypot(motion.x, motion.z);
		if (horizontal < 0.01) {
			return;
		}
		double factor = multiplier.get();
		double newHorizontal = Math.min(horizontal * factor, maxSpeed.get());
		double scale = newHorizontal / horizontal;
		player.setDeltaMovement(motion.x * scale, motion.y, motion.z * scale);
	}
}
