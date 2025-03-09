package su.uTa4u.specialforces.entities.goal;

import com.tacz.guns.api.entity.ShootResult;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import su.uTa4u.specialforces.SpecialForces;
import su.uTa4u.specialforces.entities.SwatEntity;

import java.util.EnumSet;

public class GunAttackGoal extends Goal {
    private final SwatEntity shooter;
    private long lastCanUseCheck;
    private Path path;
    private final float effectiveRange;

    public GunAttackGoal(SwatEntity shooter) {
        this.shooter = shooter;
//        AttachmentCacheProperty cacheProperty = Objects.requireNonNull(IGunOperator.fromLivingEntity(shooter).getCacheProperty());
//        this.effectiveRange = cacheProperty.getCache("effective_range");
        this.effectiveRange = 8.0f;

        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        long time = this.shooter.level().getGameTime();
        if (time - this.lastCanUseCheck < 20L) {
            return false;
        }
        this.lastCanUseCheck = time;

        // TODO: shoot non LivingEntity targets if no living target available (ITargetEntity)
        LivingEntity target = this.shooter.getTarget();
        if (target == null || target.isDeadOrDying()) return false;

        // TODO: try setting accuracy to shooting range
//        this.path = this.shooter.getNavigation().createPath(target, 0);
//        if (this.path != null) {
//            return true;
//        } else {
//            return this.effectiveRange * this.effectiveRange >= this.shooter.distanceToSqr(target.getX(), target.getY(), target.getZ());
//        }
        return true;
    }

    @Override
    public void start() {
        // TODO: speed modifier if was shooter was shot previously
        this.shooter.getNavigation().moveTo(this.path, 1D);
        this.shooter.setAggressive(true);
        // TODO: time until change position ?
    }

    @Override
    public void stop() {
        LivingEntity target = this.shooter.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
            this.shooter.setTarget(null);
        }
        this.shooter.setAggressive(false);
        this.shooter.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = this.shooter.getTarget();
        if (target == null || target.isDeadOrDying()) return;

        if (!this.shooter.getLookControl().isLookingAtTarget()) return;

        this.shooter.aim(true);
        ShootResult result = this.shooter.shoot(this.shooter::getXRot, this.shooter::getYHeadRot);
        SpecialForces.LOGGER.debug("shootResult: " + result);

    }

}
