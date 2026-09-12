package pl.memehub.modules.utility;

import net.minecraft.client.Minecraft;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.ModuleManager;
import pl.memehub.gui.ClickGuiScreen;
import pl.memehub.util.ChatUtil;

/**
 * Panic - wylacza wszystkie wlaczone moduly jednym skrotem klawiszowym
 * i zamyka ClickGUI. W 26.2 ekrany sa zarzadzane przez {@code Minecraft#gui}.
 */
public final class Panic extends Module {

	public Panic() {
		super("Panic", "Wylacza wszystkie moduly jednym klawiszem", Category.UTILITY);
	}

	@Override
	protected void onEnable() {
		ModuleManager.INSTANCE.disableAllExcept(this);
		Minecraft mc = mc();
		if (mc.gui != null && mc.gui.screen() instanceof ClickGuiScreen) {
			mc.gui.setScreen(null);
		}
		ChatUtil.warn("Wylaczono wszystkie moduly.");
		this.setEnabled(false);
	}
}
