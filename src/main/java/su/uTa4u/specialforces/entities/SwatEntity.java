package su.uTa4u.specialforces.entities;

import com.mojang.logging.LogUtils;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.entity.shooter.*;
import com.tacz.guns.entity.sync.ModSyncedEntityData;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import su.uTa4u.specialforces.SpecialForces;
import su.uTa4u.specialforces.Specialty;
import su.uTa4u.specialforces.entities.goal.GunAttackGoal;

import java.util.Optional;
import java.util.function.Supplier;

public class SwatEntity extends PathfinderMob implements IGunOperator {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Attribute[] ATTRIBUTES = new Attribute[]{
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

    private static final EntityDataAccessor<Specialty> SPECIALTY = SynchedEntityData.defineId(SwatEntity.class, ModEntityDataSerializers.SPECIAL_FORCE_SPECIALTY);
    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    protected SwatEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public SwatEntity(Level level, Specialty specialty, double x, double y, double z) {
        this(ModEntities.TEST_ENTITY.get(), level);
        this.setSpecialty(specialty);
        LOGGER.info("Spec in constr was set to: " + this.getSpecialty());
        this.setPos(x, y, z);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor levelAccessor, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        // TODO: || spawnType == MobSpawnType.SPAWN_EGG
        if (spawnType == MobSpawnType.SPAWNER || spawnType == MobSpawnType.COMMAND) {
            this.setSpecialty(Specialty.getRandomSpecialty());
            LOGGER.info("Random Spec: " + this.getSpecialty());
        }

        ServerLevel level = levelAccessor.getLevel();

        this.copySpecialAttributes();
        if (!level.isClientSide) {
            // TODO: these should be compressed into a single method
            this.registerCommonGoals();
            this.registerSpecialGoals();
        }

        CompoundTag tag = new CompoundTag();
        tag.putString("GunId", "tacz:ak47");
        tag.putString("GunFireMode", "SEMI");
        tag.putInt("GunCurrentAmmoCount", 30);
        ItemStack gunItemStack = ModItems.MODERN_KINETIC_GUN.get().getDefaultInstance();
        Item gunItem = gunItemStack.getItem();
        gunItemStack.setTag(tag);
        this.setItemInHand(InteractionHand.MAIN_HAND, gunItemStack);
        this.tacz$data.currentGunItem = () -> gunItemStack;
        AttachmentCacheProperty prop = new AttachmentCacheProperty();
        if (gunItem instanceof IGun iGun) {
            ResourceLocation gunId = iGun.getGunId(gunItemStack);
            Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gunId);
            if (gunIndexOptional.isEmpty()) {
                this.terminate();
                return null;
            }
            CommonGunIndex gunIndex =  gunIndexOptional.get();
            prop.eval(gunItemStack, gunIndex.getGunData());
            this.updateCacheProperty(prop);
        } else {
            this.terminate();
            return null;
        }

        if (this.getSpecialty() != Specialty.COMMANDER) return spawnData;

        // TODO: Spawn allies based on the current mission
//        Vec3 pos = this.position();
//        SwatEntity mob1 = new SwatEntity(level, Specialty.SNIPER, pos.x + 1, pos.y, pos.z + 1);
//        ForgeEventFactory.onFinalizeSpawn(mob1, level, difficulty, MobSpawnType.MOB_SUMMONED, null, null);
//        level.addFreshEntity(mob1);

        return spawnData;
    }

    // TODO: remove before release, this is for debugging only
//    @Override
//    public boolean hurt(DamageSource source, float amount) {
//        Entity causingEntity = source.getEntity();
//        if (causingEntity instanceof LivingEntity living && !living.level().isClientSide && living.getItemInHand(InteractionHand.MAIN_HAND).is(Items.STICK)) {
//            LOGGER.debug("target: " + this.getTarget());
//            this.targetSelector.getRunningGoals().forEach((goal) -> LOGGER.debug("goal: " + goal.getGoal()));
//        }
//        return super.hurt(source, amount);
//    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            setupAnimationStates();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("specialty", this.getSpecialty().getName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        Specialty specialty = Specialty.getByName(compound.getString("specialty"));
        if (specialty != null) {
            this.setSpecialty(specialty);
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
                LOGGER.info("COMMANDER goals registered");
            }
            case ASSAULTER -> {
                LOGGER.info("ASSAULTER goals registered");
            }
            case GRENADIER -> {
                LOGGER.info("GRENADIER goals registered");
            }
            case BULLDOZER -> {
                LOGGER.info("BULLDOZER goals registered");
            }
            case ENGINEER -> {
                LOGGER.info("ENGINEER goals registered");
            }
            case SNIPER -> {
                LOGGER.info("SNIPER goals registered");
            }
            case MEDIC -> {
                LOGGER.info("MEDIC goals registered");
            }
            case SCOUT -> {
                LOGGER.info("SCOUT goals registered");
            }
            case SPY -> {
                LOGGER.info("SPY goals registered");
            }
        }
    }

    // TODO: remove/move this before release
    private void terminate() {
        this.remove(Entity.RemovalReason.DISCARDED);
        LOGGER.error("SwatEntity spawned with a defect and was TERMINATED.");
    }

    @Override
    protected void registerGoals() {
    }

    private void registerCommonGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
