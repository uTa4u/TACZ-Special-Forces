package su.uTa4u.specialforces.entities;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.entity.shooter.*;
import com.tacz.guns.entity.sync.ModSyncedEntityData;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.uTa4u.specialforces.SpecialForces;
import su.uTa4u.specialforces.Specialty;
import su.uTa4u.specialforces.Util;
import su.uTa4u.specialforces.entities.goals.GunAttackGoal;
import su.uTa4u.specialforces.entities.goals.RetreatGoal;
import su.uTa4u.specialforces.menus.SwatCorpseMenu;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Supplier;

public class SwatEntity extends PathfinderMob implements IGunOperator, Container, MenuProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final EntityDimensions SWIMMING_DIMENSIONS = EntityDimensions.scalable(0.6F, 0.6F);
    private static final EntityDataAccessor<Specialty> SPECIALTY = SynchedEntityData.defineId(SwatEntity.class, ModEntityDataSerializers.SPECIAL_FORCE_SPECIALTY);
    // TODO: get rid of this
    private static final Attribute[] ATTRIBUTES = new Attribute[]{Attributes.MAX_HEALTH, Attributes.FOLLOW_RANGE, Attributes.KNOCKBACK_RESISTANCE, Attributes.MOVEMENT_SPEED, Attributes.ATTACK_DAMAGE, Attributes.ATTACK_KNOCKBACK, Attributes.ATTACK_SPEED, Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS,};

    // TODO:
    //  3. give supplies on spawn
    //  4. spawn corpse on death
