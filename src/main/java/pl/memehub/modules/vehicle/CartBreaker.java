package pl.memehub.modules.vehicle;

import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.NumberSetting;
import pl.memehub.util.RotationUtil;

/**
 * Cart Breaker - niszczenie wszystkich wozkow (minecart) wokol gracza.
 *
 * <p>Rozpoznaje wozki po opisie typu encji zawierajacym "minecart" - dzieki
 * temu dziala niezaleznie od dokladnych nazw klas wozkow w 26.2
 * (np. zwykle wozki, wozki z TNT, wozki z skrzynia, wozki ze szkieletem).
 */
public final class CartBreaker extends Module {

	private final NumberSetting range = new NumberSetting("Range", "Zasieg niszczenia (bloki)", 4.0, 1.0, 8.0, 0.5);
	private final NumberSetting delay = new NumberSetting("Delay", "Opoznienie miedzy atakami (ms)", 150, 0, 2000, 10);
	private final BooleanSetting rotate = new BooleanSetting("Rotate", "Obracaj gracza w strone wozka", true);
	private final BooleanSetting stopAtNone = new BooleanSetting("Idle When None", "Nic nie rob, gdy brak wozkow", true);

	private long lastAttack = 0;

	public CartBreaker() {
		super("Cart Breaker", "Niszczy wszystkie wozki wokol gracza", Category.COMBAT);
		addSetting(range);
		addSetting(delay);
		addSetting(rotate);
		addSetting(stopAtNone);
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
		Entity cart = findNearestCart(mc.level, player, range.get());
		if (cart == null) {
			return;
		}
		long now = Util.getMillis();
		if (now - lastAttack < delay.longValue()) {
			return;
		}
		if (rotate.get()) {
			RotationUtil.facePos(player, cart.position());
		}
		mc.gameMode.attack(cart);
		player.swing(InteractionHand.MAIN_HAND);
		lastAttack = now;
	}

	private Entity findNearestCart(Level level, Player player, double range) {
		AABB area = player.getBoundingBox().inflate(range);
		Entity best = null;
		double bestDist = Double.MAX_VALUE;
		for (Entity entity : level.getEntities(player, area)) {
			if (!isMinecart(entity)) {
				continue;
			}
			double dist = player.distanceToSqr(entity);
			if (dist < bestDist) {
				bestDist = dist;
				best = entity;
			}
		}
		return best;
	}

	/** Czy encja jest wozkiem - sprawdza po opisie typu encji. */
	private boolean isMinecart(Entity entity) {
		return entity.isAlive() && entity.getType().getDescriptionId().contains("minecart");
	}
}
