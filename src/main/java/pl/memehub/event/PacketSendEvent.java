package pl.memehub.event;

import net.minecraft.network.protocol.Packet;

/**
 * Zdarzenie wysylania pakietu przez klienta (mixin {@code Connection#send}).
 * Anulowanie zdarzenia blokuje wyslanie pakietu - to podstawa "nadpisywania pakietow".
 */
public final class PacketSendEvent extends Event implements Cancellable {
	private final Packet<?> packet;
	private boolean cancelled = false;

	public PacketSendEvent(Packet<?> packet) {
		this.packet = packet;
	}

	public Packet<?> packet() {
		return packet;
	}

	@Override
	public void cancel() {
		this.cancelled = true;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}
}
