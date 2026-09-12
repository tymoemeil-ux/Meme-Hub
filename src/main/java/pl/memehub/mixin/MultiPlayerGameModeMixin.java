package pl.memehub.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.memehub.core.ModuleManager;

/**
 * Mieszanie w {@code MultiPlayerGameMode#attack} - hook przed atakiem.
 *
 * <p>W Minecraft 26.2 sygnatura to {@code attack(Player, Entity)} (wczesniej
 * {@code attack(Entity)}) - cel mixin musi pasowac co do argumentow, inaczej
 * Mixin zglosi "target was not found" i gra sie nie uruchomi.
 *
 * <p>Moduly (np. Criticals) moga przejac atak (zwrocic true), wtedy oryginalne
 * wywolanie {@code attack} zostaje anulowane i modul sam decyduje o ataku.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

	@Inject(method = "attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V",
			at = @At("HEAD"), cancellable = true)
	private void memehub$onAttack(Player player, Entity target, CallbackInfo ci) {
		if (!(player instanceof LocalPlayer localPlayer)) {
			return;
		}
		if (ModuleManager.INSTANCE.onPreAttack(localPlayer, target)) {
			ci.cancel();
		}
	}
}
