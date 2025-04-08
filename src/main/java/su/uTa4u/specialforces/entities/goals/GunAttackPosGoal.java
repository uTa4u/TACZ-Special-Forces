package su.uTa4u.specialforces.entities.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import su.uTa4u.specialforces.entities.SwatEntity;

import java.util.EnumSet;
import java.util.Optional;

public class GunAttackPosGoal extends Goal {
    private final SwatEntity shooter;
    private Vec3 posToTake;

    public GunAttackPosGoal(SwatEntity shooter) {
        this.shooter = shooter;

        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.shooter.getTarget();
        if (target == null || target.isDeadOrDying()) return false;

        double dist = this.shooter.distanceToSqr(target);
        if (dist > this.shooter.getGunAttackRadiusSqr()) {
            this.posToTake = target.position();
            return true;
        } else if (!this.shooter.hasLineOfSight(target)) {
            Vec3 posToTake = getPosToTake();
            if (posToTake == null) {
                return false;
            } else {
                this.posToTake = posToTake;
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !this.shooter.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.shooter.getNavigation().moveTo(this.posToTake.x, this.posToTake.y, this.posToTake.z, 1.0f);
    }

    @Override
    public void stop() {
        this.posToTake = null;
    }

    @Nullable
    private Vec3 getPosToTake() {
        BlockPos center = BlockPos.containing(this.shooter.position());
        //Iterable<BlockPos.MutableBlockPos> posIter = BlockPos.spiralAround(center, );
        Optional<BlockPos> posOpt = BlockPos.findClosestMatch(center, 8, 8, this::hasLineOfSight);
        if (posOpt.isEmpty()) {
            return null;
        }
        BlockPos pos = posOpt.get();
        return new Vec3(pos.getX(), pos.getY(), pos.getZ());
    }

    private boolean hasLineOfSight(BlockPos pos) {
        LivingEntity target = this.shooter.getTarget();
        if (target == null || target.isDeadOrDying()) return false;

        Vec3 posVec3 = new Vec3(pos.getX(), pos.getY(), pos.getZ());
        Vec3 targetVec3 = target.position();
        if (targetVec3.distanceToSqr(posVec3) > 128.0 * 128.0) {
            return false;
        } else {
            return this.shooter.level().clip(new ClipContext(posVec3, targetVec3, ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, target)).getType() == HitResult.Type.MISS;
        }
    }
}
