package pl.memehub.modules.combat;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.EnumSetting;
import pl.memehub.core.settings.NumberSetting;
import pl.memehub.util.EntityUtil;
import pl.memehub.util.RotationUtil;

import java.util.Comparator;
import java.util.List;

/**
 * KillAura - automatyczny atak na najblizszy cel.
 *
 * <p>Opcje: zasieg (Reach), tryb rotacji (NONE / INSTANT / SMOOTH),
 * priorytet celu (DISTANCE / HEALTH / ARMOR), filtry celow (gracze,
 * potwory, zwierzeta), opoznienie miedzy atakami oraz wymog gotowosci
 * ataku (attack cooldown).
 */
public final class KillAura extends Module {

	public enum RotationMode {
		NONE, INSTANT, SMOOTH
	}

	public enum Priority {
		DISTANCE, HEALTH, ARMOR
	}

	private final NumberSetting range = new NumberSetting("Reach", "Zasieg ataku (bloki)", 3.2, 1.0, 6.0, 0.1);
	private final EnumSetting<RotationMode> rotationMode = new EnumSetting<>("Rotation", "Tryb rotacji", RotationMode.SMOOTH);
	private final NumberSetting rotationSpeed = new NumberSetting("Rotation Speed", "Predkosc obrotu (SMOOTH)", 3.0, 0.5, 10.0, 0.5);
	private final NumberSetting rotationTolerance = new NumberSetting("Rotate Tolerance", "Tolerancja wycelowania (stopnie)", 15.0, 1.0, 60.0, 1.0);
	private final EnumSetting<Priority> priority = new EnumSetting<>("Priority", "Priorytet wyboru celu", Priority.DISTANCE);
	private final BooleanSetting targetPlayers = new BooleanSetting("Players", "Atakuj graczy", true);
	private final BooleanSetting targetMonsters = new BooleanSetting("Monsters", "Atakuj potwory", true);
	private final BooleanSetting targetAnimals = new BooleanSetting("Animals", "Atakuj zwierzeta", false);
	private final NumberSetting attackDelay = new NumberSetting("Attack Delay", "Opoznienie miedzy atakami (ms)", 300, 0, 2000, 10);
	private final BooleanSetting waitForCooldown = new BooleanSetting("Wait Cooldown", "Czekaj na pelny attack cooldown (1.9+)", true);
	private final BooleanSetting requireLooking = new BooleanSetting("Require Looking", "Atakuj tylko gdy cel jest mniej wiecej na celowniku", false);
	private final BooleanSetting swing = new BooleanSetting("Swing", "Animacja zamachu", true);

	private long lastAttack = 0;

	public KillAura() {
		super("KillAura", "Automatyczny atak na najblizszy cel", Category.COMBAT);
		addSetting(range);
		addSetting(rotationMode);
		addSetting(rotationSpeed);
		addSetting(rotationTolerance);
		addSetting(priority);
		addSetting(targetPlayers);
		addSetting(targetMonsters);
		addSetting(targetAnimals);
		addSetting(attackDelay);
		addSetting(waitForCooldown);
		addSetting(requireLooking);
		addSetting(swing);
	}

	@Override
	protected void onEnable() {
		lastAttack = 0;
	}

	@Override
	public void onTick() {
		var mc = mc();
		var player = player();
		if (mc.level == null || player == null || mc.gameMode == null) {
			return;
		}

		List<LivingEntity> targets = EntityUtil.getTargetsInRange(player, range.get(),
				targetPlayers.get(), targetMonsters.get(), targetAnimals.get());
		if (targets.isEmpty()) {
			return;
		}
		targets.sort(targetComparator(player));
		LivingEntity target = targets.get(0);

		// Rotacja
		boolean aimed = true;
		if (rotationMode.get() != RotationMode.NONE) {
			Vec3 aim = target.getEyePosition();
			if (rotationMode.get() == RotationMode.INSTANT) {
				RotationUtil.facePos(player, aim);
			} else {
				aimed = RotationUtil.smoothFacePos(player, aim, rotationSpeed.get().floatValue());
			}
		}
		if (requireLooking.get() && RotationUtil.angleToPos(player, target.getEyePosition()) > rotationTolerance.get()) {
			return;
		}
		if (!aimed && rotationMode.get() == RotationMode.SMOOTH) {
			return;
		}
		if (waitForCooldown.get() && player.getAttackStrengthScale(0.5f) < 0.9f) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - lastAttack < attackDelay.longValue()) {
			return;
		}

		mc.gameMode.attack(target);
		if (swing.get()) {
			player.swing(InteractionHand.MAIN_HAND);
		}
		lastAttack = now;
	}

	private Comparator<LivingEntity> targetComparator(Player player) {
		return switch (priority.get()) {
			case DISTANCE -> Comparator.comparingDouble(player::distanceToSqr);
			case HEALTH -> Comparator.comparingDouble(LivingEntity::getHealth);
			case ARMOR -> Comparator.comparingInt(LivingEntity::getArmorValue).reversed();
		};
	}

	@SuppressWarnings("unused")
	private float yawTo(Vec3 from, Vec3 to) {
		double dx = to.x - from.x;
		double dz = to.z - from.z;
		return (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
	}

	@SuppressWarnings("unused")
	private float wrap(float angle) {
		return Mth.wrapDegrees(angle);
	}
}
