package pl.memehub.modules.combat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import pl.memehub.core.Category;
import pl.memehub.core.Module;

/**
 * AutoSword - przed atakiem automatycznie wybiera najlepszy miecz w hotbarze.
 *
 * <p>Kolejnosc jakosci: Netheryt &gt; Diament &gt; Zelazo &gt; Kamien &gt; Zloto &gt; Drewno.
 * Dziala przez hook {@code onPreAttack} (mixin MultiPlayerGameMode#attack).
 */
public final class AutoSword extends Module {

	private static final Item[] SWORD_ORDER = {
			Items.NETHERITE_SWORD, Items.DIAMOND_SWORD, Items.IRON_SWORD,
			Items.STONE_SWORD, Items.GOLDEN_SWORD, Items.WOODEN_SWORD
	};

	public AutoSword() {
		super("AutoSword", "Automatyczny wybor najlepszego miecza przed atakiem", Category.COMBAT);
	}

	@Override
	public boolean onPreAttack(LocalPlayer player, Entity target) {
		if (player == null) {
			return false;
		}
		Inventory inventory = player.getInventory();
		int best = -1;
		for (int i = 0; i < SWORD_ORDER.length; i++) {
			int slot = pl.memehub.util.InventoryUtil.findHotbarSlot(inventory, SWORD_ORDER[i]);
			if (slot != -1) {
				best = slot;
				break;
			}
		}
		if (best != -1 && best != inventory.selected) {
			inventory.selected = best;
		}
		return false; // nie przejmujemy ataku
	}
}
