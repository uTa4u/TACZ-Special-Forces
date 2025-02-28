package su.uTa4u.specialforces.entities;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import su.uTa4u.specialforces.SpecialForces;
import su.uTa4u.specialforces.Specialty;
import su.uTa4u.specialforces.Util;

public class TestEntity extends PathfinderMob {

    private static final EntityDataAccessor<Specialty> SPECIALTY = SynchedEntityData.defineId(TestEntity.class, ModEntityDataSerializers.SPECIAL_FORCE_SPECIALTY);
    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    protected TestEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public TestEntity(Level level, Specialty specialty, double x, double y, double z) {
        this(ModEntities.TEST_ENTITY.get(), level);
        this.setSpecialty(specialty);
        this.setPos(x, y, z);
    }

    @Override
    public void onAddedToWorld() {
        Level level = this.level();
        MobSpawnType reason = this.getSpawnType();

        // TODO: reason == MobSpawnType.SPAWN_EGG
        if (reason == MobSpawnType.SPAWNER || reason == MobSpawnType.COMMAND) {
            this.setSpecialty(Specialty.getRandomSpecialty());
        }

        if (!level.isClientSide) this.registerSpecialGoals();
        
        Util.copyAttributes(this.getAttributes(), this.getSpecialty().getAttributes());
        this.setHealth(this.getMaxHealth());

        if (this.getSpecialty() == Specialty.COMMANDER) {
            // Spawn allies based on the current mission
            Vec3 pos = this.position();
            // TODO: This even if entity was loaded from disk, not freshly created. Not the worst, but ideally should
            //  only run once - on freshly created.
            //  maybe finalizeSpawn should only process Commander (natural spawn) and other spawns
            //  should be processed (set specialty, goals, attributes) in the custom constructor?
            level.addFreshEntity(new TestEntity(level, Specialty.SNIPER, pos.x + 1, pos.y, pos.z + 1));
            level.addFreshEntity(new TestEntity(level, Specialty.MEDIC, pos.x - 1, pos.y, pos.z - 1));
        }

        super.onAddedToWorld();
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

    private void registerSpecialGoals() {
        switch (this.getSpecialty()) {
            case COMMANDER -> {
                SpecialForces.LOGGER.info("COMMANDER goals registered");
            }
            case ASSAULTER -> {
                SpecialForces.LOGGER.info("ASSAULTER goals registered");
            }
            case GRENADIER -> {
                SpecialForces.LOGGER.info("GRENADIER goals registered");
            }
            case BULLDOZER -> {
                SpecialForces.LOGGER.info("BULLDOZER goals registered");
            }
            case ENGINEER -> {
                SpecialForces.LOGGER.info("ENGINEER goals registered");
            }
            case SNIPER -> {
                SpecialForces.LOGGER.info("SNIPER goals registered");
            }
            case MEDIC -> {
                SpecialForces.LOGGER.info("MEDIC goals registered");
            }
            case SCOUT -> {
                SpecialForces.LOGGER.info("SCOUT goals registered");
            }
            case SPY -> {
                SpecialForces.LOGGER.info("SPY goals registered");
            }
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    public static AttributeSupplier.Builder createDefaultAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1)
                .add(Attributes.FOLLOW_RANGE, 1)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.MOVEMENT_SPEED, 1)
                .add(Attributes.FLYING_SPEED, 1)
                .add(Attributes.ATTACK_DAMAGE, 1)
                .add(Attributes.ATTACK_KNOCKBACK, 1)
                .add(Attributes.ATTACK_SPEED, 1)
                .add(Attributes.ARMOR, 1)
                .add(Attributes.ARMOR_TOUGHNESS, 1);
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable("entity." + SpecialForces.MOD_ID + "." + this.getSpecialty().getName());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SPECIALTY, Specialty.COMMANDER);
    }

    public Specialty getSpecialty() {
        return this.entityData.get(SPECIALTY);
    }

    private void setSpecialty(Specialty specialty) {
        this.entityData.set(SPECIALTY, specialty);
    }
}
