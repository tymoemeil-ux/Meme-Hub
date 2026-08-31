package pl.memehub.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Pomocnik wyboru celow.
 */
public final class EntityUtil {
	private EntityUtil() {
	}

	/**
	 * Sprawdza, czy encja jest poprawnym celem.
	 *
	 * @param entity   potencjalny cel
	 * @param self     gracz (wykluczany)
	 * @param players  czy celem moga byc gracze
	 * @param monsters czy celem moga byc potwory (Enemy)
	 * @param animals  czy celem moga byc inne zywe stworzenia (Mob nie-bedacy Enemy)
	 */
	public static boolean isValidTarget(Entity entity, Entity self, boolean players, boolean monsters, boolean animals) {
		if (entity == null || entity == self || !entity.isAlive() || !(entity instanceof LivingEntity living)) {
			return false;
		}
		if (entity instanceof Player player) {
			if (player.isSpectator() || player.isDeadOrDying()) {
				return false;
			}
			return players;
		}
		if (!(entity instanceof Mob)) {
			return false;
		}
		if (entity instanceof Enemy) {
			return monsters;
		}
		return animals;
	}

	/** Zwraca wszystkich zywych celow w zasiegu spelniajacych filtry. */
	public static List<LivingEntity> getTargetsInRange(Entity source, double range,
			boolean players, boolean monsters, boolean animals) {
		List<LivingEntity> result = new ArrayList<>();
		if (source.level() == null) {
			return result;
		}
		double rangeSq = range * range;
		for (Entity entity : source.level().getEntities(source, source.getBoundingBox().inflate(range))) {
			if (entity.distanceToSqr(source) > rangeSq) {
				continue;
			}
			if (!isValidTarget(entity, source, players, monsters, animals)) {
				continue;
			}
			if (source instanceof LivingEntity from && !from.hasLineOfSight(entity)) {
				continue;
			}
			result.add((LivingEntity) entity);
		}
		return result;
	}

	/** Zwraca najblizszy cel w zasiegu lub null. */
	public static LivingEntity getNearestTarget(Entity source, double range,
			boolean players, boolean monsters, boolean animals) {
		LivingEntity best = null;
		double bestDist = Double.MAX_VALUE;
		for (LivingEntity target : getTargetsInRange(source, range, players, monsters, animals)) {
			double dist = source.distanceToSqr(target);
			if (dist < bestDist) {
				bestDist = dist;
				best = target;
			}
		}
		return best;
	}
}
