package su.uTa4u.specialforces.entities.goals;

import com.tacz.guns.api.entity.ShootResult;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import su.uTa4u.specialforces.entities.SwatEntity;

import java.util.EnumSet;

public class GunAttackGoal extends Goal {
    // TODO: Should depend on roundsPerMinute stored in GunData
    private static final int ATTACK_COOLDOWN = 40;

    private final SwatEntity shooter;
    private int lastAttackTick = 0;
    private boolean isAimingAtHead = false;

    private float bulletPitch;
    private float bulletYaw;

    public GunAttackGoal(SwatEntity shooter) {
        this.shooter = shooter;

        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.shooter.getTarget();
        return target != null && !target.isDeadOrDying();
    }

    @Override
    public void start() {
        this.shooter.aim(true);
        this.shooter.setAggressive(true);
    }

    @Override
    public void stop() {
        LivingEntity target = this.shooter.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
            this.shooter.setTarget(null);
        }
        this.shooter.aim(false);
        this.shooter.setAggressive(false);
        this.shooter.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = this.shooter.getTarget();
        if (target == null || target.isDeadOrDying()) return;

        double dist = this.shooter.distanceToSqr(target);
        if (dist > this.shooter.getGunAttackRadiusSqr() || !this.shooter.hasLineOfSight(target)) {
            this.shooter.getNavigation().moveTo(target, 1.0);
            return;
        }
        this.shooter.getNavigation().stop();

        double targetX = target.getX();
        double targetY = this.isAimingAtHead ? target.getEyeY() : getBodyY(target);
        double targetZ = target.getZ();

        this.shooter.getLookControl().setLookAt(targetX, targetY, targetZ);

        if (this.shooter.tickCount - lastAttackTick < ATTACK_COOLDOWN) return;
        lastAttackTick = this.shooter.tickCount;

        this.computeBulletPitchYaw(targetX, targetY, targetZ);
        ShootResult result = this.shooter.shoot(() -> this.bulletPitch, () -> this.bulletYaw);
        this.shooter.shoot(this.shooter::getXRot, this.shooter::getYHeadRot);
        if (result == ShootResult.NO_AMMO) {
            if (this.shooter.hasAmmoForGun(this.shooter.getMainHandItem())) {
                this.shooter.reload();
            } else {
                this.shooter.takeNextGun();
            }
        }

        this.isAimingAtHead = this.shooter.getRandom().nextFloat() < this.shooter.getSpecialty().getHeadAimChance();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private void computeBulletPitchYaw(double targetX, double targetY, double targetZ) {
        RandomSource rng = this.shooter.getRandom();
        double lookOffset = (3 - this.shooter.level().getDifficulty().getId() + 2) * 0.15;
        targetX += Mth.nextDouble(rng, -lookOffset, lookOffset);
        targetY += Mth.nextDouble(rng, -lookOffset, lookOffset);
        targetZ += Mth.nextDouble(rng, -lookOffset, lookOffset);
        double lookX = targetX - this.shooter.getX();
        double lookZ = targetZ - this.shooter.getZ();
        double lookY = targetY - this.shooter.getEyeY();
        double lookD = Math.sqrt(lookX * lookX + lookZ * lookZ);
        float bulletYaw = (float) (Mth.atan2(lookZ, lookX) * Mth.RAD_TO_DEG) - 90.0f;
        float bulletPitch = (float) (-(Mth.atan2(lookY, lookD) * Mth.RAD_TO_DEG));
        this.bulletYaw = rotateTowards(this.shooter.getYHeadRot(), bulletYaw, this.shooter.getHeadRotSpeed());
        this.bulletPitch = rotateTowards(this.shooter.getXRot(), bulletPitch, this.shooter.getMaxHeadXRot());
    }

    // Adapted from LookControl
    private static float rotateTowards(float from, float to, float max) {
        float f = Mth.degreesDifference(from, to);
        f =  Mth.clamp(f, -max, max);
        return from + f;
    }

    private static double getBodyY(Entity entity) {
        return (entity.getBoundingBox().minY + entity.getBoundingBox().maxY) / 2.0;
    }
}
