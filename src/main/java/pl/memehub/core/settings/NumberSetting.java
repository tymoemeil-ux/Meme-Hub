package pl.memehub.core.settings;

/**
 * Ustawienie liczbowe (double) z zakresem i krokiem.
 */
public class NumberSetting extends Setting<Double> {
	private final double min;
	private final double max;
	private final double step;

	public NumberSetting(String name, String description, double defaultValue, double min, double max, double step) {
		super(name, description, clamp(defaultValue, min, max));
		this.min = min;
		this.max = max;
		this.step = step;
	}

	public double min() {
		return min;
	}

	public double max() {
		return max;
	}

	public double step() {
		return step;
	}

	public int intValue() {
		return value.intValue();
	}

	/** Wartosc jako long (do opoznien w ms). */
	public long longValue() {
		return value.longValue();
	}

	public void increment() {
		set(clamp(value + step, min, max));
	}

	public void decrement() {
		set(clamp(value - step, min, max));
	}

	@Override
	public void set(Double newValue) {
		this.value = clamp(newValue, min, max);
	}

	private static double clamp(double v, double min, double max) {
		return Math.max(min, Math.min(max, v));
	}

	@Override
	public String displayValue() {
		if (step >= 1.0) {
			return String.valueOf(value.intValue());
		}
		return String.format("%.2f", value);
	}
}
