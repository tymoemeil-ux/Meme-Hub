package pl.memehub.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.memehub.core.ModuleManager;

/**
 * Mieszanie w {@code MultiPlayerGameMode#attack} - hook przed atakiem.
 *
 * <p>Moduly (np. Criticals) moga przejac atak (zwrocic true), wtedy oryginalne
 * wywolanie {@code attack} zostaje anulowane i modul sam decyduje o ataku.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

	@Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
	private void memehub$onAttack(Entity target, CallbackInfo ci) {
		if (ModuleManager.INSTANCE.onPreAttack(Minecraft.getInstance().player, target)) {
			ci.cancel();
		}
	}
}
