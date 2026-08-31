package pl.memehub.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Pomocnik rotacji gracza (celowanie w punkt / encje).
 * Oprocz zmiany rotacji lokalnie wysyla pakiet {@code ServerboundMovePlayerPacket.Rot},
 * aby serwer takze "widzial" obrot.
 */
public final class RotationUtil {
	private RotationUtil() {
	}

	/** Oblicza yaw/pitch potrzebne do spojrzenia z punktu {@code from} na {@code to}. */
	public static float[] getRotations(Vec3 from, Vec3 to) {
		double dx = to.x - from.x;
		double dy = to.y - from.y;
		double dz = to.z - from.z;
		double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontalDistance));
		return new float[]{Mth.wrapDegrees(yaw), Mth.wrapDegrees(pitch)};
	}

	/**
	 * Natychmiastowo obraca gracza w kierunku punktu i synchronizuje obrot z serwerem.
	 */
	public static void facePos(LocalPlayer player, Vec3 pos) {
		if (player == null) {
			return;
		}
		float[] rotations = getRotations(player.getEyePosition(), pos);
		player.setYRot(rotations[0]);
		player.setXRot(rotations[1]);
		player.yHeadRot = rotations[0];
		player.yBodyRot = rotations[0];
		if (player.connection != null) {
			player.connection.send(new ServerboundMovePlayerPacket.Rot(rotations[0], rotations[1], player.onGround()));
		}
	}

	/**
	 * Plyynnie obraca gracza w kierunku punktu (interpolacja rotLerp).
	 *
	 * @return true, gdy gracz jest juz (prawie) wycelowany.
	 */
	public static boolean smoothFacePos(LocalPlayer player, Vec3 pos, float step) {
		if (player == null) {
			return true;
		}
		float[] rotations = getRotations(player.getEyePosition(), pos);
		float yaw = Mth.rotLerp(step, player.getYRot(), rotations[0]);
		float pitch = Mth.rotLerp(step, player.getXRot(), rotations[1]);
		player.setYRot(yaw);
		player.setXRot(pitch);
		player.yHeadRot = yaw;
		player.yBodyRot = yaw;
		if (player.connection != null) {
			player.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround()));
		}
		return Math.abs(Mth.wrapDegrees(yaw - rotations[0])) < 2.0F
				&& Math.abs(Mth.wrapDegrees(pitch - rotations[1])) < 2.0F;
	}

	/** Kat (w stopniach) miedzy kierunkiem patrzenia gracza a kierunkiem do punktu. */
	public static float angleToPos(LocalPlayer player, Vec3 pos) {
		if (player == null) {
			return 0.0F;
		}
		float[] rotations = getRotations(player.getEyePosition(), pos);
		return Math.abs(Mth.wrapDegrees(rotations[0] - player.getYRot()))
				+ Math.abs(Mth.wrapDegrees(rotations[1] - player.getXRot()));
	}

	/** Czy gracz patrzy mniej-wiecej na punkt (kat ponizej progu). */
	public static boolean lookingAt(LocalPlayer player, Vec3 pos, float tolerance) {
		return angleToPos(player, pos) <= tolerance;
	}

	@SuppressWarnings("unused")
	private static Minecraft mc() {
		return Minecraft.getInstance();
	}
}
