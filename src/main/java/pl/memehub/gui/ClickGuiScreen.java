package pl.memehub.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import pl.memehub.config.Config;
import pl.memehub.core.Category;
import pl.memehub.core.Module;
import pl.memehub.core.ModuleManager;
import pl.memehub.core.settings.BooleanSetting;
import pl.memehub.core.settings.EnumSetting;
import pl.memehub.core.settings.NumberSetting;
import pl.memehub.core.settings.Setting;
import pl.memehub.util.RenderUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClickGUI - kategoryzowany panel zarzadzania modulami (Minecraft 26.x, Screen).
 *
 * <p>Funkcje:
 * <ul>
 *   <li>paniele per kategoria (Combat / Movement / Render / Utility), mozna je
 *       przeciagac za naglowek; LPM na naglowek = przeciaganie, PPM = zwijanie,</li>
 *   <li>scroll myszy przewija dlugie listy modulow,</li>
 *   <li>LPM na module - wlacz / wylacz, PPM - rozwin ustawienia,</li>
 *   <li>ustawienia: boolean - klik, number - LPM + / PPM -, enum - cykl LPM,</li>
 *   <li>wiersz KEY - klik i wcisniecie klawisza ustawia keybind (ESC kasuje),</li>
 *   <li>tooltip z opisem modulu / ustawienia po najechaniu myszka.</li>
 * </ul>
 *
 * <p>Zgodnie z cyklem zycia Screen w 26.1+: renderowanie przez
 * {@code extractBackground} + {@code extractRenderState} z {@code GuiGraphicsExtractor}.
 */
public final class ClickGuiScreen extends Screen {

	private final int panelWidth;
	private final int rowHeight = 12;
	private final int headerHeight = 14;
	private final int maxRows = 14;

	private final Map<Category, Panel> panels = new HashMap<>();
	private Module bindingModule = null;

	/** Opis podpowiedzi narysowany w ostatniej klatce. */
	private String hoverTooltip = null;

	private static final class Panel {
		int x;
		int y;
		int scroll;
		boolean collapsed;
		boolean dragging;
		double dragOffsetX;
		double dragOffsetY;
		final Map<Module, Boolean> expanded = new HashMap<>();

