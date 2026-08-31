package pl.memehub.modules.utility;

import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.NumberSetting;

/**
 * AutoRespawn - automatyczne odradzanie sie po smierci.
 */
public final class AutoRespawn extends Module {

	private final NumberSetting delay = new NumberSetting("Delay", "Opoznienie po smierci (ticki)", 10.0, 0.0, 100.0, 1.0);

	private int deathTicks = 0;

	public AutoRespawn() {
		super("AutoRespawn", "Automatyczne odradzanie po smierci", Category.UTILITY);
		addSetting(delay);
	}

	@Override
	protected void onEnable() {
		deathTicks = 0;
	}

	@Override
	public void onTick() {
		var mc = mc();
		var player = player();
		if (mc.gui == null || player == null || player.connection == null) {
			return;
		}
		if (!(mc.gui.screen() instanceof DeathScreen)) {
			deathTicks = 0;
			return;
		}
		deathTicks++;
		if (deathTicks < delay.intValue()) {
			return;
		}
		player.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
		mc.gui.setScreen(null);
		deathTicks = 0;
	}
}
