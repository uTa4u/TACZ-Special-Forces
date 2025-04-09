package su.uTa4u.specialforces.entities.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import su.uTa4u.specialforces.config.CommonConfig;
import su.uTa4u.specialforces.entities.SwatEntity;

import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Predicate;

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
            Vec3 posToTake = getPosToTake((pos) -> this.hasNavPath(pos) && this.hasLineOfSight(pos));
            if (posToTake != null) {
                this.posToTake = posToTake;
                return true;
            } else {
                // If entity spawned in an unlucky spot there is a chance it will never reach the player,
                // but will take the limits we set for both commander and squad counts.
                // As a fix we will teleport such "stuck" entities to a position from which they can shoot.
                // Teleport attempts will not stop until the entity is successfully unstuck.
                this.shooter.incFailedGunPosCounter();
                if (this.shooter.getFailedGunPosCounter() >= CommonConfig.SWAT_ENTITY_FAILED_GUN_POS_LIMIT.get()) {
                    Vec3 tpPos = this.getPosToTake(this::hasLineOfSight);
                    if (tpPos != null) {
                        this.shooter.resetFailedGunPosCounter();
                        this.shooter.setPos(tpPos);
                    }
                }
                return false;
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
    private Vec3 getPosToTake(Predicate<BlockPos> predicate) {
        BlockPos center = BlockPos.containing(this.shooter.position());
        // BlockPos#spiralAround looks interesting, maybe we could try it out
        Optional<BlockPos> posOpt = BlockPos.findClosestMatch(center, 16, 16, predicate);
        if (posOpt.isEmpty()) {
            return null;
        }
        BlockPos pos = posOpt.get();
        return new Vec3(pos.getX(), pos.getY(), pos.getZ());
    }

    private boolean hasNavPath(BlockPos pos) {
        return this.shooter.getNavigation().createPath(pos, 1) != null;
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