//    public final AnimationState idleAnimationState = new AnimationState();
//    private int idleAnimationTimeout = 0;

    protected SwatEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.itemStacks = NonNullList.withSize(SWAT_INVENTORY_SIZE, ItemStack.EMPTY);
        this.itemHandler = LazyOptional.of(() -> new InvWrapper(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor levelAccessor, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType spawnType, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        // TODO: || spawnType == MobSpawnType.SPAWN_EGG
        if (spawnType == MobSpawnType.SPAWNER || spawnType == MobSpawnType.COMMAND) {
            this.setSpecialty(Specialty.getRandomSpecialty());
            SpecialForces.LOGGER.info("Random Spec: " + this.getSpecialty());
        }

        ServerLevel level = levelAccessor.getLevel();

        String gunId = "ak47";
        ItemStack gun = GunItemBuilder.create()
                .setId(Util.getTaczResource(gunId))
                .setAmmoCount(5)
                .setFireMode(FireMode.SEMI)
                .build();
        this.setItemInHand(InteractionHand.MAIN_HAND, gun);
        this.tacz$data.currentGunItem = () -> gun;
        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(Util.getTaczResource(gunId));
        if (gunIndexOptional.isEmpty()) {
            this.remove(RemovalReason.DISCARDED);
            SpecialForces.LOGGER.error("SwatEntity spawned with an invalid gun an was terminated: {}", gunId);
            return null;
        }
        AttachmentCacheProperty prop = new AttachmentCacheProperty();
        prop.eval(gun, gunIndexOptional.get().getGunData());
        this.updateCacheProperty(prop);

        this.copySpecialAttributes();
        if (!level.isClientSide) {
            this.registerCommonGoals();
            this.registerSpecialGoals();
        }

        if (this.getSpecialty() != Specialty.COMMANDER) return spawnData;

        // TODO: Spawn allies based on the current mission
//        Vec3 pos = this.position();
//        SwatEntity mob1 = new SwatEntity(level, Specialty.SNIPER, pos.x + 1, pos.y, pos.z + 1);
//        ForgeEventFactory.onFinalizeSpawn(mob1, level, difficulty, MobSpawnType.MOB_SUMMONED, null, null);
//        level.addFreshEntity(mob1);

        return spawnData;
    }

    @NotNull
    @Override
    protected InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        Level level = player.level();
        player.openMenu(new SimpleMenuProvider( (id, inv, p) -> new SwatCorpseMenu(id, inv, this), this.getTypeName()));

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void tick() {
        super.tick();
        this.taczTick();

        LivingEntity target = this.getTarget();
        if (this.tacz$data.isCrawling && (target == null || target.isDeadOrDying())) {
            this.crawl(false);
        }

        if (this.level().isClientSide) {
            setupAnimationStates();
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("specialty", this.getSpecialty().getName());
        ContainerHelper.saveAllItems(nbt, this.itemStacks);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        Specialty specialty = Specialty.SPECIALTY_BY_NAME.get(nbt.getString("specialty"));
        if (specialty != null) {
            this.setSpecialty(specialty);
        }
        ContainerHelper.loadAllItems(nbt, this.itemStacks);
    }

    private void setupAnimationStates() {
//        if (this.idleAnimationTimeout <= 0) {
//            this.idleAnimationTimeout = this.random.nextInt(32) + 32;
//            this.idleAnimationState.start(this.tickCount);
//        } else {
//            --this.idleAnimationTimeout;
//        }
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
    }

    private void registerCommonGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RetreatGoal(this));
        this.goalSelector.addGoal(2, new GunAttackGoal(this));
        // Use potion
        // RandomLookAroundGoal

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Pig.class, true));
    }

    public static AttributeSupplier.Builder createDefaultAttributes() {
        // TODO: rework this to not depend on ATTRIBUTES array (if possible)
        AttributeSupplier.Builder builder = PathfinderMob.createLivingAttributes();
        for (Attribute attribute : ATTRIBUTES) builder.add(attribute);
        return builder;
    }

    private void copySpecialAttributes() {
        // TODO: rework this to not depend on ATTRIBUTES array (if possible)
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
    public boolean canBeLeashed(@NotNull Player player) {
        return false;
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, @NotNull EntityDimensions dimensions) {
        return switch (pose) {
            case SWIMMING, FALL_FLYING, SPIN_ATTACK -> 0.4F;
            case CROUCHING -> 1.27F;
            default -> 1.62F;
        };
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return target.canBeSeenAsEnemy();
    }

    @NotNull
    @Override
    protected Component getTypeName() {
        return Specialty.TYPE_NAME_BY_SPECIALTY.get(this.getSpecialty());
    }

    @NotNull
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (pose == Pose.SWIMMING) return SWIMMING_DIMENSIONS;
        return super.getDimensions(pose);
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
        this.tacz$reload.reload();
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

    private void taczTick() {
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

    //////////////////////////////////////////////////////////////////////
    // Container and MenuProvider interfaces implementation begins here //
    // ItemHandler capability implementation begins here                //
    //////////////////////////////////////////////////////////////////////

    // Adapted from AbstractMinecartContainer and ContainerEntity

    // 2 * InventorySlots + ArmorSlots + OffhandSlot
    public static final int SWAT_INVENTORY_SIZE = 36 + 36 + 4 + 1;
    private final SimpleContainer inventory = new SimpleContainer(SWAT_INVENTORY_SIZE);
    private final NonNullList<ItemStack> itemStacks;
    private LazyOptional<?> itemHandler;

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction facing) {
        return capability == ForgeCapabilities.ITEM_HANDLER && this.isAlive() ? this.itemHandler.cast() : super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.itemHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        this.itemHandler = LazyOptional.of(() -> new InvWrapper(this));
    }

    public NonNullList<ItemStack> getItemStacks() {
        return this.itemStacks;
    }

    @Override
    public int getContainerSize() {
        return SWAT_INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        Iterator<ItemStack> iter = this.getItemStacks().iterator();
        ItemStack itemstack;
        do {
            if (!iter.hasNext()) return true;
            itemstack = iter.next();
        } while (itemstack.isEmpty());
        return false;
    }

    @NotNull
    @Override
    public ItemStack getItem(int slot) {
        return this.itemStacks.get(slot);
    }

    @NotNull
    @Override
    public ItemStack removeItem(int slot, int count) {
        return ContainerHelper.removeItem(this.itemStacks, slot, count);
    }

    @NotNull
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack itemstack = this.itemStacks.get(slot);
        if (itemstack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.itemStacks.set(slot, ItemStack.EMPTY);
            return itemstack;
        }
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack itemStack) {
        this.itemStacks.set(slot, itemStack);
        if (!itemStack.isEmpty() && itemStack.getCount() > this.getMaxStackSize()) {
            itemStack.setCount(this.getMaxStackSize());
        }
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return !this.isRemoved() && this.position().closerThan(player.position(), 8.0);
    }

    @Override
    public void clearContent() {
        this.itemStacks.clear();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new SwatCorpseMenu(containerId, playerInventory, this.inventory);
    }
}
