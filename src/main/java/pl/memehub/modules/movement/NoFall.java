package pl.memehub.modules.movement;

import net.minecraft.client.player.LocalPlayer;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.EnumSetting;
import pl.memehub.core.settings.NumberSetting;

/**
 * NoFall - anulowanie obrazen od upadku.
 *
 * <p>PACKET: jesli gracz ma za duza wysokosc spadku, wysyla pakiet
 * {@code ServerboundMovePlayerPacket} z onGround=true, przez co serwer
 * resetuje licznik upadku.
 */
public final class NoFall extends Module {

	public enum Mode {
		PACKET
	}

	private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", "Tryb dzialania", Mode.PACKET);
	private final NumberSetting maxFallDistance = new NumberSetting("Max Fall Distance", "Po jakiej wysokosci aktywowac", 3.0, 0.5, 10.0, 0.5);

	public NoFall() {
		super("NoFall", "Anuluje obrazenia od upadku (pakietowo)", Category.MOVEMENT);
		addSetting(mode);
		addSetting(maxFallDistance);
	}

	@Override
	public void onTick() {
		var player = player();
		if (player == null || player.connection == null) {
			return;
		}
		if (player.fallDistance <= maxFallDistance.get()) {
			return;
		}
		if (player.isFallFlying() || player.isInWater() || player.isInLava()) {
			return;
		}
		LocalPlayer p = player;
		p.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
				.Pos(p.getX(), p.getY(), p.getZ(), true));
		player.fallDistance = 0.0F;
	}
}
