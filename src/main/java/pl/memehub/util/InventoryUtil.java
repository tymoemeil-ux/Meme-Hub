package pl.memehub.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Pomocnik operacji na ekwipunku: szukanie przedmiotow, przechwytywanie slotow.
 */
public final class InventoryUtil {
	private InventoryUtil() {
	}

	/** Szuka przedmiotu w hotbarze (sloty 0-8). Zwraca indeks lub -1. */
	public static int findHotbarSlot(Inventory inventory, Item item) {
		for (int i = 0; i < 9; i++) {
			if (inventory.getItem(i).is(item)) {
				return i;
			}
		}
		return -1;
	}

	/** Szuka przedmiotu w calym ekwipunku (items: hotbar + main inventory). Zwraca indeks lub -1. */
	public static int findSlot(Inventory inventory, Item item) {
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).is(item)) {
				return i;
			}
		}
		return -1;
	}

	public static boolean hasItem(Inventory inventory, Item item) {
		return findSlot(inventory, item) != -1;
	}

	/**
	 * Wybiera przedmiot w hotbarze. Gdy {@code searchInventory} jest true i przedmiotu
	 * nie ma w hotbarze, probuje przeniesc go z reszty ekwipunku do aktualnego slotu.
	 *
	 * @return true, gdy przedmiot jest wybrany (lub juz w rece).
	 */
	public static boolean selectItem(Minecraft mc, Item item, boolean searchInventory) {
		if (mc.player == null) {
			return false;
		}
		Inventory inventory = mc.player.getInventory();

		if (inventory.getItem(inventory.getSelectedSlot()).is(item)) {
			return true;
		}

		int hotbar = findHotbarSlot(inventory, item);
		if (hotbar != -1) {
			inventory.setSelectedSlot(hotbar);
			return true;
		}

		if (!searchInventory) {
			return false;
		}

		int slot = findSlot(inventory, item);
		if (slot == -1 || slot < 9) {
			return false;
		}

		// Zamiana przedmiotu z ekwipunku z aktualnie wybranym.
		ItemStack current = inventory.getItem(inventory.getSelectedSlot());
		ItemStack wanted = inventory.getItem(slot);
		inventory.setItem(inventory.getSelectedSlot(), wanted);
		inventory.setItem(slot, current);
		mc.player.containerMenu.broadcastChanges();
		return true;
	}

	/** Czy gracz trzyma przedmiot w rece glownej lub drugiej rece. */
	public static boolean isHolding(Player player, Item item) {
		return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
	}

	/** Indeks wolnego slotu (nie-czystego) w items lub -1. */
	public static int findEmptySlot(Inventory inventory) {
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).isEmpty()) {
				return i;
			}
		}
		return -1;
	}

	/** Czy w drugiej rece jest przedmiot. */
	public static boolean offhandIs(Player player, Item item) {
		return player.getOffhandItem().is(item);
	}
}
