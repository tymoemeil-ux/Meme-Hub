package pl.memehub.modules.movement;

import net.minecraft.world.phys.Vec3;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.NumberSetting;

/**
 * LongJump - skok z duzym rozpedem (boost poziomej predkosci w kierunku patrzenia).
 */
public final class LongJump extends Module {

	private final NumberSetting power = new NumberSetting("Power", "Predkosc pozioma po skoku (bloki/s)", 1.4, 0.5, 4.0, 0.1);
	private final NumberSetting minForward = new NumberSetting("Min Forward", "Min. wartosc wcisniecia do przodu (0-1)", 0.3, 0.0, 1.0, 0.05);
	private final BooleanSetting autoSprint = new BooleanSetting("Auto Sprint", "Automatyczny sprint podczas skoku", true);

	public LongJump() {
		super("LongJump", "Wzmocniony skok do przodu", Category.MOVEMENT);
		addSetting(power);
		addSetting(minForward);
		addSetting(autoSprint);
	}

	@Override
	public void onTick() {
		var player = player();
		if (player == null || !player.onGround()) {
			return;
		}
		if (player.input.getMoveVector().y < minForward.get().floatValue()) {
			return;
		}
		Vec3 look = player.getLookAngle();
		double h = Math.hypot(look.x, look.z);
		if (h < 0.01) {
			return;
		}
		if (autoSprint.get()) {
			player.setSprinting(true);
		}
		player.jumpFromGround();
		double speed = power.get();
		player.setDeltaMovement(look.x / h * speed, player.getDeltaMovement().y, look.z / h * speed);
	}
}