		Panel(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	/** Pojedynczy wiersz panelu (modul / ustawienie / wiersz klawisza). */
	private record Row(int y, Module module, Setting<?> setting, boolean keyRow, boolean header) {
	}

	public ClickGuiScreen(Minecraft minecraft, int panelWidth) {
		super(minecraft, minecraft.font, Component.literal("Meme Hub"));
		this.panelWidth = panelWidth;
	}

	@Override
	public void init() {
		int x = 6;
		int y = 6;
		for (Category category : Category.values()) {
			panels.put(category, new Panel(x, y));
			x += panelWidth + 6;
		}
	}

	@Override
	public void tick() {
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		hoverTooltip = null;
		for (Category category : Category.values()) {
			renderPanel(graphics, category, mouseX, mouseY);
		}
		renderFooter(graphics);
		if (hoverTooltip != null) {
			renderTooltip(graphics, hoverTooltip, mouseX, mouseY);
		}
	}

	// ------------------------------------------------------------------
	// Uklad wierszy (wspolne dla renderowania i klikow)
	// ------------------------------------------------------------------

	private List<Row> buildRows(Category category, Panel panel) {
		List<Row> rows = new ArrayList<>();
		int y = panel.y + headerHeight + 1;
		for (Module module : ModuleManager.INSTANCE.getByCategory(category)) {
			rows.add(new Row(y - panel.scroll * rowHeight, module, null, false, false));
			y += rowHeight;
			if (Boolean.TRUE.equals(panel.expanded.get(module))) {
				for (Setting<?> setting : module.settings()) {
					rows.add(new Row(y - panel.scroll * rowHeight, module, setting, false, false));
					y += rowHeight;
				}
				rows.add(new Row(y - panel.scroll * rowHeight, module, null, true, false));
				y += rowHeight;
			}
		}
		return rows;
	}

	private int contentRows(Category category, Panel panel) {
		int rows = ModuleManager.INSTANCE.getByCategory(category).size();
		for (Module module : ModuleManager.INSTANCE.getByCategory(category)) {
			if (Boolean.TRUE.equals(panel.expanded.get(module))) {
				rows += module.settings().size() + 1;
			}
		}
		return rows;
	}

	private int panelHeight(Category category, Panel panel) {
		int rows = panel.collapsed ? 0 : Math.min(contentRows(category, panel), maxRows);
		return headerHeight + rows * rowHeight + 2;
	}

	// ------------------------------------------------------------------
	// Rysowanie
	// ------------------------------------------------------------------

	private void renderPanel(GuiGraphicsExtractor graphics, Category category, int mouseX, int mouseY) {
		Panel panel = panels.get(category);
		if (panel == null) {
			return;
		}
		int height = panelHeight(category, panel);

		// Tlo panelu.
		RenderUtil.rect(graphics, panel.x, panel.y, panelWidth, height, RenderUtil.rgba(26, 26, 30, 215));
		RenderUtil.outline(graphics, panel.x, panel.y, panelWidth, height, RenderUtil.rgb(90, 90, 100));

		// Naglowek kategorii - gradient w kolorze kategorii.
		RenderUtil.gradient(graphics, panel.x, panel.y, panelWidth, headerHeight,
				RenderUtil.withAlpha(category.color(), 235), RenderUtil.withAlpha(category.color(), 120));
		RenderUtil.centeredText(graphics, this.font, category.displayName() + (panel.collapsed ? "  [+] " : "  [-] "),
				panel.x + panelWidth / 2, panel.y + (headerHeight - this.font.lineHeight) / 2 + 1,
				RenderUtil.rgb(255, 255, 255));

		if (panel.collapsed) {
			return;
		}

		// Wiersze modulow / ustawien.
		for (Row row : buildRows(category, panel)) {
			boolean visible = row.y >= panel.y + headerHeight && row.y < panel.y + height - 1;
			if (!visible) {
				continue;
			}
			boolean hovered = mouseX >= panel.x && mouseX <= panel.x + panelWidth
					&& mouseY >= row.y && mouseY <= row.y + rowHeight;
			if (row.keyRow) {
				int bg = hovered ? RenderUtil.rgba(80, 60, 50, 200) : RenderUtil.rgba(48, 40, 36, 170);
				RenderUtil.rect(graphics, panel.x, row.y, panelWidth, rowHeight, bg);
				String text = bindingModule == row.module ? "PRESS KEY..." : "KEY: " + row.module.keyName();
				RenderUtil.textShadow(graphics, this.font, text, panel.x + 5, row.y + 2, RenderUtil.rgb(255, 210, 140));
				if (hovered) {
					hoverTooltip = "Kliknij i wcisnij klawisz, aby ustawic keybind. ESC - usun.";
				}
				continue;
			}
			if (row.setting != null) {
				int bg = hovered ? RenderUtil.rgba(60, 62, 72, 200) : RenderUtil.rgba(36, 37, 44, 170);
				RenderUtil.rect(graphics, panel.x, row.y, panelWidth, rowHeight, bg);
				String text = row.setting.name() + ": " + row.setting.displayValue();
				RenderUtil.textShadow(graphics, this.font, text, panel.x + 5, row.y + 2, RenderUtil.rgb(205, 208, 230));
				if (hovered) {
					hoverTooltip = row.setting.description();
				}
				continue;
			}

			// Wiersz modulu.
			Module module = row.module;
			int bg = hovered ? RenderUtil.rgba(70, 72, 82, 200) : RenderUtil.rgba(44, 45, 54, 170);
			RenderUtil.rect(graphics, panel.x, row.y, panelWidth, rowHeight, bg);
			// Pasek stanu z lewej.
			RenderUtil.rect(graphics, panel.x, row.y, 2, rowHeight,
					module.isEnabled() ? RenderUtil.rgb(80, 255, 80) : RenderUtil.rgb(120, 120, 130));
			String label = module.name();
			if (module.key != 0) {
				label += " [" + module.keyName() + "]";
			}
			RenderUtil.textShadow(graphics, this.font, label, panel.x + 5, row.y + 2,
					module.isEnabled() ? RenderUtil.rgb(90, 255, 120) : RenderUtil.rgb(210, 210, 220));
			if (Boolean.TRUE.equals(panel.expanded.get(module))) {
				RenderUtil.textShadow(graphics, this.font, "-", panel.x + panelWidth - 8, row.y + 2,
						RenderUtil.rgb(255, 255, 255));
			}
			if (hovered) {
				hoverTooltip = module.description();
			}
		}

		// Pasek scrolla (jesli lista sie przecina).
		int content = contentRows(category, panel);
		if (content > maxRows) {
			int trackY = panel.y + headerHeight + 1;
			int trackH = height - headerHeight - 2;
			float ratio = (float) maxRows / content;
			int thumbH = Math.max(8, (int) (trackH * ratio));
			int thumbY = trackY + (int) ((trackH - thumbH) * ((float) panel.scroll / (content - maxRows)));
			RenderUtil.rect(graphics, panel.x + panelWidth - 2, thumbY, 2, thumbH, RenderUtil.rgba(255, 255, 255, 120));
		}
	}

	private void renderFooter(GuiGraphicsExtractor graphics) {
		int y = this.height - 14;
		RenderUtil.rect(graphics, 0, y, this.width, 14, RenderUtil.rgba(0, 0, 0, 160));
		RenderUtil.textShadow(graphics, this.font, "LPM: wlacz/wylacz  |  PPM: ustawienia  |  Kolo: przewijanie  |  ESC: zamknij",
				4, y + 3, RenderUtil.rgb(190, 190, 200));
	}

	private void renderTooltip(GuiGraphicsExtractor graphics, String text, int mouseX, int mouseY) {
		int maxWidth = 200;
		List<String> lines = wrapText(text, maxWidth);
		int boxWidth = 8;
		for (String line : lines) {
			boxWidth = Math.max(boxWidth, this.font.width(line) + 8);
		}
		int boxHeight = lines.size() * this.font.lineHeight + 6;
		int x = mouseX + 10;
		int y = mouseY + 8;
		if (x + boxWidth > this.width) {
			x = mouseX - boxWidth - 10;
		}
		if (y + boxHeight > this.height) {
			y = mouseY - boxHeight - 8;
		}
		RenderUtil.rect(graphics, x, y, boxWidth, boxHeight, RenderUtil.rgba(15, 15, 20, 230));
		RenderUtil.outline(graphics, x, y, boxWidth, boxHeight, RenderUtil.rgb(140, 140, 160));
		int ty = y + 3;
		for (String line : lines) {
			RenderUtil.textShadow(graphics, this.font, line, x + 4, ty, RenderUtil.rgb(235, 235, 240));
			ty += this.font.lineHeight;
		}
	}

	/** Proste zawijanie tekstu po spacjach do podanej szerokosci (bez FormattedCharSequence). */
	private List<String> wrapText(String text, int maxWidth) {
		List<String> lines = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (String word : text.split(" ")) {
			String candidate = current.isEmpty() ? word : current + " " + word;
			if (this.font.width(candidate) > maxWidth && !current.isEmpty()) {
				lines.add(current.toString());
				current = new StringBuilder(word);
			} else {
				current = new StringBuilder(candidate);
			}
		}
		if (!current.isEmpty()) {
			lines.add(current.toString());
		}
		return lines;
	}

	// ------------------------------------------------------------------
	// Wejscie
	// ------------------------------------------------------------------

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		for (Category category : Category.values()) {
			Panel panel = panels.get(category);
			if (panel == null) {
				continue;
			}
			boolean inHeader = mouseX >= panel.x && mouseX <= panel.x + panelWidth
					&& mouseY >= panel.y && mouseY <= panel.y + headerHeight;
			if (inHeader) {
				if (button == 0) {
					panel.dragging = true;
					panel.dragOffsetX = mouseX - panel.x;
					panel.dragOffsetY = mouseY - panel.y;
				} else if (button == 1) {
					panel.collapsed = !panel.collapsed;
				}
				return true;
			}
			if (handleRowClick(category, panel, mouseX, mouseY, button)) {
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private boolean handleRowClick(Category category, Panel panel, double mouseX, double mouseY, int button) {
		if (panel.collapsed) {
			return false;
		}
		for (Row row : buildRows(category, panel)) {
			if (mouseX >= panel.x && mouseX <= panel.x + panelWidth
					&& mouseY >= row.y && mouseY <= row.y + rowHeight) {
				if (row.keyRow) {
					bindingModule = row.module;
					return true;
				}
				if (row.setting != null) {
					applySettingClick(row.setting, button);
					Config.INSTANCE.save();
					return true;
				}
				if (button == 0) {
					row.module.toggle();
					Config.INSTANCE.save();
				} else if (button == 1) {
					panel.expanded.merge(row.module, Boolean.TRUE, (a, b) -> !a);
				}
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void applySettingClick(Setting<?> setting, int button) {
		if (setting instanceof BooleanSetting bool) {
			bool.toggle();
		} else if (setting instanceof NumberSetting number) {
			if (button == 0) {
				number.increment();
			} else if (button == 1) {
				number.decrement();
			}
		} else if (setting instanceof EnumSetting enumSetting) {
			enumSetting.cycle();
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		for (Category category : Category.values()) {
			Panel panel = panels.get(category);
			if (panel == null) {
				continue;
			}
			boolean inPanel = mouseX >= panel.x && mouseX <= panel.x + panelWidth
					&& mouseY >= panel.y && mouseY <= panel.y + panelHeight(category, panel);
			if (inPanel) {
				int content = contentRows(category, panel);
				int maxScroll = Math.max(0, content - maxRows);
				panel.scroll = Math.max(0, Math.min(maxScroll, panel.scroll - (int) verticalAmount));
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		for (Panel panel : panels.values()) {
			panel.dragging = false;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		for (Panel panel : panels.values()) {
			if (panel.dragging) {
				panel.x = (int) Math.max(0, mouseX - panel.dragOffsetX);
				panel.y = (int) Math.max(0, mouseY - panel.dragOffsetY);
				return true;
			}
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (bindingModule != null) {
			if (keyCode == InputConstants.KEY_ESCAPE) {
				bindingModule.key = 0;
			} else {
				bindingModule.key = keyCode;
			}
			bindingModule = null;
			Config.INSTANCE.save();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		Config.INSTANCE.save();
		super.onClose();
	}

	@Override
	public void removed() {
		Config.INSTANCE.save();
		super.removed();
	}
}
