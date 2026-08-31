package pl.memehub.modules.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.EnumSetting;
import pl.memehub.core.settings.NumberSetting;
import pl.memehub.util.ChatUtil;
import pl.memehub.util.DamageMath;
import pl.memehub.util.EntityUtil;
import pl.memehub.util.InteractionUtil;
import pl.memehub.util.InventoryUtil;
import pl.memehub.util.RotationUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * AutoCrystal - automatyczne stawianie i niszczenie krysztalow Endu.
 *
 * <p>Funkcje:
 * <ul>
 *   <li>niszczenie krysztalow w zasiegu {@code breakRange} (atak jak zwykly gracz),</li>
 *   <li>stawianie krysztalow nad celem / wokol celu z pelna walidacja pozycji,</li>
 *   <li>wyliczanie obrazen (DamageMath) - wybor najlepszej pozycji i krysztalow,</li>
 *   <li>opoznienia place/break/switch, obrot w strone akcji,</li>
 *   <li>uzywanie krysztalow z rece glownej LUB drugiej rece (obydwie rece).</li>
 * </ul>
 */
public final class AutoCrystal extends Module {

	public enum TargetMode {
		PLAYERS, MOBS, SELF
	}

	public enum PlaceMode {
		ABOVE, SURROUND
	}

	private final EnumSetting<TargetMode> targetMode = new EnumSetting<>("Target", "Kogo atakujemy", TargetMode.PLAYERS);
	private final EnumSetting<PlaceMode> placeMode = new EnumSetting<>("Place Mode", "Gdzie stawiamy krysztaly", PlaceMode.ABOVE);

	private final NumberSetting placeRange = new NumberSetting("Place Range", "Zasieg stawiania (bloki)", 4.5, 1.0, 7.0, 0.1);
	private final NumberSetting breakRange = new NumberSetting("Break Range", "Zasieg niszczenia (bloki)", 4.5, 1.0, 7.0, 0.1);
	private final NumberSetting placeDelay = new NumberSetting("Place Delay", "Opoznienie stawiania (ms)", 150, 0, 2000, 10);
	private final NumberSetting breakDelay = new NumberSetting("Break Delay", "Opoznienie niszczenia (ms)", 100, 0, 2000, 10);
	private final NumberSetting switchDelay = new NumberSetting("Switch Delay", "Opoznienie zmiany przedmiotu (ms)", 100, 0, 1000, 10);

	private final NumberSetting minDamage = new NumberSetting("Min Damage", "Minimalne obrazenia na celu", 6.0, 0.0, 40.0, 1.0);
	private final NumberSetting maxSelfDamage = new NumberSetting("Max Self Damage", "Maks. obrazenia na sobie", 10.0, 0.0, 40.0, 1.0);

	private final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", "Automatyczna zmiana na krysztal", true);
	private final BooleanSetting preferOffhand = new BooleanSetting("Prefer Offhand", "Uzywaj krysztalu z drugiej reki, gdy jest dostepny", true);
	private final BooleanSetting rotate = new BooleanSetting("Rotate", "Obracaj gracza w strone akcji", true);
	private final BooleanSetting swing = new BooleanSetting("Swing", "Animacja zamachu", true);
	private final BooleanSetting swapBack = new BooleanSetting("Swap Back", "Po stawianiu wroc do poprzedniego slotu", false);
	private final BooleanSetting breakOnSelf = new BooleanSetting("Break Own Crystal", "Niszcz wlasne krysztaly (testy)", false);

	private long lastPlace = 0;
	private long lastBreak = 0;
	private long lastSwitch = 0;
	private int previousSlot = -1;
	private boolean placing = false;

	public AutoCrystal() {
		super("AutoCrystal", "Automatyczne stawianie i niszczenie krysztalow Endu", Category.COMBAT);
		addSetting(targetMode);
		addSetting(placeMode);
		addSetting(placeRange);
		addSetting(breakRange);
		addSetting(placeDelay);
		addSetting(breakDelay);
		addSetting(switchDelay);
		addSetting(minDamage);
		addSetting(maxSelfDamage);
		addSetting(autoSwitch);
		addSetting(preferOffhand);
		addSetting(rotate);
		addSetting(swing);
		addSetting(swapBack);
		addSetting(breakOnSelf);
	}

	@Override
	protected void onEnable() {
		lastPlace = 0;
		lastBreak = 0;
		previousSlot = -1;
		placing = false;
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
			restoreSlot(player);
			return;
		}

		long now = Util.getMillis();

		// Faza niszczenia: znajdz krysztal wokol celu.
		Entity crystal = findCrystalNearTarget(mc.level, target, player);
		if (crystal != null && now - lastBreak >= breakDelay.longValue() && !placing) {
			restoreSlot(player);
			if (rotate.get() && player.connection != null) {
				RotationUtil.facePos(player, crystal.position());
			}
			mc.gameMode.attack(crystal);
			if (swing.get()) {
				player.swing(InteractionHand.MAIN_HAND);
			}
			lastBreak = now;
			return;
		}

		// Faza stawiania: znajdz najlepsza pozycje.
		BlockPos bestPos = findBestPlacePos(mc.level, target, player);
		if (bestPos == null) {
			restoreSlot(player);
			return;
		}
		if (now - lastPlace < placeDelay.longValue()) {
			return;
		}
		if (now - lastSwitch < switchDelay.longValue()) {
			return;
		}

		InteractionHand hand = handWithCrystal(player);
		if (hand == null) {
			if (!autoSwitch.get() || !InventoryUtil.selectItem(mc, Items.END_CRYSTAL, true)) {
				restoreSlot(player);
				return;
			}
			hand = InteractionHand.MAIN_HAND;
			previousSlot = player.getInventory().selected;
			placing = true;
			lastSwitch = now;
		}

