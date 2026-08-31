package pl.memehub.util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Pomocnik rysowania 2D na HUD / w ClickGUI przy uzyciu GuiGraphicsExtractor (26.2).
 *
 * <p>Uwaga: w 26.1 klasa {@code GuiGraphics} zostala przemianowana na
 * {@code GuiGraphicsExtractor}, a metody rysujace przyjely nowe nazwy:
 * {@code drawString -> text}, {@code drawCenteredString -> centeredText},
 * {@code renderOutline -> outline}, {@code hLine -> horizontalLine}.
 * Wszystkie kolory musza byc w formacie ARGB.
 */
public final class RenderUtil {
	private RenderUtil() {
	}

	public static int rgba(int r, int g, int b, int a) {
		return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
	}

	public static int rgb(int r, int g, int b) {
		return 0xFF000000 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
	}

	public static int withAlpha(int argb, int alpha) {
		return (argb & 0x00FFFFFF) | (alpha & 0xFF) << 24;
	}

	public static void rect(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
		graphics.fill(x, y, x + width, y + height, color);
	}

	public static void outline(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
		graphics.outline(x, y, width, height, color);
	}

	public static void gradient(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int top, int bottom) {
		graphics.fillGradient(x, y, x + width, y + height, top, bottom);
	}

	public static void text(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
		graphics.text(font, text, x, y, color, false);
	}

	public static void textShadow(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
		graphics.text(font, text, x, y, color, true);
	}

	public static void centeredText(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
		graphics.centeredText(font, text, x, y, color);
	}

	public static int width(Font font, String text) {
		return font.width(text);
	}

	public static int textHeight(Font font) {
		return font.lineHeight;
	}

	/** Tekst wyrownany do prawej (koniec w punkcie x). */
	public static void rightText(GuiGraphicsExtractor graphics, Font font, String text, int rightX, int y, int color) {
		text(graphics, font, text, rightX - width(font, text), y, color);
	}
}
