package pl.memehub.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.memehub.event.EntityMoveEvent;
import pl.memehub.event.EventBus;

/**
 * Mieszanie w {@code Entity#move} - wsparcie dla nadpisywania fizyki gry.
 *
 * <p>Kazdy ruch encji wysyla {@link EntityMoveEvent}. Anulowanie zdarzenia
 * zatrzymuje przemieszczenie encji; modyfikacja wektora (setMovement/setX/...)
 * zmienia faktyczne przemieszczenie (uzywane np. przez modul Velocity).
 *
 * <p>Implementacja: {@link #memehub$onMove} (HEAD, anulowalny) wywoluje event
 * i zapisuje zmodyfikowany wektor w polu {@code @Unique};
 * {@link #memehub$applyMove} ({@code @ModifyVariable}, HEAD) podmienia argument
 * metody. Kolejnosc deklaracji determinuje kolejnosc wykonania (Inject przed
 * ModifyVariable), a pole jest zerowane po odczycie.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

	@Unique
	private Vec3 memehub$modifiedMovement = null;

	@Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
			at = @At("HEAD"), cancellable = true)
	private void memehub$onMove(MoverType moverType, Vec3 movement, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		EntityMoveEvent event = EventBus.INSTANCE.post(new EntityMoveEvent(self, moverType, movement));
		if (event.isCancelled()) {
			ci.cancel();
			return;
		}
		memehub$modifiedMovement = event.modifiedMovement();
	}

	// ordinal indeksuje zmienne OSOBNO DLA KAZDEGO TYPU (LocalVariableDiscriminator.
	// initOrdinals), a argsOnly=true ogranicza liste do argumentow. move() ma dokladnie
	// JEDEN argument typu Vec3, wiec jego ordinal to 0 - wczesniej bylo 1 i mixin nie
	// znajdowal celu ("failed injection check, (0/1) succeeded. Scanned 0 target(s)").
	@ModifyVariable(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private Vec3 memehub$applyMove(Vec3 movement) {
		Vec3 modified = memehub$modifiedMovement;
		memehub$modifiedMovement = null;
		return modified != null ? modified : movement;
	}
}