//        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0f, 1.0f));
        this.goalSelector.addGoal(2, new GunAttackGoal(this));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Pig.class, true));
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
//                LOGGER.debug("set attr: " + attribute.getDescriptionId());
                attributeInstance.setBaseValue(src.getBaseValue(attribute));
            }
        }
        this.setHealth(this.getMaxHealth());
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return target.canBeSeenAsEnemy();
    }

    @NotNull
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

    ///////////////////////////////////////////////////////
    // IGunOperator interface implementation begins here //
    ///////////////////////////////////////////////////////

    private final LivingEntity tacz$shooter = this;
    private final ShooterDataHolder tacz$data = new ShooterDataHolder();
    private final LivingEntityDrawGun tacz$draw = new LivingEntityDrawGun(tacz$shooter, tacz$data);
    private final LivingEntityAim tacz$aim = new LivingEntityAim(tacz$shooter, this.tacz$data);
    private final LivingEntityCrawl tacz$crawl = new LivingEntityCrawl(tacz$shooter, this.tacz$data);
    private final LivingEntityAmmoCheck tacz$ammoCheck = new LivingEntityAmmoCheck(tacz$shooter);
    private final LivingEntityFireSelect tacz$fireSelect = new LivingEntityFireSelect(tacz$shooter, this.tacz$data);
    private final LivingEntityMelee tacz$melee = new LivingEntityMelee(tacz$shooter, this.tacz$data, this.tacz$draw);
    private final LivingEntityShoot tacz$shoot = new LivingEntityShoot(tacz$shooter, this.tacz$data, this.tacz$draw);
    private final LivingEntityBolt tacz$bolt = new LivingEntityBolt(this.tacz$data, this.tacz$shooter, this.tacz$draw, this.tacz$shoot);
    private final LivingEntityReload tacz$reload = new LivingEntityReload(tacz$shooter, this.tacz$data, this.tacz$draw, this.tacz$shoot);
    private final LivingEntitySpeedModifier tacz$speed = new LivingEntitySpeedModifier(tacz$shooter, tacz$data);
    private final LivingEntitySprint tacz$sprint = new LivingEntitySprint(tacz$shooter, this.tacz$data);

    @Override
    public long getSynShootCoolDown() {
        return ModSyncedEntityData.SHOOT_COOL_DOWN_KEY.getValue(tacz$shooter);
    }

    @Override
    public long getSynMeleeCoolDown() {
        return ModSyncedEntityData.MELEE_COOL_DOWN_KEY.getValue(tacz$shooter);
    }

    @Override
    public long getSynDrawCoolDown() {
        return ModSyncedEntityData.DRAW_COOL_DOWN_KEY.getValue(tacz$shooter);
    }

    @Override
    public boolean getSynIsBolting() {
        return ModSyncedEntityData.IS_BOLTING_KEY.getValue(tacz$shooter);
    }

    @Override
    public ReloadState getSynReloadState() {
        return ModSyncedEntityData.RELOAD_STATE_KEY.getValue(tacz$shooter);
    }

    @Override
    public float getSynAimingProgress() {
        return ModSyncedEntityData.AIMING_PROGRESS_KEY.getValue(tacz$shooter);
    }

    @Override
    public boolean getSynIsAiming() {
        return ModSyncedEntityData.IS_AIMING_KEY.getValue(tacz$shooter);
    }

    @Override
    public float getSynSprintTime() {
        return ModSyncedEntityData.SPRINT_TIME_KEY.getValue(tacz$shooter);
    }

    @Override
    public void initialData() {
        this.tacz$data.initialData();
        // propusk
    }

    @Override
    public void draw(Supplier<ItemStack> gunItemSupplier) {
        this.tacz$draw.draw(gunItemSupplier);
//        this.isDrawn = true;
    }

    @Override
    public void bolt() {
        this.tacz$bolt.bolt();
    }

    @Override
    public void reload() {
//        if (this.level() instanceof ServerLevel) this.triggerAnim("reload", "reload");
        this.tacz$reload.reload();
//        this.isReloading = true;
    }

    @Override
    public void cancelReload() {
        this.tacz$reload.cancelReload();
    }

    @Override
    public void melee() {
        this.tacz$melee.melee();
    }

    @Override
    public ShootResult shoot(Supplier<Float> pitch, Supplier<Float> yaw) {
        return this.shoot(pitch, yaw, System.currentTimeMillis() - tacz$data.baseTimestamp);
    }

    @Override
    public ShootResult shoot(Supplier<Float> pitch, Supplier<Float> yaw, long timestamp) {
        return tacz$shoot.shoot(pitch, yaw, timestamp);
    }

    @Override
    public boolean needCheckAmmo() {
        return this.tacz$ammoCheck.needCheckAmmo();
    }

    @Override
    public boolean consumesAmmoOrNot() {
        return this.tacz$ammoCheck.consumesAmmoOrNot();
    }

    @Override
    public boolean getProcessedSprintStatus(boolean sprint) {
        return this.tacz$sprint.getProcessedSprintStatus(sprint);
    }

    @Override
    public void aim(boolean isAim) {
        this.tacz$aim.aim(isAim);
    }

    @Override
    public void crawl(boolean isCrawl) {
        this.tacz$crawl.crawl(isCrawl);
    }

    @Override
    public void updateCacheProperty(AttachmentCacheProperty cacheProperty) {
        this.tacz$data.cacheProperty = cacheProperty;
    }

    @Override
    @Nullable
    public AttachmentCacheProperty getCacheProperty() {
        return this.tacz$data.cacheProperty;
    }

    @Override
    public ShooterDataHolder getDataHolder() {
        return this.tacz$data;
    }

    @Override
    public boolean nextBulletIsTracer(int tracerCountInterval) {
        this.tacz$data.shootCount++;
        if (tracerCountInterval == -1) return false;
        return tacz$data.shootCount % (tracerCountInterval + 1) == 0;
    }

    @Override
    public void fireSelect() {
        this.tacz$fireSelect.fireSelect();
    }

    @Override
    public void zoom() {
        this.tacz$aim.zoom();
    }

    private void onServerSideTick() {
        if (level().isClientSide) return;

        ReloadState reloadState = this.tacz$reload.tickReloadState();
        this.tacz$aim.tickAimingProgress();
        this.tacz$aim.tickSprint();
        this.tacz$crawl.tickCrawling();
        this.tacz$bolt.tickBolt();
        this.tacz$melee.scheduleTickMelee();
        this.tacz$speed.updateSpeedModifier();
        tacz$shooter.setSprinting(getProcessedSprintStatus(tacz$shooter.isSprinting()));

        ModSyncedEntityData.SHOOT_COOL_DOWN_KEY.setValue(tacz$shooter, this.tacz$shoot.getShootCoolDown());
        ModSyncedEntityData.MELEE_COOL_DOWN_KEY.setValue(tacz$shooter, this.tacz$melee.getMeleeCoolDown());
        ModSyncedEntityData.DRAW_COOL_DOWN_KEY.setValue(tacz$shooter, this.tacz$draw.getDrawCoolDown());
        ModSyncedEntityData.IS_BOLTING_KEY.setValue(tacz$shooter, this.tacz$data.isBolting);
        ModSyncedEntityData.RELOAD_STATE_KEY.setValue(tacz$shooter, reloadState);
        ModSyncedEntityData.AIMING_PROGRESS_KEY.setValue(tacz$shooter, this.tacz$data.aimingProgress);
        ModSyncedEntityData.IS_AIMING_KEY.setValue(tacz$shooter, this.tacz$data.isAiming);
        ModSyncedEntityData.SPRINT_TIME_KEY.setValue(tacz$shooter, this.tacz$data.sprintTimeS);
    }

}
