package pl.memehub.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.ModuleManager;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.NumberSetting;
import pl.memehub.event.HudRenderEvent;
import pl.memehub.event.Subscribe;
import pl.memehub.modules.combat.KillAura;
import pl.memehub.util.EntityUtil;
import pl.memehub.util.RenderUtil;

import java.util.List;

/**
 * HUD moda - rysuje (2D, GuiGraphicsExtractor):
 * <ul>
 *   <li>ArrayList - liste aktywnych modulow w prawym gornym rogu,</li>
 *   <li>TargetHUD - pasek zdrowia wybranego celu KillAura,</li>
 *   <li>wskaznik gotowosci ataku (attack cooldown).</li>
 * </ul>
 *
 * <p>Rysowanie odbywa sie przez event bus: element HUD zarejestrowany w
 * {@code HudElementRegistry} (patrz MemeHubClient) wysyla {@link HudRenderEvent}.
 */
public final class HudModule extends Module {

	private final BooleanSetting arrayList = new BooleanSetting("ArrayList", "Lista aktywnych modulow", true);
	private final BooleanSetting targetHud = new BooleanSetting("TargetHUD", "Pasek zdrowia celu", true);
	private final BooleanSetting cooldownDisplay = new BooleanSetting("Cooldown", "Wskaznik attack cooldown", true);
	private final BooleanSetting fpsDisplay = new BooleanSetting("FPS", "Licznik klatek", true);
	private final BooleanSetting coordsDisplay = new BooleanSetting("Coords", "Wspolrzedne gracza", false);
	private final BooleanSetting shadows = new BooleanSetting("Shadows", "Cienie pod tekstem", true);
	private final NumberSetting scale = new NumberSetting("Scale", "Skala HUD (0.5-2.0)", 1.0, 0.5, 2.0, 0.1);

	public HudModule() {
		super("HUD", "Nakladka HUD: lista modulow, TargetHUD, cooldown ataku", Category.RENDER, true);
		addSetting(arrayList);
		addSetting(targetHud);
		addSetting(cooldownDisplay);
		addSetting(fpsDisplay);
		addSetting(coordsDisplay);
		addSetting(shadows);
		addSetting(scale);
	}

	@Override
	protected void onEnable() {
	}

	@Subscribe
	public void onHudRender(HudRenderEvent event) {
		if (!isEnabled() || event.graphics() == null) {
			return;
		}
		Minecraft mc = mc();
		if (mc.player == null || mc.getWindow() == null) {
			return;
		}
		float s = scale.get().floatValue();
		int width = mc.getWindow().getGuiScaledWidth();
		int height = mc.getWindow().getGuiScaledHeight();

		if (arrayList.get()) {
			renderArrayList(event.graphics(), mc.font, width, s);
		}
		if (targetHud.get()) {
			renderTargetHud(event.graphics(), mc.font, height, s);
		}
		if (cooldownDisplay.get()) {
			renderCooldown(event.graphics(), mc.font, width, height, s);
		}
		if (fpsDisplay.get()) {
			renderFps(event.graphics(), mc.font, s);
		}
		if (coordsDisplay.get() && mc.level != null) {
			renderCoords(event.graphics(), mc.font, width, height, s);
		}
	}

	// ------------------------------------------------------------------
	// FPS / Coords
	// ------------------------------------------------------------------

	private void renderFps(GuiGraphicsExtractor graphics, Font font, float scale) {
		int fps = mc().getFps();
		int color = fps >= 120 ? RenderUtil.rgb(80, 255, 80) : fps >= 60 ? RenderUtil.rgb(255, 220, 60) : RenderUtil.rgb(255, 80, 80);
		String text = fps + " FPS";
		if (shadows.get()) {
			RenderUtil.textShadow(graphics, font, text, 2, 12, color);
		} else {
			RenderUtil.text(graphics, font, text, 2, 12, color);
		}
	}

