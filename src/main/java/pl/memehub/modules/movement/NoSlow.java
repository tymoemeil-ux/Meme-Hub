package pl.memehub.modules.movement;

import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.NumberSetting;

/**
 * NoSlow - zniesienie spowolnienia podczas uzywania przedmiotow
 * (jedzenie, luki, bloki). Mnozy pozioma predkosc, gdy gracz uzywa przedmiotu.
 */
public final class NoSlow extends Module {

	private final NumberSetting factor = new NumberSetting("Factor", "Mnoznik predkosci podczas uzywania", 1.6, 1.0, 3.0, 0.1);
	private final BooleanSetting onlyMoving = new BooleanSetting("Only Moving", "Dzialaj tylko przy wcisnietym ruchu", true);

	public NoSlow() {
		super("NoSlow", "Brak spowolnienia podczas uzywania przedmiotow", Category.MOVEMENT);
		addSetting(factor);
		addSetting(onlyMoving);
	}

	@Override
	public void onTick() {
		var player = player();
		if (player == null || !player.isUsingItem() || player.onGround()) {
			return;
		}
		boolean moving = player.input.keyPresses.forward() || player.input.keyPresses.backward()
				|| player.input.keyPresses.left() || player.input.keyPresses.right();
		if (onlyMoving.get() && !moving) {
			return;
		}
		var motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x * factor.get(), motion.y, motion.z * factor.get());
	}
}
