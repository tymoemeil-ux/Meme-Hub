package pl.memehub.modules.vehicle;

import net.minecraft.client.Minecraft;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.EnumSetting;
import pl.memehub.core.settings.NumberSetting;
import pl.memehub.util.EntityUtil;
import pl.memehub.util.InteractionUtil;
import pl.memehub.util.InventoryUtil;
import pl.memehub.util.RotationUtil;

/**
 * TNT Cart Auto-Placer - automatyczne stawianie torow, wozkow z TNT
 * i ich natychmiastowa detonacja (atak w ulamek sekundy po postawieniu).
 *
 * <p>Kolejnosc: poloz tor -> postaw wozek TNT -> uderz wozek (destrukcja
 * wozka w ruchu wywoluje eksplozje; w pojedynczym swiecie testowym mozna
 * tez uzyc trybu "Primed" z aktywatorowym torem).
 */
public final class CartPlacer extends Module {

	public enum PlaceMode {
		SELF, TARGET
	}

	private final EnumSetting<PlaceMode> placeMode = new EnumSetting<>("Place Mode", "Gdzie stawiamy wozek", PlaceMode.SELF);
	private final NumberSetting range = new NumberSetting("Range", "Zasieg stawiania (bloki)", 4.0, 1.0, 8.0, 0.5);
	private final BooleanSetting placeRail = new BooleanSetting("Place Rail", "Stawiaj tor pod wozkiem", true);
	private final BooleanSetting placeCart = new BooleanSetting("Place Cart", "Stawiaj wozek z TNT", true);
	private final BooleanSetting detonate = new BooleanSetting("Detonate", "Natychmiastowa detonacja (atak na wozek)", true);
	private final NumberSetting stepDelay = new NumberSetting("Step Delay", "Opoznienie miedzy krokami (ms)", 80, 0, 1000, 10);
	private final NumberSetting cooldown = new NumberSetting("Cooldown", "Przerwa po cyklu (ms)", 1500, 0, 10000, 100);
	private final BooleanSetting rotate = new BooleanSetting("Rotate", "Obracaj gracza w strone akcji", true);

	private enum Step {
		IDLE, RAIL, CART, DETONATE, WAIT
	}

	private Step step = Step.IDLE;
	private long stepTime = 0;
	private BlockPos cartPos = null;

	public CartPlacer() {
		super("TNT Cart Auto-Placer", "Automatyczne stawianie toru, wozka TNT i jego detonacja", Category.COMBAT);
		addSetting(placeMode);
		addSetting(range);
		addSetting(placeRail);
		addSetting(placeCart);
		addSetting(detonate);
		addSetting(stepDelay);
		addSetting(cooldown);
		addSetting(rotate);
	}

	@Override
	protected void onEnable() {
		step = Step.IDLE;
		cartPos = null;
	}

	@Override
	public void onTick() {
		var mc = mc();
		var player = player();
		if (mc.level == null || player == null || mc.gameMode == null) {
			return;
		}

		long now = Util.getMillis();
		BlockPos targetPos = targetBlockPos(mc.level, player);
		if (targetPos == null) {
			step = Step.IDLE;
			return;
		}

		switch (step) {
			case IDLE -> {
				if (now - stepTime < cooldown.longValue()) {
					return;
				}
				cartPos = targetPos;
				if (placeRail.get()) {
					step = Step.RAIL;
				} else if (placeCart.get()) {
					step = Step.CART;
				} else {
					step = Step.DETONATE;
				}
				stepTime = now;
			}
			case RAIL -> {
				if (now - stepTime < stepDelay.longValue()) {
					return;
				}
				if (placeRailIfNeeded(mc, player, cartPos)) {
					step = placeCart.get() ? Step.CART : Step.DETONATE;
				}
				stepTime = now;
			}
			case CART -> {
				if (now - stepTime < stepDelay.longValue()) {
					return;
				}
				if (placeCartIfNeeded(mc, player, cartPos)) {
					step = detonate.get() ? Step.DETONATE : Step.WAIT;
				}
				stepTime = now;
			}
			case DETONATE -> {
				if (now - stepTime < stepDelay.longValue()) {
					return;
				}
				Entity cart = findTntCart(mc.level, cartPos);
				if (cart != null) {
					if (rotate.get()) {
						RotationUtil.facePos(player, cart.position());
					}
					mc.gameMode.attack(cart);
					player.swing(InteractionHand.MAIN_HAND);
				}
				step = Step.WAIT;
				stepTime = now;
			}
			case WAIT -> {
				if (now - stepTime >= cooldown.longValue()) {
					step = Step.IDLE;
					stepTime = now;
				}
			}
		}
	}

	// ------------------------------------------------------------------
	// Pomocnicze
	// ------------------------------------------------------------------

	/** Pozycja pod encja docelowa (tor + wozek stana pod nogami). */
	private BlockPos targetBlockPos(Level level, Player player) {
		BlockPos base;
		if (placeMode.get() == PlaceMode.SELF) {
			base = player.blockPosition().below();
		} else {
			LivingEntity target = EntityUtil.getNearestTarget(player, range.get(), true, true, false);
			if (target == null) {
				return null;
			}
			base = target.blockPosition().below();
		}
		if (player.distanceToSqr(Vec3.atCenterOf(base)) > range.get() * range.get()) {
			return null;
		}
		return base;
	}

	private boolean placeRailIfNeeded(Minecraft mc, Player player, BlockPos railPos) {
		if (mc.level == null) {
			return false;
		}
		if (mc.level.getBlockState(railPos).is(Blocks.RAIL)) {
			return true;
		}
		if (!mc.level.getBlockState(railPos).isAir()) {
			return false;
		}
		if (mc.level.getBlockState(railPos.below()).isAir()) {
			return false;
		}
		if (!InventoryUtil.selectItem(mc, Items.RAIL, true)) {
			return false;
		}
		if (rotate.get()) {
			RotationUtil.facePos(player, Vec3.atCenterOf(railPos));
		}
		InteractionUtil.useItemOnBlock(mc, railPos.below(), Direction.UP);
		return true;
	}

	private boolean placeCartIfNeeded(Minecraft mc, Player player, BlockPos railPos) {
		if (mc.level == null) {
			return false;
		}
		if (findTntCart(mc.level, railPos) != null) {
			return true;
		}
		if (!mc.level.getBlockState(railPos).is(Blocks.RAIL)) {
			return false;
		}
		if (!InventoryUtil.selectItem(mc, Items.TNT_MINECART, true)) {
			return false;
		}
		if (rotate.get()) {
			RotationUtil.facePos(player, Vec3.atCenterOf(railPos));
		}
		InteractionUtil.useItemOnBlock(mc, railPos, Direction.UP);
		return true;
	}

	private Entity findTntCart(Level level, BlockPos pos) {
		if (pos == null) {
			return null;
		}
		AABB area = new AABB(pos).inflate(1.5);
		for (Entity entity : level.getEntities(null, area)) {
			if (entity.getType() == EntityTypes.TNT_MINECART && entity.isAlive()) {
				return entity;
			}
		}
		return null;
	}
}
