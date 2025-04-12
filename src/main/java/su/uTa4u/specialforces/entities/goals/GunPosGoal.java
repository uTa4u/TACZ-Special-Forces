package su.uTa4u.specialforces.entities.goals;

import com.tacz.guns.api.item.IGun;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import su.uTa4u.specialforces.Specialty;
import su.uTa4u.specialforces.config.CommonConfig;
import su.uTa4u.specialforces.entities.SwatEntity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class GunPosGoal extends Goal {
    private final SwatEntity shooter;
    private Vec3 posToTake;
    private int holdPosTimer = 0;

    public GunPosGoal(SwatEntity shooter) {
        this.shooter = shooter;

        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(this.shooter.getMainHandItem().getItem() instanceof IGun)) return false;

        this.holdPosTimer += 1;
        if (this.holdPosTimer < CommonConfig.SWAT_ENTITY_HOLD_POSITION_DURATION.get()) return false;
        this.holdPosTimer = 0;

        LivingEntity target = this.shooter.getTarget();
        if (target == null || target.isDeadOrDying()) return false;

        double dist = this.shooter.distanceToSqr(target);
        if (dist > this.shooter.getGunAttackRadiusSqr()) {
            this.posToTake = target.position();
            return true;
        } else if (!this.shooter.hasLineOfSight(target)) {
            List<BlockPos> haveLineOfSight = posWithLineOfSight();
            List<BlockPos> haveLineOfSightAndPath = haveLineOfSight.stream().filter(this::hasNavPath).toList();
            Vec3 posToTake = null;
            if (!haveLineOfSightAndPath.isEmpty()) {
                BlockPos pos = haveLineOfSightAndPath.get(0);
                posToTake = new Vec3(pos.getX(), pos.getY(), pos.getZ());
            }
            if (posToTake != null) {
                this.posToTake = posToTake;
                return true;
            }
            // If entity spawned in an unlucky spot there is a chance it will never reach the player,
            // but will take the limits we set for both commander and squad counts.
            // As a fix we will teleport such "stuck" entities to a position from which they can shoot.
            this.shooter.incFailedGunPosCounter();
            if (this.shooter.getFailedGunPosCounter() >= CommonConfig.SWAT_ENTITY_FAILED_GUN_POS_LIMIT.get()) {
                this.shooter.resetFailedGunPosCounter();
                Vec3 tpPos = null;
                if (this.shooter.getSpecialty() == Specialty.COMMANDER) {
                    for (SwatEntity swat : this.shooter.getSquad()) {
                        if (swat.hasLineOfSight(target)) {
                            tpPos = swat.position();
                            break;
                        }
                    }
                } else {
                    SwatEntity commander = this.shooter.getCommander();
                    if (commander != null) {
                        for (SwatEntity swat : commander.getSquad()) {
                            if (swat.hasLineOfSight(target)) {
                                tpPos = swat.position();
                                break;
                            }
                        }
                    }
                }
                if (tpPos == null) {
                    if (!haveLineOfSight.isEmpty()) {
                        BlockPos pos = haveLineOfSight.get(0);
                        tpPos = new Vec3(pos.getX(), pos.getY(), pos.getZ());
                    }
                }
                if (tpPos != null) {
                    this.shooter.setPos(tpPos);
                }
            }
            return false;
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

    private List<BlockPos> posWithLineOfSight() {
        List<BlockPos> ret = new ArrayList<>();
        for (BlockPos pos : BlockPos.withinManhattan(this.shooter.blockPosition(), 8, 4, 8)) {
            if (this.hasLineOfSight(pos)) {
                ret.add(pos);
            }
        }
        return ret;
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
