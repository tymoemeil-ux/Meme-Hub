package pl.memehub.event;

/**
 * Znacznik zdarzenia, ktore mozna anulowac.
 */
public interface Cancellable {
	void cancel();

	boolean isCancelled();
}
