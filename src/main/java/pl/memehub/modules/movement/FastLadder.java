package pl.memehub.modules.movement;

import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.NumberSetting;

/**
 * FastLadder - szybsze wspinanie sie po drabinach (i pnaczach).
 */
public final class FastLadder extends Module {

	private final NumberSetting speed = new NumberSetting("Speed", "Dodatkowa predkosc w gore (bloki/s)", 0.2, 0.0, 1.0, 0.05);
	private final BooleanSetting requireUp = new BooleanSetting("Require Up", "Dzialaj tylko przy wcisnietym skoku", true);

	public FastLadder() {
		super("FastLadder", "Szybsze wspinanie po drabinach", Category.MOVEMENT);
		addSetting(speed);
		addSetting(requireUp);
	}

	@Override
	public void onTick() {
		var player = player();
		if (player == null || !player.onClimbable()) {
			return;
		}
		if (requireUp.get() && !player.input.keyPresses.jump()) {
			return;
		}
		var motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x, speed.get(), motion.z);
		player.fallDistance = 0.0F;
	}
}
