package pl.memehub.modules.combat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.EnumSetting;
import pl.memehub.core.settings.NumberSetting;

/**
 * Criticals - wymuszanie krytycznych uderzen.
 *
 * <p>Tryb PACKET: przed atakiem wysyla pakietowe "mikroskoki" (seria pakietow
 * {@code ServerboundMovePlayerPacket.PosRot} z onGround=false), przez co serwer
 * uznaje gracza za spadajacego i nalicza krytyczne obrazenia.
 *
 * <p>Tryb JUMP: wykonuje prawdziwy skok ({@code jumpFromGround}) i atakuje
 * po kilku tickach, gdy gracz jest w powietrzu.
 */
public final class Criticals extends Module {

	public enum Mode {
		PACKET, JUMP
	}

	private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", "Sposob wymuszenia critow", Mode.PACKET);
	private final NumberSetting jumpTicks = new NumberSetting("Jump Delay", "Ticki przed atakiem (JUMP)", 3.0, 1.0, 10.0, 1.0);
	private final BooleanSetting onlyGround = new BooleanSetting("Only Ground", "Dzialaj tylko na ziemi", true);
	private final BooleanSetting notInLiquid = new BooleanSetting("Not In Liquid", "Nie dzialaj w wodzie/lawie", true);

	private Entity scheduledTarget = null;
	private int scheduledTicks = 0;

	public Criticals() {
		super("Criticals", "Wymusza krytyczne uderzenia (pakietowy skok lub atak podczas opadania)", Category.COMBAT);
		addSetting(mode);
		addSetting(jumpTicks);
		addSetting(onlyGround);
		addSetting(notInLiquid);
	}

	@Override
	protected void onEnable() {
		scheduledTarget = null;
		scheduledTicks = 0;
	}

	@Override
	public void onTick() {
		if (scheduledTarget == null) {
			return;
		}
		scheduledTicks--;
		if (scheduledTicks <= 0) {
			Entity target = scheduledTarget;
			scheduledTarget = null;
			var mc = mc();
			if (mc.gameMode != null && target != null && target.isAlive()) {
				mc.gameMode.attack(target);
			}
		}
	}

	@Override
	public boolean onPreAttack(LocalPlayer player, Entity target) {
		if (player == null || target == null) {
			return false;
		}
		// Atak zaprogramowany przez tryb JUMP - przepusc go.
		if (scheduledTarget != null) {
			return false;
		}
		if (onlyGround.get() && !player.onGround()) {
			return false;
		}
		if (notInLiquid.get() && (player.isInWater() || player.isInLava())) {
			return false;
		}
		if (player.isFallFlying() || player.isPassenger()) {
			return false;
		}

		if (mode.get() == Mode.PACKET) {
			doPacketJump(player);
			return false;
		}

		// Mode.JUMP
		player.jumpFromGround();
		scheduledTarget = target;
		scheduledTicks = jumpTicks.intValue();
		return true;
	}

	/** Pakietowy "mikroskok" - serwer widzi gracza spadajacego o ~0.0625 bloka. */
	private void doPacketJump(LocalPlayer player) {
		double x = player.getX();
		double y = player.getY();
		double z = player.getZ();
		float yaw = player.getYRot();
		float pitch = player.getXRot();
		var connection = player.connection;
		if (connection == null) {
			return;
		}
		connection.send(new ServerboundMovePlayerPacket.PosRot(x, y + 0.0625, z, yaw, pitch, false));
		connection.send(new ServerboundMovePlayerPacket.PosRot(x, y, z, yaw, pitch, false));
	}
}
