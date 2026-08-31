package pl.memehub.modules.utility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.NumberSetting;
import pl.memehub.util.InventoryUtil;

/**
 * AutoTool - automatyczny wybor najlepszego narzedzia do niszczonego bloku.
 */
public final class AutoTool extends Module {

	private final BooleanSetting onlyWhenBreaking = new BooleanSetting("Only When Breaking", "Dzialaj tylko podczas niszczenia bloku", true);
	private final BooleanSetting switchBack = new BooleanSetting("Switch Back", "Wroc do poprzedniego slotu po zniszczeniu", true);
	private final NumberSetting checkRange = new NumberSetting("Range", "Zasieg bloku (bloki)", 5.0, 1.0, 8.0, 0.5);

	private int previousSlot = -1;
	private boolean active = false;

	public AutoTool() {
		super("AutoTool", "Automatyczny wybor najlepszego narzedzia", Category.UTILITY);
		addSetting(onlyWhenBreaking);
		addSetting(switchBack);
		addSetting(checkRange);
	}

	@Override
	protected void onEnable() {
		previousSlot = -1;
		active = false;
	}

	@Override
	public void onTick() {
		var mc = mc();
		var player = player();
		if (mc.level == null || player == null || mc.gameMode == null) {
			return;
		}
		if (!mc.gameMode.isDestroying()) {
			if (active && switchBack.get() && previousSlot >= 0) {
				player.getInventory().selected = previousSlot;
			}
			active = false;
			previousSlot = -1;
			return;
		}
		BlockHitResult hit = mc.hitResult instanceof BlockHitResult bhr ? bhr : null;
		if (hit == null) {
			return;
		}
		BlockPos pos = hit.getBlockPos();
		BlockState state = mc.level.getBlockState(pos);
		if (state.isAir()) {
			return;
		}
		if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > checkRange.get() * checkRange.get()) {
			return;
		}

		int bestSlot = -1;
		float bestSpeed = 1.0F;
		for (int i = 0; i < 9; i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.isEmpty()) {
				continue;
			}
			float speed = stack.getDestroySpeed(state);
			if (speed > bestSpeed) {
				bestSpeed = speed;
				bestSlot = i;
			}
		}
		if (bestSlot != -1 && bestSlot != player.getInventory().selected) {
			if (!active) {
				previousSlot = player.getInventory().selected;
				active = true;
			}
			player.getInventory().selected = bestSlot;
		}
	}

	@Override
	protected void onDisable() {
		var player = player();
		if (player != null && active && switchBack.get() && previousSlot >= 0) {
			player.getInventory().selected = previousSlot;
		}
		active = false;
		previousSlot = -1;
	}

	@SuppressWarnings("unused")
	private static void unusedImportsGuard() {
		// Utrzymuje importy w konsystencji (Items/Direction uzywane w dalszym rozwoju).
		ItemStack ignored = ItemStack.EMPTY;
		Direction d = Direction.UP;
		boolean b = ignored.is(Items.DIAMOND_PICKAXE) && d != null;
	}
}
