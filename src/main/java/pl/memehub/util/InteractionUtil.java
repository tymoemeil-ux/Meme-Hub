package pl.memehub.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Pomocnik interakcji swiata: uzywanie przedmiotu na bloku, stawianie blokow.
 */
public final class InteractionUtil {
	private InteractionUtil() {
	}

	/** Uzywa aktualnie trzymanego przedmiotu na scianie {@code pos} od strony {@code direction}. */
	public static InteractionResult useItemOnBlock(Minecraft mc, BlockPos pos, Direction direction) {
		if (mc.player == null || mc.gameMode == null) {
			return InteractionResult.PASS;
		}
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), direction, pos, false);
		InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
		mc.player.swing(InteractionHand.MAIN_HAND);
		return result;
	}

	/**
	 * Stawia przedmiot (blok) NA pozycji {@code pos} - klika w blok pod spodem
	 * od gory, tak jak zwykly gracz.
	 */
	public static InteractionResult placeBlockAt(Minecraft mc, BlockPos pos) {
		return useItemOnBlock(mc, pos.below(), Direction.UP);
	}

	/** Uzywa przedmiotu (np. rzuca Wind Charge / je jedzenie) w rece glownej. */
	public static InteractionResult useItemMainHand(Minecraft mc) {
		if (mc.player == null || mc.gameMode == null) {
			return InteractionResult.PASS;
		}
		InteractionResult result = mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
		mc.player.swing(InteractionHand.MAIN_HAND);
		return result;
	}

	/**
	 * Wybiera przedmiot i stawia go na pozycji {@code pos}.
	 *
	 * @return true, gdy akcja zostala wykonana.
	 */
	public static boolean selectAndPlace(Minecraft mc, Item item, BlockPos pos, boolean searchInventory) {
		if (mc.player == null) {
			return false;
		}
		if (!InventoryUtil.selectItem(mc, item, searchInventory)) {
			return false;
		}
		placeBlockAt(mc, pos);
		return true;
	}
}
