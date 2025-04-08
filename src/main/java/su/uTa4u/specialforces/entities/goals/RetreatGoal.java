package su.uTa4u.specialforces.entities.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import su.uTa4u.specialforces.entities.SwatEntity;

import java.util.EnumSet;

public class RetreatGoal extends Goal {
    private static final int RETREAT_DIST = 8;

    private final SwatEntity shooter;
    private Vec3 retreatPos;

    public RetreatGoal(SwatEntity shooter) {
        this.shooter = shooter;

        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity lastHurt = this.shooter.getLastHurtByMob();
        if (lastHurt == null || lastHurt.isDeadOrDying()) return false;
        Vec3 retreatPos = LandRandomPos.getPosAway(this.shooter, RETREAT_DIST, 4, lastHurt.position());
        if (retreatPos == null) {
            return false;
        } else {
            this.retreatPos = retreatPos;
            return true;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !this.shooter.getNavigation().isDone();
    }

    @Override
    public void start() {
        LivingEntity lastHurt = this.shooter.getLastHurtByMob();
        if (lastHurt == null || lastHurt.isDeadOrDying()) return;

        if (this.shooter.distanceToSqr(lastHurt) > (double) (RETREAT_DIST * RETREAT_DIST)) {
            this.shooter.crawl(true);
        } else {
            if (this.retreatPos != null) {
                float speed = this.shooter.getDataHolder().isCrawling ? 0.60f : 1.4f;
                this.shooter.getNavigation().moveTo(this.retreatPos.x, this.retreatPos.y, this.retreatPos.z, speed);
            }
        }
    }

    @Override
    public void stop() {
        this.shooter.crawl(false);
        this.retreatPos = null;
    }
}
