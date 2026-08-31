package pl.memehub.modules.combat;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.NumberSetting;
import pl.memehub.util.EntityUtil;

/**
 * Triggerbot - automatyczny atak w momencie najechania celownikiem na wroga.
 */
public final class Triggerbot extends Module {

	private final NumberSetting range = new NumberSetting("Range", "Zasieg ataku (bloki)", 3.2, 1.0, 6.0, 0.1);
	private final NumberSetting delay = new NumberSetting("Delay", "Opoznienie miedzy atakami (ms)", 200, 0, 1000, 10);
	private final BooleanSetting targetPlayers = new BooleanSetting("Players", "Atakuj graczy", true);
	private final BooleanSetting targetMonsters = new BooleanSetting("Monsters", "Atakuj potwory", true);
	private final BooleanSetting targetAnimals = new BooleanSetting("Animals", "Atakuj zwierzeta", false);
	private final BooleanSetting waitForCooldown = new BooleanSetting("Wait Cooldown", "Czekaj na pelny attack cooldown", true);

	private long lastAttack = 0;

	public Triggerbot() {
		super("Triggerbot", "Atakuje, gdy celownik najedzie na wroga", Category.COMBAT);
		addSetting(range);
		addSetting(delay);
		addSetting(targetPlayers);
		addSetting(targetMonsters);
		addSetting(targetAnimals);
		addSetting(waitForCooldown);
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
		HitResult hit = mc.hitResult;
		if (!(hit instanceof EntityHitResult entityHit)) {
			return;
		}
		if (!(entityHit.getEntity() instanceof LivingEntity target)) {
			return;
		}
		if (!EntityUtil.isValidTarget(target, player, targetPlayers.get(), targetMonsters.get(), targetAnimals.get())) {
			return;
		}
		if (player.distanceToSqr(target) > range.get() * range.get()) {
			return;
		}
		if (waitForCooldown.get() && player.getAttackStrengthScale(0.5f) < 0.9f) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - lastAttack < delay.longValue()) {
			return;
		}

		mc.gameMode.attack(target);
		player.swing(InteractionHand.MAIN_HAND);
		lastAttack = now;
	}
}