		if (rotate.get() && player.connection != null) {
			RotationUtil.facePos(player, Vec3.atCenterOf(bestPos));
		}
		if (hand == InteractionHand.MAIN_HAND) {
			InteractionUtil.placeBlockAt(mc, bestPos);
		} else {
			// Stawianie z drugiej reki.
			InteractionUtil.useItemOnBlock(mc, bestPos.below(), Direction.UP);
		}
		if (swing.get()) {
			player.swing(hand);
		}
		lastPlace = now;
	}

	// ------------------------------------------------------------------
	// Wybor celu
	// ------------------------------------------------------------------

	private LivingEntity findTarget(Player player, Level level) {
		return switch (targetMode.get()) {
			case SELF -> player;
			case MOBS -> EntityUtil.getNearestTarget(player, Math.max(placeRange.get(), breakRange.get()) + 2.0,
					false, true, true);
			case PLAYERS -> {
				LivingEntity nearestPlayer = EntityUtil.getNearestTarget(player, Math.max(placeRange.get(), breakRange.get()) + 2.0,
						true, false, false);
				yield nearestPlayer != null ? nearestPlayer
						: EntityUtil.getNearestTarget(player, Math.max(placeRange.get(), breakRange.get()) + 2.0,
								false, true, true);
			}
		};
	}

	// ------------------------------------------------------------------
	// Niszczenie krysztalow
	// ------------------------------------------------------------------

	private Entity findCrystalNearTarget(Level level, LivingEntity target, Player player) {
		double range = breakRange.get();
		AABB area = target.getBoundingBox().inflate(range);
		Entity best = null;
		double bestDist = Double.MAX_VALUE;
		for (Entity entity : level.getEntities(target, area, e -> e.getType() == EntityTypes.END_CRYSTAL && e.isAlive())) {
			if (!breakOnSelf.get() && entity.distanceToSqr(player) > range * range) {
				continue;
			}
			if (player.distanceToSqr(entity) > range * range) {
				continue;
			}
			if (minDamage.get() > 0.0 && target instanceof LivingEntity living
					&& DamageMath.crystalDamage(level, entity.position(), living) < minDamage.get()) {
				continue;
			}
			double dist = target.distanceToSqr(entity);
			if (dist < bestDist) {
				bestDist = dist;
				best = entity;
			}
		}
		return best;
	}

	// ------------------------------------------------------------------
	// Stawianie krysztalow
	// ------------------------------------------------------------------

	private BlockPos findBestPlacePos(Level level, LivingEntity target, Player player) {
		List<BlockPos> candidates = new ArrayList<>();
		BlockPos base = target.blockPosition();
		if (placeMode.get() == PlaceMode.ABOVE) {
			candidates.add(base.above());
		} else {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dz == 0) {
						continue;
					}
					candidates.add(base.above().offset(dx, 0, dz));
				}
			}
		}

		BlockPos best = null;
		float bestScore = -1.0F;
		for (BlockPos pos : candidates) {
			if (!canPlace(level, pos, player, target)) {
				continue;
			}
			float damage = DamageMath.crystalDamage(level, crystalSource(pos), target);
			if (damage < minDamage.get()) {
				continue;
			}
			float selfDamage = DamageMath.crystalDamage(level, crystalSource(pos), player);
			if (maxSelfDamage.get() > 0.0 && selfDamage > maxSelfDamage.get()) {
				continue;
			}
			float score = damage - selfDamage * 0.5F;
			if (score > bestScore) {
				bestScore = score;
				best = pos;
			}
		}
		return best;
	}

	private boolean canPlace(Level level, BlockPos pos, Player player, LivingEntity target) {
		if (pos.getY() < level.getMinY() || pos.getY() + 1 > level.getMaxY()) {
			return false;
		}
		// Krysztal potrzebuje dwoch blokow powietrza.
		if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
			return false;
		}
		if (level.getBlockState(pos.below()).isAir()) {
			return false;
		}
		// Brak encji w miejscu krysztalu.
		AABB box = new AABB(pos, pos.above());
		if (!level.getEntities(target, box).isEmpty()) {
			return false;
		}
		// Zasieg stawiania od gracza.
		if (player.distanceToSqr(Vec3.atCenterOf(pos)) > placeRange.get() * placeRange.get()) {
			return false;
		}
		return true;
	}

	private Vec3 crystalSource(BlockPos pos) {
		return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
	}

	/** Reka z krysztalem: glownej lub drugiej (zgodnie z preferencjami). */
	private InteractionHand handWithCrystal(Player player) {
		if (player.getMainHandItem().is(Items.END_CRYSTAL)) {
			return InteractionHand.MAIN_HAND;
		}
		if (preferOffhand.get() && player.getOffhandItem().is(Items.END_CRYSTAL)) {
			return InteractionHand.OFF_HAND;
		}
		if (!preferOffhand.get() && player.getOffhandItem().is(Items.END_CRYSTAL)) {
			return InteractionHand.OFF_HAND;
		}
		return null;
	}

	private void restoreSlot(Player player) {
		if (placing && swapBack.get() && previousSlot >= 0 && player != null) {
			player.getInventory().selected = previousSlot;
			placing = false;
			previousSlot = -1;
		}
	}

	@Override
	public void onDisable() {
		restoreSlot(player());
	}

	@SuppressWarnings("unused")
	private void log(String message) {
		ChatUtil.info("\u00a7d[AutoCrystal]\u00a7r " + message);
	}
}
