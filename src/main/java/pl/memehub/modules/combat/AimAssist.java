package pl.memehub.modules.combat;

import net.minecraft.world.entity.LivingEntity;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.NumberSetting;
import pl.memehub.util.EntityUtil;
import pl.memehub.util.RotationUtil;

/**
 * AimAssist - plynnym celowanie w najblizszy cel (bez ataku).
 */
public final class AimAssist extends Module {

	private final NumberSetting range = new NumberSetting("Range", "Zasieg szukania celu (bloki)", 6.0, 1.0, 12.0, 0.5);
	private final NumberSetting speed = new NumberSetting("Speed", "Predkosc obrotu (1-10)", 3.0, 0.5, 10.0, 0.5);
	private final BooleanSetting targetPlayers = new BooleanSetting("Players", "Celuj w graczy", true);
	private final BooleanSetting targetMonsters = new BooleanSetting("Monsters", "Celuj w potwory", true);
	private final BooleanSetting targetAnimals = new BooleanSetting("Animals", "Celuj w zwierzeta", false);
	private final BooleanSetting silent = new BooleanSetting("Silent", "Nie obracaj widoku gracza (tylko pakiet rotacji)", false);

	public AimAssist() {
		super("AimAssist", "Plynne celowanie w najblizszy cel", Category.COMBAT);
		addSetting(range);
		addSetting(speed);
		addSetting(targetPlayers);
		addSetting(targetMonsters);
		addSetting(targetAnimals);
		addSetting(silent);
	}

	@Override
	public void onTick() {
		var player = player();
		if (player == null || player.connection == null) {
			return;
		}
		LivingEntity target = EntityUtil.getNearestTarget(player, range.get(),
				targetPlayers.get(), targetMonsters.get(), targetAnimals.get());
		if (target == null) {
			return;
		}
		float[] rotations = RotationUtil.getRotations(player.getEyePosition(), target.getEyePosition());
		if (silent.get()) {
			// Wyslij pakiet rotacji bez zmiany lokalnego widoku (serwer "widzi" obrot).
			player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
					.Rot(rotations[0], rotations[1], player.onGround()));
		} else {
			RotationUtil.smoothFacePos(player, target.getEyePosition(), speed.get().floatValue());
		}
	}
}
