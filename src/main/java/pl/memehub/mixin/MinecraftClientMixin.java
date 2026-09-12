package pl.memehub.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.memehub.event.ClientTickEvent;
import pl.memehub.event.EventBus;

/**
 * Mieszanie w glowny tick klienta - zrodlo zdarzen ticku dla event bussa.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {

	@Inject(method = "tick", at = @At("HEAD"))
	private void memehub$onTickHead(CallbackInfo ci) {
		EventBus.INSTANCE.post(new ClientTickEvent.Pre());
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void memehub$onTickTail(CallbackInfo ci) {
		EventBus.INSTANCE.post(new ClientTickEvent.Post());
	}
}
