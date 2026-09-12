package pl.memehub.core.settings;

/**
 * Ustawienie logiczne (wlaczone / wylaczone).
 */
public class BooleanSetting extends Setting<Boolean> {

	public BooleanSetting(String name, String description, boolean defaultValue) {
		super(name, description, defaultValue);
	}

	public boolean isEnabled() {
		return value;
	}

	@Override
	public String displayValue() {
		return value ? "ON" : "OFF";
	}

	/** Przelacza wartosc na przeciwna. */
	public void toggle() {
		this.value = !this.value;
	}
}
