package su.uTa4u.specialforces.entities.goals;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.EffectiveRangeModifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import su.uTa4u.specialforces.entities.SwatEntity;

import java.util.EnumSet;
import java.util.Objects;

public class GunAttackGoal extends Goal {
    private static final float EFFECTIVE_RANGE_MULT = 2.0f;
    private static final int ATTACK_COOLDOWN = 40;

    private final SwatEntity shooter;
    private Path path;
    private final float attackRadiusSqr;
    private int lastAttackTick = 0;
    private boolean isAimingAtHead = false;

    public GunAttackGoal(SwatEntity shooter) {
        this.shooter = shooter;

        AttachmentCacheProperty cacheProperty = Objects.requireNonNull(IGunOperator.fromLivingEntity(shooter).getCacheProperty());
        float effectiveRange = cacheProperty.getCache(EffectiveRangeModifier.ID);
        this.attackRadiusSqr = effectiveRange * effectiveRange * EFFECTIVE_RANGE_MULT * EFFECTIVE_RANGE_MULT;

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
        if (target == null || target.isDeadOrDying() || !this.shooter.hasLineOfSight(target)) return;

        double dist = this.shooter.distanceToSqr(target);
        if (dist > this.attackRadiusSqr) {
            this.shooter.getNavigation().moveTo(target, 1.0);
            return;
        }
        this.shooter.getNavigation().stop();

        RandomSource rng = this.shooter.getRandom();
        double lookOffset = (3 - this.shooter.level().getDifficulty().getId() + 2) * 0.15;
        double lookX = target.getX() + Mth.nextDouble(rng, -lookOffset, lookOffset);
        double lookZ = target.getZ() + Mth.nextDouble(rng, -lookOffset, lookOffset);
        double lookY = (this.isAimingAtHead ? target.getEyeY() : getBodyY(target)) + Mth.nextDouble(rng, -lookOffset, lookOffset);
        this.shooter.getLookControl().setLookAt(lookX, lookY, lookZ);

        if (this.shooter.tickCount - lastAttackTick < ATTACK_COOLDOWN) return;
        lastAttackTick = this.shooter.tickCount;

        this.isAimingAtHead = this.shooter.getRandom().nextFloat() < this.shooter.getSpecialty().getHeadAimChance();

        // TODO: instead of making entity look at the place they are about to shoot, calculate pitch and yaw
        //  while looking at head or body
        ShootResult result = this.shooter.shoot(this.shooter::getXRot, this.shooter::getYHeadRot);
        if (result == ShootResult.NO_AMMO) {
            this.shooter.reload();
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private static double getBodyY(Entity entity) {
        return (entity.getBoundingBox().minY + entity.getBoundingBox().maxY) / 2.0;
    }
}
