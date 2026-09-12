package pl.memehub.modules.combat;

import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.NumberSetting;
import pl.memehub.util.EntityUtil;
import pl.memehub.util.InventoryUtil;
import pl.memehub.util.RotationUtil;

/**
 * Mace Smash Assist - automatyczne przelaczenie na Bulave (Mace) i atak
 * dokladnie w momencie osiagniecia maksymalnej predkosci opadania z wysokosci,
 * aby zadac najwyzsze mozliwe obrazenia (skalowanie obrazen bulawy od
 * wysokosci spadku).
 */
public final class MaceSmashAssist extends Module {

	private final NumberSetting minFallDistance = new NumberSetting("Min Fall Distance", "Minimalna wysokosc spadku (bloki)", 3.0, 1.0, 20.0, 0.5);
	private final NumberSetting minFallSpeed = new NumberSetting("Min Fall Speed", "Min. predkosc opadania (Y/s)", 1.2, 0.5, 5.0, 0.1);
	private final NumberSetting switchDelay = new NumberSetting("Switch Delay", "Opoznienie zmiany na bulave (ms)", 100, 0, 1000, 10);
	private final NumberSetting switchBackDelay = new NumberSetting("Switch Back Delay", "Po ilu ms wrocic do poprzedniego slotu (0 = nie wracaj)", 500.0, 0.0, 3000.0, 50.0);
	private final NumberSetting attackCooldown = new NumberSetting("Attack Cooldown", "Opoznienie miedzy atakami (ms)", 600, 0, 3000, 50);
	private final BooleanSetting requireTarget = new BooleanSetting("Require Target", "Atakuj tylko gdy cel jest w zasiegu", true);
	private final NumberSetting targetRange = new NumberSetting("Target Range", "Zasieg szukania celu (bloki)", 4.0, 1.0, 8.0, 0.5);
	private final BooleanSetting rotate = new BooleanSetting("Rotate", "Obracaj gracza w strone celu", true);

	private long switchTime = 0;
	private long lastAttack = 0;
	private int previousSlot = -1;
	private boolean switchingBack = false;

	public MaceSmashAssist() {
		super("Mace Smash Assist", "Automatyczny atak bulawa przy maksymalnej predkosci opadania", Category.COMBAT);
		addSetting(minFallDistance);
		addSetting(minFallSpeed);
		addSetting(switchDelay);
		addSetting(switchBackDelay);
		addSetting(attackCooldown);
		addSetting(requireTarget);
		addSetting(targetRange);
		addSetting(rotate);
	}

	@Override
	protected void onEnable() {
		switchTime = 0;
		lastAttack = 0;
		previousSlot = -1;
		switchingBack = false;
	}

	@Override
	public void onTick() {
		var mc = mc();
		var player = player();
		if (mc.level == null || player == null || mc.gameMode == null) {
			return;
		}

		// Powrot do poprzedniego slotu po ataku.
		if (switchingBack && switchBackDelay.get() > 0.0
				&& Util.getMillis() - lastAttack >= switchBackDelay.longValue()) {
			player.getInventory().setSelectedSlot(previousSlot);
			switchingBack = false;
			previousSlot = -1;
		}

		// Warunki opadania.
		if (player.onGround() || player.isFallFlying()
				|| player.isInWater() || player.isInLava()) {
			return;
		}
		if (player.fallDistance < minFallDistance.get()) {
			return;
		}
		if (player.getDeltaMovement().y > -minFallSpeed.get()) {
			return;
		}

		LivingEntity target = null;
		if (requireTarget.get()) {
			target = EntityUtil.getNearestTarget(player, targetRange.get(), true, true, false);
			if (target == null) {
				return;
			}
		}

		// Przelacz na bulave, jesli nie trzymamy jej w rece.
		if (!player.getMainHandItem().is(Items.MACE) && !player.getOffhandItem().is(Items.MACE)) {
			if (previousSlot == -1) {
				previousSlot = player.getInventory().getSelectedSlot();
			}
			if (!InventoryUtil.selectItem(mc, Items.MACE, true)) {
				return;
			}
			switchTime = Util.getMillis();
			return;
		}
		if (Util.getMillis() - switchTime < switchDelay.longValue()) {
			return;
		}

		long now = Util.getMillis();
		if (now - lastAttack < attackCooldown.longValue()) {
			return;
		}

		// ATUT: atak w szczycie predkosci opadania.
		if (target != null && rotate.get()) {
			RotationUtil.facePos(player, target.getEyePosition());
		}
		if (target != null) {
			mc.gameMode.attack(player, target);
		}
		player.swing(InteractionHand.MAIN_HAND);
		lastAttack = now;

		if (switchBackDelay.get() > 0.0 && previousSlot != -1) {
			switchingBack = true;
		}
	}

	@Override
	protected void onDisable() {
		var player = player();
		if (player != null && previousSlot != -1 && switchingBack) {
			player.getInventory().setSelectedSlot(previousSlot);
		}
		previousSlot = -1;
		switchingBack = false;
	}
}
