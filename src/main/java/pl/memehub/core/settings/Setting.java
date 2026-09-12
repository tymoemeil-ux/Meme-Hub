package pl.memehub.core.settings;

/**
 * Bazowa klasa ustawienia modulu. Kazde ustawienie ma nazwe, opis i wartosc.
 * Konkretne typy: {@link BooleanSetting}, {@link NumberSetting}, {@link EnumSetting}.
 */
public abstract class Setting<T> {
	protected final String name;
	protected final String description;
	protected final T defaultValue;
	protected T value;

	protected Setting(String name, String description, T defaultValue) {
		this.name = name;
		this.description = description;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public String name() {
		return name;
	}

	public String description() {
		return description;
	}

	public T get() {
		return value;
	}

	public void set(T value) {
		this.value = value;
	}

	/** Wartosc domyslna (z konstruktora). */
	public T defaultValue() {
		return defaultValue;
	}

	/** Przywraca wartosc domyslna. */
	public void reset() {
		this.value = defaultValue;
	}

	/** Nazwa wartosci do wyswietlenia w ClickGUI. */
	public abstract String displayValue();

	@Override
	public String toString() {
		return name + " = " + value;
	}
}
