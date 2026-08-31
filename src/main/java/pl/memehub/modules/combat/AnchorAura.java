package pl.memehub.modules.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.phys.Vec3;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.EnumSetting;
import pl.memehub.core.settings.NumberSetting;
import pl.memehub.util.DamageMath;
import pl.memehub.util.EntityUtil;
import pl.memehub.util.InteractionUtil;
import pl.memehub.util.InventoryUtil;
import pl.memehub.util.RotationUtil;

/**
 * AnchorAura - automatyczne ladowanie Respawn Anchorow jasnota (glowstone)
 * i ich eksplozja przy celu.
 *
 * <p>Fazy: PLACE (postaw anchor pod celem) -&gt; CHARGE (klikaj jasnota az do
 * 4 ladowan) -&gt; EXPLODE (prawy klik pusta reka - eksplozja jak lozko)
 * -&gt; COOLDOWN (odczekaj i powtorz).
 */
public final class AnchorAura extends Module {

	public enum TargetMode {
		PLAYERS, MOBS, SELF
	}

	public enum Phase {
		PLACE, CHARGE, EXPLODE, COOLDOWN
	}

	private final EnumSetting<TargetMode> targetMode = new EnumSetting<>("Target", "Kogo atakujemy", TargetMode.PLAYERS);
	private final NumberSetting range = new NumberSetting("Range", "Zasieg dzialania (bloki)", 4.0, 1.0, 7.0, 0.1);
	private final NumberSetting placeDelay = new NumberSetting("Place Delay", "Opoznienie stawiania (ms)", 200, 0, 2000, 10);
	private final NumberSetting chargeDelay = new NumberSetting("Charge Delay", "Opoznienie miedzy ladowaniami (ms)", 150, 0, 2000, 10);
	private final NumberSetting explodeDelay = new NumberSetting("Explode Delay", "Opoznienie eksplozji (ms)", 150, 0, 2000, 10);
	private final NumberSetting cooldown = new NumberSetting("Cooldown", "Przerwa po eksplozji (ms)", 1500, 0, 10000, 100);
	private final BooleanSetting rotate = new BooleanSetting("Rotate", "Obracaj gracza w strone akcji", true);
	private final BooleanSetting autoPlace = new BooleanSetting("Auto Place", "Stawiaj anchor, gdy go brakuje", true);

	private Phase phase = Phase.PLACE;
	private long lastAction = 0;
	private int chargeCount = 0;
	private long cooldownUntil = 0;

	public AnchorAura() {
		super("AnchorAura", "Automatyczne ladowanie Respawn Anchorow i eksplozja przy celu", Category.COMBAT);
		addSetting(targetMode);
		addSetting(range);
		addSetting(placeDelay);
		addSetting(chargeDelay);
		addSetting(explodeDelay);
		addSetting(cooldown);
		addSetting(rotate);
		addSetting(autoPlace);
	}

	@Override
	protected void onEnable() {
		phase = Phase.PLACE;
		chargeCount = 0;
		cooldownUntil = 0;
	}

	@Override
	public void onTick() {
		var mc = mc();
		var player = player();
		if (mc.level == null || player == null || mc.gameMode == null) {
			return;
		}
		LivingEntity target = findTarget(player, mc.level);
		if (target == null) {
			phase = Phase.PLACE;
			return;
		}

		BlockPos anchorPos = target.blockPosition();
		long now = Util.getMillis();
		if (now < cooldownUntil) {
			return;
		}

		switch (phase) {
			case PLACE -> {
				if (!mc.level.getBlockState(anchorPos).is(Blocks.RESPAWN_ANCHOR)) {
					if (!autoPlace.get()) {
						return;
					}
					if (now - lastAction < placeDelay.longValue()) {
						return;
					}
					if (!InventoryUtil.selectItem(mc, Items.RESPAWN_ANCHOR, true)) {
						return;
					}
					if (rotate.get()) {
						RotationUtil.facePos(player, Vec3.atCenterOf(anchorPos));
					}
					InteractionUtil.placeBlockAt(mc, anchorPos);
					lastAction = now;
					return;
				}
				phase = Phase.CHARGE;
				chargeCount = mc.level.getBlockState(anchorPos).getValue(RespawnAnchorBlock.CHARGE);
			}
			case CHARGE -> {
				int charges = mc.level.getBlockState(anchorPos).getValue(RespawnAnchorBlock.CHARGE);
				if (charges >= 4) {
					phase = Phase.EXPLODE;
					return;
				}
				if (now - lastAction < chargeDelay.longValue()) {
					return;
				}
				if (!InventoryUtil.selectItem(mc, Items.GLOWSTONE, true)) {
					return;
				}
				if (rotate.get()) {
					RotationUtil.facePos(player, Vec3.atCenterOf(anchorPos));
				}
				InteractionUtil.useItemOnBlock(mc, anchorPos, Direction.UP);
				lastAction = now;
				chargeCount = charges + 1;
			}
			case EXPLODE -> {
				if (now - lastAction < explodeDelay.longValue()) {
					return;
				}
				// Eksplozja: prawy klik naladowanego anchoru pusta reka.
				selectEmptyHand(player);
				if (rotate.get()) {
					RotationUtil.facePos(player, Vec3.atCenterOf(anchorPos));
				}
				InteractionUtil.useItemOnBlock(mc, anchorPos, Direction.UP);
				lastAction = now;
				phase = Phase.COOLDOWN;
				cooldownUntil = now + cooldown.longValue();
			}
			case COOLDOWN -> {
				chargeCount = 0;
				phase = Phase.PLACE;
			}
		}
	}

	private void selectEmptyHand(Player player) {
		var inventory = player.getInventory();
		int empty = InventoryUtil.findEmptySlot(inventory);
		if (empty != -1 && empty < 9) {
			inventory.setSelectedSlot(empty);
		} else if (empty != -1) {
			// Zamien pusty slot z aktualnie wybranym.
			var stack = inventory.getItem(inventory.getSelectedSlot());
			inventory.setItem(inventory.getSelectedSlot(), inventory.getItem(empty));
			inventory.setItem(empty, stack);
			player.containerMenu.broadcastChanges();
		}
	}

	private LivingEntity findTarget(Player player, Level level) {
		return switch (targetMode.get()) {
			case SELF -> player;
			case MOBS -> EntityUtil.getNearestTarget(player, range.get(), false, true, true);
			case PLAYERS -> {
				LivingEntity nearest = EntityUtil.getNearestTarget(player, range.get(), true, false, false);
				yield nearest != null ? nearest : EntityUtil.getNearestTarget(player, range.get(), false, true, true);
			}
		};
	}

	/** Pogladowe obrazenia anchoru (do HUD / logow). */
	public float predictedDamage(Level level, BlockPos anchorPos, LivingEntity target) {
		return DamageMath.anchorDamage(level, anchorPos, target);
	}
}
