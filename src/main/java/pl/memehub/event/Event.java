package pl.memehub.event;

/**
 * Bazowa klasa zdarzenia event bussa.
 * Zdarzenia anulowalne (np. {@link PacketSendEvent}, {@link EntityMoveEvent})
 * implementuja {@link Cancellable} - po anulowaniu mixin nie wykona oryginalnej operacji.
 */
public abstract class Event {
}
