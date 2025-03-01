package su.uTa4u.specialforces.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Nullable;
import su.uTa4u.specialforces.SpecialForces;
import su.uTa4u.specialforces.Specialty;

public class SwatEntity extends PathfinderMob {

    private static final Attribute[] ATTRIBUTES;

    private static final EntityDataAccessor<Specialty> SPECIALTY = SynchedEntityData.defineId(SwatEntity.class, ModEntityDataSerializers.SPECIAL_FORCE_SPECIALTY);
    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    protected SwatEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public SwatEntity(Level level, Specialty specialty, double x, double y, double z) {
        this(ModEntities.TEST_ENTITY.get(), level);
        this.setSpecialty(specialty);
        SpecialForces.LOGGER.info("Spec in constr was set to: " + this.getSpecialty());
        this.setPos(x, y, z);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor levelAccessor, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        // TODO: spawnType == MobSpawnType.SPAWN_EGG
        if (spawnType == MobSpawnType.SPAWNER || spawnType == MobSpawnType.COMMAND) {
            this.setSpecialty(Specialty.getRandomSpecialty());
            SpecialForces.LOGGER.info("Random Spec: " + this.getSpecialty());
        }

        ServerLevel level = levelAccessor.getLevel();

        if (!level.isClientSide) this.registerSpecialGoals();
        this.copySpecialAttributes();

        if (this.getSpecialty() != Specialty.COMMANDER) return spawnData;

        // TODO: Spawn allies based on the current mission
        Vec3 pos = this.position();
        SwatEntity mob1 = new SwatEntity(level, Specialty.SNIPER, pos.x + 1, pos.y, pos.z + 1);
//            TestEntity mob2 = new TestEntity(level, Specialty.MEDIC, pos.x - 1, pos.y, pos.z - 1);
        ForgeEventFactory.onFinalizeSpawn(mob1, level, difficulty, MobSpawnType.BREEDING, spawnData, dataTag);
        level.addFreshEntity(mob1);

        return spawnData;
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
        AttributeSupplier.Builder builder = PathfinderMob.createLivingAttributes();
        for (Attribute attribute : ATTRIBUTES) builder.add(attribute, 1);
        return builder;
    }

    private void copySpecialAttributes() {
        AttributeSupplier src = this.getSpecialty().getAttributes();
        for (Attribute attribute : ATTRIBUTES) {
            AttributeInstance attributeInstance = this.getAttributes().getInstance(attribute);
            if (attributeInstance != null && src.hasAttribute(attribute)) {
                attributeInstance.setBaseValue(src.getBaseValue(attribute));
            }
        }
        this.setHealth(this.getMaxHealth());
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

    static {
        ATTRIBUTES = new Attribute[]{
                Attributes.MAX_HEALTH,
                Attributes.FOLLOW_RANGE,
                Attributes.KNOCKBACK_RESISTANCE,
                Attributes.MOVEMENT_SPEED,
                Attributes.FLYING_SPEED,
                Attributes.ATTACK_DAMAGE,
                Attributes.ATTACK_KNOCKBACK,
                Attributes.ATTACK_SPEED,
                Attributes.ARMOR,
                Attributes.ARMOR_TOUGHNESS,
        };
    }
}
