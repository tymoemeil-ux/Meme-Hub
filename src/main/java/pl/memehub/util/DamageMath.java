package pl.memehub.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side explosion damage calculations (crystals, anchors).
 *
 * <p>The code mirrors vanilla {@code Explosion} logic:
 * <ul>
 *   <li>{@code exposure} - fraction of rays from the explosion source to the target
 *       that are not blocked (raytraced via {@code Level#clip}),</li>
 *   <li>{@code distanceFactor = sqrt(dist^2) / (2 * power)},</li>
 *   <li>{@code damage = 7 * 2 * power * (1 - distanceFactor) * exposure}.</li>
 * </ul>
 * The value is raw explosion damage - the server applies armour/enchantment
 * reduction. The calculation is used for target selection and placement scoring.
 */
public final class DamageMath {
	/** Explosion power of an End crystal (as in vanilla). */
	public static final double CRYSTAL_POWER = 6.0;
	/** Explosion power of a charged Respawn Anchor / bed. */
	public static final double ANCHOR_POWER = 5.0;

	private DamageMath() {
	}

	/** Estimated damage from a crystal placed at {@code crystalPos}. */
	public static float crystalDamage(Level level, Vec3 crystalPos, LivingEntity target) {
		return explosionDamage(level, crystalPos, CRYSTAL_POWER, target);
	}

	/** Estimated damage from a charged Respawn Anchor at {@code anchorPos}. */
	public static float anchorDamage(Level level, BlockPos anchorPos, LivingEntity target) {
		return explosionDamage(level, Vec3.atCenterOf(anchorPos), ANCHOR_POWER, target);
	}

	/**
	 * Calculates raw explosion damage of {@code power} at {@code source} for {@code target}.
	 */
	public static float explosionDamage(Level level, Vec3 source, double power, LivingEntity target) {
		if (level == null || target == null) {
			return 0.0F;
		}
		double distanceSq = source.distanceToSqr(target.getX(), target.getY(), target.getZ());
		double distanceFactor = Math.sqrt(distanceSq) / (power * 2.0);
		if (distanceFactor >= 1.0) {
			return 0.0F;
		}
		double exposure = getSeenPercent(level, source, target);
		double damage = 7.0 * (power + power) * (1.0 - distanceFactor) * exposure;
		return Math.max(0.0F, (float) damage);
	}

	/**
	 * Vanilla-equivalent of {@code Explosion#getSeenPercent}: fraction of samples
	 * on the target's bounding box that have a clear line of sight from the source.
	 */
	public static float getSeenPercent(Level level, Vec3 source, Entity entity) {
		AABB aabb = entity.getBoundingBox();
		double d0 = 1.0 / ((aabb.maxX - aabb.minX) * 2.0 + 1.0);
		double d1 = 1.0 / ((aabb.maxY - aabb.minY) * 2.0 + 1.0);
		double d2 = 1.0 / ((aabb.maxZ - aabb.minZ) * 2.0 + 1.0);
		double d3 = (1.0 - Math.floor(1.0 / d0) * d0) / 2.0;
		double d4 = (1.0 - Math.floor(1.0 / d2) * d2) / 2.0;
		if (!(d0 < 0.0) && !(d1 < 0.0) && !(d2 < 0.0)) {
			int hit = 0;
			int total = 0;
			for (double d5 = 0.0; d5 <= 1.0; d5 += d0) {
				for (double d6 = 0.0; d6 <= 1.0; d6 += d1) {
					for (double d7 = 0.0; d7 <= 1.0; d7 += d2) {
						double x = Mth.lerp(d5, aabb.minX, aabb.maxX);
						double y = Mth.lerp(d6, aabb.minY, aabb.maxY);
						double z = Mth.lerp(d7, aabb.minZ, aabb.maxZ);
						Vec3 sample = new Vec3(x + d3, y, z + d4);
						ClipContext context = new ClipContext(sample, source, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity);
						if (level.clip(context).getType() == HitResult.Type.MISS) {
							hit++;
						}
						total++;
					}
				}
			}
			return (float) hit / (float) total;
		}
		return 0.0F;
	}

	/** Rough armour-reduced estimate (HUD display only; server does the real math). */
	public static float armorAdjusted(float rawDamage, LivingEntity target) {
		if (target == null) {
			return rawDamage;
		}
		int armor = target.getArmorValue();
		double reduction = armor / 25.0;
		return (float) Math.max(0.0, rawDamage * (1.0 - reduction));
	}
}
