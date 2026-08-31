package pl.memehub.modules.movement;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.NumberSetting;

/**
 * AirJump - dodatkowy skok w powietrzu.
 *
 * <p>Gdy gracz wciska skok, a nie stoi na ziemi, modul sztucznie ustawia
 * {@code onGround = true} i wywoluje {@code jumpFromGround()}, co pozwala na
 * kolejny skok. Opcjonalnie wysyla pakiet {@code StatusOnly(true)}, aby serwer
 * zintegrowany "widzial" gracza na ziemi (testy fizyki).
 */
public final class AirJump extends Module {

	private final BooleanSetting packet = new BooleanSetting("Packet", "Wysylaj pakiet onGround=true", true);
	private final BooleanSetting notInLiquid = new BooleanSetting("Not In Liquid", "Nie dzialaj w wodzie/lawie", true);
	private final BooleanSetting notFlying = new BooleanSetting("Not Elytra", "Nie dzialaj podczas lotu (elytra)", true);
	private final NumberSetting cooldown = new NumberSetting("Cooldown", "Min. przerwa miedzy skokami (ticki)", 3.0, 0.0, 20.0, 1.0);

	private int lastJumpTick = -100;

	public AirJump() {
		super("AirJump", "Pozwala skakac w powietrzu (double jump)", Category.MOVEMENT);
		addSetting(packet);
		addSetting(notInLiquid);
		addSetting(notFlying);
		addSetting(cooldown);
	}

	@Override
	public void onTick() {
		var player = player();
		if (player == null || player.connection == null) {
			return;
		}
		if (player.onGround()) {
			return;
		}
		if (!player.input.jumping) {
			return;
		}
		if (notInLiquid.get() && (player.isInWater() || player.isInLava())) {
			return;
		}
		if (notFlying.get() && player.isFallFlying()) {
			return;
		}
		int tick = player.tickCount;
		if (tick - lastJumpTick < cooldown.intValue()) {
			return;
		}

		player.onGround = true;
		player.jumpFromGround();
		if (packet.get()) {
			player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true));
		}
		lastJumpTick = tick;
	}
}