	private void renderCoords(GuiGraphicsExtractor graphics, Font font, int screenWidth, int screenHeight, float scale) {
		var player = player();
		if (player == null) {
			return;
		}
		String text = String.format("XYZ %.1f / %.1f / %.1f", player.getX(), player.getY(), player.getZ());
		String dim = player.level().dimension().identifier().getPath();
		String full = text + "  [" + dim + "]";
		int color = RenderUtil.rgb(210, 210, 210);
		if (shadows.get()) {
			RenderUtil.textShadow(graphics, font, full, 2, screenHeight - 10, color);
		} else {
			RenderUtil.text(graphics, font, full, 2, screenHeight - 10, color);
		}
	}

	// ------------------------------------------------------------------
	// ArrayList
	// ------------------------------------------------------------------

	private void renderArrayList(GuiGraphicsExtractor graphics, Font font, int screenWidth, float scale) {
		List<Module> active = ModuleManager.INSTANCE.getAll().stream().filter(Module::isEnabled).toList();
		if (active.isEmpty()) {
			return;
		}
		int y = 2;
		int x = screenWidth - 2;
		for (Module module : active) {
			String text = module.name();
			int color = module.category().color();
			int width = RenderUtil.width(font, text);
			if (shadows.get()) {
				RenderUtil.textShadow(graphics, font, text, x - width - 2, y, color);
			} else {
				RenderUtil.text(graphics, font, text, x - width - 2, y, color);
			}
			y += RenderUtil.textHeight(font);
		}
	}

	// ------------------------------------------------------------------
	// TargetHUD
	// ------------------------------------------------------------------

	private void renderTargetHud(GuiGraphicsExtractor graphics, Font font, int screenHeight, float scale) {
		KillAura killAura = ModuleManager.INSTANCE.get(KillAura.class);
		if (killAura == null || !killAura.isEnabled()) {
			return;
		}
		var player = player();
		if (player == null) {
			return;
		}
		LivingEntity target = EntityUtil.getNearestTarget(player, 6.0, true, true, false);
		if (target == null) {
			return;
		}
		int x = 4;
		int y = screenHeight / 2 - 40;

		String name = target.getName().getString();
		int width = Math.max(60, RenderUtil.width(font, name) + 8);
		int boxHeight = 26;

		RenderUtil.rect(graphics, x, y, width, boxHeight, RenderUtil.rgba(0, 0, 0, 120));
		RenderUtil.outline(graphics, x, y, width, boxHeight, RenderUtil.rgb(255, 255, 255));

		RenderUtil.textShadow(graphics, font, name, x + 4, y + 3, RenderUtil.rgb(255, 255, 255));

		// Pasek zdrowia.
		float health = Math.max(0.0F, target.getHealth());
		float maxHealth = Math.max(1.0F, target.getMaxHealth());
		float ratio = Math.min(1.0F, health / maxHealth);
		int barY = y + 15;
		int barWidth = width - 8;
		RenderUtil.rect(graphics, x + 4, barY, barWidth, 4, RenderUtil.rgba(60, 60, 60, 200));
		RenderUtil.rect(graphics, x + 4, barY, (int) (barWidth * ratio), 4, RenderUtil.rgb(80, 255, 80));
		RenderUtil.textShadow(graphics, font, String.format("%.1f / %.1f", health, maxHealth),
				x + 4, barY + 5, RenderUtil.rgb(230, 230, 230));
	}

	// ------------------------------------------------------------------
	// Cooldown ataku
	// ------------------------------------------------------------------

	private void renderCooldown(GuiGraphicsExtractor graphics, Font font, int screenWidth, int screenHeight, float scale) {
		var player = player();
		if (player == null) {
			return;
		}
		float strength = player.getAttackStrengthScale(0.0f);
		int barWidth = 40;
		int x = screenWidth / 2 - barWidth / 2;
		int y = screenHeight / 2 + 14;
		RenderUtil.rect(graphics, x, y, barWidth, 3, RenderUtil.rgba(0, 0, 0, 150));
		int filled = (int) (barWidth * Math.max(0.0F, Math.min(1.0F, strength)));
		int color = strength >= 0.9F ? RenderUtil.rgb(80, 255, 80) : RenderUtil.rgb(255, 200, 60);
		RenderUtil.rect(graphics, x, y, filled, 3, color);
		if (strength < 0.95F) {
			RenderUtil.textShadow(graphics, font, String.format("%.0f%%", strength * 100.0F),
					x + barWidth + 3, y - 2, RenderUtil.rgb(255, 255, 255));
		}
	}

}
