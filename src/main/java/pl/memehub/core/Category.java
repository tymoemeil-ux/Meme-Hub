package pl.memehub.core;

/**
 * Kategorie modulow uzywane w ClickGUI oraz w HUD (ArrayList).
 */
public enum Category {
	COMBAT("Combat", 0xFFFF5555),
	MOVEMENT("Movement", 0xFF55FF55),
	RENDER("Render", 0xFFFFAA55),
	UTILITY("Utility", 0xFFAAAAAA);

	private final String displayName;
	private final int color;

	Category(String displayName, int color) {
		this.displayName = displayName;
		this.color = color;
	}

	public String displayName() {
		return displayName;
	}

	/** Kolor ARGB uzywany np. w liscie aktywnych modulow (ArrayList). */
	public int color() {
		return color;
	}
}
