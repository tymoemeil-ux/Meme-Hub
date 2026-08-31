package pl.memehub.modules.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.EnumSetting;
import pl.memehub.core.settings.NumberSetting;
import pl.memehub.util.InteractionUtil;
import pl.memehub.util.InventoryUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Surround - automatyczne otaczanie gracza obsydianem (4 lub 8 blokow wokol).
 *
 * <p>Automatyzacja przydatna w testach PvP: buduje sciane wokol gracza,
 * aby utrudnic trafienie krysztalami / atakami. Kazdy blok stawiany jest
 * kliknieciem w blok pod spodem (jak zwykly gracz).
 */
public final class Surround extends Module {

	public enum Mode {
		CROSS, FULL
	}

	private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", "Ksztalt sciany", Mode.CROSS);
	private final NumberSetting placeDelay = new NumberSetting("Place Delay", "Opoznienie miedzy blokami (ms)", 50, 0, 1000, 10);
	private final BooleanSetting rotate = new BooleanSetting("Rotate", "Obracaj gracza w strone stawianego bloku", true);
	private final BooleanSetting retry = new BooleanSetting("Retry", "Powtarzaj, az wszystkie bloki beda postawione", true);

	private int cursor = 0;
	private long lastPlace = 0;

	public Surround() {
		super("Surround", "Otacza gracza sciana z obsydianu", Category.COMBAT);
		addSetting(mode);
		addSetting(placeDelay);
		addSetting(rotate);
		addSetting(retry);
	}

	@Override
	protected void onEnable() {
		cursor = 0;
		lastPlace = 0;
	}

	@Override
	public void onTick() {
		var mc = mc();
		var player = player();
		if (mc.level == null || player == null) {
			return;
		}
		if (!InventoryUtil.hasItem(player.getInventory(), Items.OBSIDIAN)) {
			return;
		}

		List<BlockPos> ring = ringPositions(player.blockPosition(), mode.get() == Mode.FULL);
		if (ring.isEmpty()) {
			return;
		}
		cursor %= ring.size();
		BlockPos pos = ring.get(cursor);

		if (!isValidPlace(mc.level, pos)) {
			cursor++;
			return;
		}
		long now = Util.getMillis();
		if (now - lastPlace < placeDelay.longValue()) {
			return;
		}
		if (!InventoryUtil.selectItem(mc, Items.OBSIDIAN, true)) {
			return;
		}
		if (rotate.get()) {
			pl.memehub.util.RotationUtil.facePos(player, pos.getCenter());
		}
		InteractionUtil.placeBlockAt(mc, pos);
		lastPlace = now;
		cursor++;

		if (retry.get() && isComplete(mc.level, ring)) {
			cursor = 0;
		}
	}

	private List<BlockPos> ringPositions(BlockPos base, boolean full) {
		List<BlockPos> result = new ArrayList<>();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				if (!full && dx != 0 && dz != 0) {
					continue; // CROSS: tylko N/S/E/W
				}
				result.add(base.offset(dx, 0, dz));
			}
		}
		return result;
	}

	private boolean isValidPlace(Level level, BlockPos pos) {
		if (!level.getBlockState(pos).isAir()) {
			return false;
		}
		return !level.getBlockState(pos.below()).isAir();
	}

	private boolean isComplete(Level level, List<BlockPos> ring) {
		for (BlockPos pos : ring) {
			if (level.getBlockState(pos).isAir()) {
				return false;
			}
		}
		return true;
	}
}
