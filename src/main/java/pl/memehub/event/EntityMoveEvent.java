package pl.memehub.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/**
 * Zdarzenie ruchu encji (mixin {@code Entity#move}) - przyklad wsparcia dla
 * nadpisywania fizyki gry.
 *
 * <p>Zdarzenie jest ANULOWALNE (anulowanie zatrzymuje przemieszczenie) ORAZ
 * MUTOWALNE: obserwatorzy moga zmienic wektor ruchu przez {@link #setMovement(double, double, double)}
 * (lub setX/setY/setZ). Mixin uzywa wartosci {@link #modifiedMovement()} po
 * zakonczonym zdarzeniu.
 *
 * <pre>{@code
 * @Subscribe
 * public void onMove(EntityMoveEvent event) {
 *     if (event.entity() == Minecraft.getInstance().player && event.moverType() == MoverType.SELF) {
 *         event.setMovement(0, event.modifiedMovement().y, 0); // brak poziomego ruchu (test)
 *     }
 * }
 * }</pre>
 */
public final class EntityMoveEvent extends Event implements Cancellable {
	private final Entity entity;
	private final MoverType moverType;
	private final Vec3 originalMovement;

	private double x;
	private double y;
	private double z;
	private boolean cancelled = false;

	public EntityMoveEvent(Entity entity, MoverType moverType, Vec3 movement) {
		this.entity = entity;
		this.moverType = moverType;
		this.originalMovement = movement;
		this.x = movement.x;
		this.y = movement.y;
		this.z = movement.z;
	}

	public Entity entity() {
		return entity;
	}

	public MoverType moverType() {
		return moverType;
	}

	/** Oryginalny wektor ruchu (bez zmian obserwatorow). */
	public Vec3 originalMovement() {
		return originalMovement;
	}

	/** Biezacy wektor ruchu (po ewentualnych modyfikacjach). */
	public Vec3 modifiedMovement() {
		return new Vec3(x, y, z);
	}

	/** Alias czytelny w kodzie modulow. */
	public Vec3 movement() {
		return modifiedMovement();
	}

	public void setMovement(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void setX(double x) {
		this.x = x;
	}

	public void setY(double y) {
		this.y = y;
	}

	public void setZ(double z) {
		this.z = z;
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
