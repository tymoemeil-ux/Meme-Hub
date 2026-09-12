package pl.memehub.mixin;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.memehub.event.EventBus;
import pl.memehub.event.PacketSendEvent;

/**
 * Mieszanie w {@code Connection#send} - przechwytywanie pakietow wychodzacych.
 *
 * <p>Anulowanie {@link PacketSendEvent} blokuje wyslanie pakietu, co pozwala
 * modulom podmieniac / blokowac ruch sieciowy klienta.
 */
@Mixin(Connection.class)
public abstract class ConnectionMixin {

	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
	private void memehub$onSend(Packet<?> packet, CallbackInfo ci) {
		PacketSendEvent event = EventBus.INSTANCE.post(new PacketSendEvent(packet));
		if (event.isCancelled()) {
			ci.cancel();
		}
	}
}
