package su.uTa4u.specialforces.entities;

import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;
import su.uTa4u.specialforces.Specialty;

public class TestEntity extends PathfinderMob {

    private final Specialty SPECIALTY;
    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    protected TestEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.SPECIALTY = Specialty.getRandomSpecialty();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            setupAnimationStates();
        }
    }

    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = this.random.nextInt(32) + 32;
            this.idleAnimationState.start(this.tickCount);
        } else {
            --this.idleAnimationTimeout;
        }
    }

    @Override
    protected void updateWalkAnimation(float pPartialTick) {
        float f;
        if (this.idleAnimationState.isStarted()) {
            f = 0.0f;
        } else {
            f = Math.min(pPartialTick * 15.0f, 1.0f);
        }

        this.walkAnimation.update(f, 0.4f);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 69)
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.ATTACK_KNOCKBACK, 5);
    }

    public Specialty getSpecialty() {
        return this.SPECIALTY;
    }
}
