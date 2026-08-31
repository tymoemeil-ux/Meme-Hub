package pl.memehub.modules.combat;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.NumberSetting;

/**
 * AutoTotem - natychmiastowe przekladanie Totemu Niesmiertelnosci do drugiej reki,
 * gdy poziom zdrowia spadnie ponizej progu (lub zawsze, gdy threshold = 20).
 */
public final class AutoTotem extends Module {

	private final NumberSetting healthThreshold = new NumberSetting("Health Threshold", "Punkt krytyczny (zycie + pochloniecie)", 20.0, 1.0, 20.0, 1.0);
	private final NumberSetting delay = new NumberSetting("Delay", "Co ile tickow sprawdzac", 2.0, 1.0, 20.0, 1.0);

	private int tickCounter = 0;

	public AutoTotem() {
		super("AutoTotem", "Automatyczne przekladanie Totemu do drugiej reki", Category.COMBAT);
		addSetting(healthThreshold);
		addSetting(delay);
	}

	@Override
	protected void onEnable() {
		tickCounter = 0;
	}

	@Override
	public void onTick() {
		var player = player();
		if (player == null) {
			return;
		}
		if (++tickCounter < delay.intValue()) {
			return;
		}
		tickCounter = 0;

		if (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
			return;
		}
		double health = player.getHealth() + player.getAbsorptionAmount();
		if (health > healthThreshold.get()) {
			return;
		}

		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.items.size(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.is(Items.TOTEM_OF_UNDYING)) {
				continue;
			}
			ItemStack oldOffhand = player.getOffhandItem().copy();
			inventory.setItem(40, stack);
			inventory.setItem(slot, oldOffhand);
			player.containerMenu.broadcastChanges();
			return;
		}
	}
}
