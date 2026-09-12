package pl.memehub.core.settings;

/**
 * Ustawienie wyliczeniowe (tryb / opcja). Klikniecie w ClickGUI cykluje wartosci.
 */
public class EnumSetting<E extends Enum<E>> extends Setting<E> {
	private final Class<E> enumClass;

	public EnumSetting(String name, String description, E defaultValue) {
		super(name, description, defaultValue);
		this.enumClass = defaultValue.getDeclaringClass();
	}

	public Class<E> enumClass() {
		return enumClass;
	}

	/** Ustawia wartosc po nazwie (uzywane przy wczytywaniu konfiguracji). */
	public void setByName(String name) {
		try {
			this.value = Enum.valueOf(enumClass, name);
		} catch (IllegalArgumentException ignored) {
			// nieznana nazwa - zostawiamy domyslna wartosc
		}
	}

	/** Przelacza na kolejna wartosc wg kolejnosci deklaracji enuma. */
	public void cycle() {
		E[] values = enumClass.getEnumConstants();
		int next = (value.ordinal() + 1) % values.length;
		this.value = values[next];
	}

	@Override
	public String displayValue() {
		return value.name();
	}
}
