package pl.memehub.modules.combat;

import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.NumberSetting;
import pl.memehub.util.EntityUtil;
import pl.memehub.util.InteractionUtil;
import pl.memehub.util.InventoryUtil;
import pl.memehub.util.RotationUtil;

/**
 * Wind Charge Synergy - automatyczne rzucenie Wind Charge pod nogi gracza
 * (wybicie sie w gore), a nastepnie natychmiastowy atak Bulawa z powietrza.
 *
 * <p>Maszyna stanow: IDLE -&gt; THROW -&gt; WAIT_RISE -&gt; STRIKE -&gt; COOLDOWN.
 */
public final class WindChargeSynergy extends Module {

	private enum State {
		IDLE, WAIT_RISE, STRIKE, COOLDOWN
	}

	private final NumberSetting cooldown = new NumberSetting("Cooldown", "Przerwa miedzy cyklami (ms)", 2500, 0, 10000, 100);
	private final NumberSetting minFallDistance = new NumberSetting("Min Fall Distance", "Min. wysokosc spadku przed atakiem (bloki)", 2.0, 1.0, 15.0, 0.5);
	private final NumberSetting strikeFallSpeed = new NumberSetting("Strike Fall Speed", "Min. predkosc opadania do ataku (Y/s)", 1.0, 0.3, 4.0, 0.1);
	private final NumberSetting switchDelay = new NumberSetting("Switch Delay", "Opoznienie zmiany na bulave (ms)", 100, 0, 1000, 10);
	private final BooleanSetting requireTarget = new BooleanSetting("Require Target", "Atakuj tylko gdy cel jest w zasiegu", true);
	private final NumberSetting targetRange = new NumberSetting("Target Range", "Zasieg szukania celu (bloki)", 4.0, 1.0, 8.0, 0.5);
	private final BooleanSetting rotate = new BooleanSetting("Rotate", "Obracaj gracza w strone celu", true);

	private State state = State.IDLE;
	private long stateTime = 0;
	private long switchTime = 0;
	private double riseY = 0.0;

	public WindChargeSynergy() {
		super("Wind Charge Synergy", "Wind Charge pod nogi + natychmiastowy atak bulawa z powietrza", Category.COMBAT);
		addSetting(cooldown);
		addSetting(minFallDistance);
		addSetting(strikeFallSpeed);
		addSetting(switchDelay);
		addSetting(requireTarget);
		addSetting(targetRange);
		addSetting(rotate);
	}

	@Override
	protected void onEnable() {
		state = State.IDLE;
		stateTime = 0;
		riseY = 0.0;
	}

	@Override
	public void onTick() {
		var mc = mc();
		var player = player();
		if (mc.level == null || player == null || mc.gameMode == null) {
			return;
		}

		long now = Util.getMillis();
		switch (state) {
			case IDLE -> {
				if (now - stateTime < cooldown.longValue() || !player.onGround()) {
					return;
				}
				// Rzuc Wind Charge w ziemie pod graczem.
				if (!InventoryUtil.selectItem(mc, Items.WIND_CHARGE, true)) {
					return;
				}
				if (rotate.get()) {
					RotationUtil.facePos(player, player.getEyePosition().subtract(0, 0.4, 0));
				}
				InteractionUtil.useItemMainHand(mc);
				riseY = player.getY();
				state = State.WAIT_RISE;
				stateTime = now;
			}
			case WAIT_RISE -> {
				if (now - stateTime > 3000) {
					state = State.IDLE;
					return;
				}
				if (player.getY() > riseY + 1.0 || !player.onGround()) {
					state = State.STRIKE;
					stateTime = now;
				}
			}
			case STRIKE -> {
				if (now - stateTime > 4000) {
					state = State.IDLE;
					return;
				}
				if (player.onGround() || player.isInWater() || player.isInLava()) {
					state = State.IDLE;
					return;
				}
				if (player.fallDistance < minFallDistance.get()) {
					return;
				}
				if (player.getDeltaMovement().y > -strikeFallSpeed.get()) {
					return;
				}
				LivingEntity target = EntityUtil.getNearestTarget(player, targetRange.get(), true, true, false);
				if (requireTarget.get() && target == null) {
					state = State.IDLE;
					return;
				}
				if (!player.getMainHandItem().is(Items.MACE)) {
					if (!InventoryUtil.selectItem(mc, Items.MACE, true)) {
						state = State.IDLE;
						return;
					}
					switchTime = now;
					return;
				}
				if (now - switchTime < switchDelay.longValue()) {
					return;
				}
				if (target != null) {
					if (rotate.get()) {
						RotationUtil.facePos(player, target.getEyePosition());
					}
					mc.gameMode.attack(target);
				}
				player.swing(InteractionHand.MAIN_HAND);
				state = State.COOLDOWN;
				stateTime = now;
			}
			case COOLDOWN -> {
				if (now - stateTime >= cooldown.longValue()) {
					state = State.IDLE;
					stateTime = now;
				}
			}
		}
	}
}
