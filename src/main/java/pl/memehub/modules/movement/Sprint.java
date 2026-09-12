package pl.memehub.modules.movement;

import net.minecraft.client.player.LocalPlayer;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.EnumSetting;
import pl.memehub.core.settings.NumberSetting;

/**
 * Sprint - automatyczne sprintowanie.
 */
public final class Sprint extends Module {

	public enum Mode {
		ALWAYS, ON_MOVE, LEGIT
	}

	private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", "Tryb sprintu", Mode.ALWAYS);
	private final NumberSetting hunger = new NumberSetting("Min Food", "Min. wskaznik sycienia (0 = ignoruj)", 6.0, 0.0, 20.0, 1.0);
	private final BooleanSetting autoJump = new BooleanSetting("Auto Jump", "Automatyczne skakanie podczas sprintu", false);

	public Sprint() {
		super("Sprint", "Automatyczne sprintowanie", Category.MOVEMENT);
		addSetting(mode);
		addSetting(hunger);
		addSetting(autoJump);
	}

	@Override
	public void onTick() {
		var player = player();
		if (player == null) {
			return;
		}
		if (hunger.get() > 0.0 && player.getFoodData().getFoodLevel() < hunger.get()) {
			return;
		}
		switch (mode.get()) {
			case ALWAYS -> player.setSprinting(true);
			case ON_MOVE -> {
				if (isMoving(player)) {
					player.setSprinting(true);
				} else {
					player.setSprinting(false);
				}
			}
			case LEGIT -> {
				// Sprint tylko, gdy gracz sam trzyma sprint.
				if (player.isSprinting() && isMoving(player)) {
					player.setSprinting(true);
				}
			}
		}
		if (autoJump.get() && player.isSprinting() && player.onGround() && isMoving(player)) {
			player.jumpFromGround();
		}
	}

	private boolean isMoving(LocalPlayer player) {
		return player.input.keyPresses.forward() || player.input.keyPresses.backward()
				|| player.input.keyPresses.left() || player.input.keyPresses.right();
	}
}
