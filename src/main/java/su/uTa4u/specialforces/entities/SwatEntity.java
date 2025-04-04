package su.uTa4u.specialforces.entities;

import com.google.common.collect.ImmutableList;
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
import com.tacz.guns.resource.modifier.custom.EffectiveRangeModifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.uTa4u.specialforces.ModTags;
import su.uTa4u.specialforces.SpecialForces;
import su.uTa4u.specialforces.Specialty;
import su.uTa4u.specialforces.Util;
import su.uTa4u.specialforces.entities.goals.GunAttackGoal;
import su.uTa4u.specialforces.menus.SwatCorpseMenu;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class SwatEntity extends PathfinderMob implements IGunOperator, Container, MenuProvider {
    private static final EntityDimensions BOX_DIMENSIONS = EntityDimensions.scalable(0.6f, 0.6f);
    private static final EntityDataAccessor<Specialty> SPECIALTY = SynchedEntityData.defineId(SwatEntity.class, ModEntityDataSerializers.SPECIAL_FORCE_SPECIALTY);
    private static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(SwatEntity.class, EntityDataSerializers.BYTE);
    // Entities shoot a little further than their weapon's effective range
    private static final float EFFECTIVE_RANGE_MULT = 2.0f;
    public static final byte STATE_ALIVE = 0;
    public static final byte STATE_DOWN = 1;
    public static final byte STATE_DEAD = 2;

    private static final float DOWN_HEALTH_THRESHOLD = 20.0f;

    private float currentGunAttackRadiusSqr;

//    public final AnimationState idleAnimationState = new AnimationState();
//    private int idleAnimationTimeout = 0;

    protected SwatEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
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
                .setAmmoCount(20)
                .setFireMode(FireMode.BURST)
                .build();
        // TODO: place item in inventory and take it in hand
        //  copy methods from Inventory
//        this.setItemInHand(InteractionHand.MAIN_HAND, gun);
        this.tacz$data.currentGunItem = () -> gun;
        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(Util.getTaczResource(gunId));
        if (gunIndexOptional.isEmpty()) {
            this.remove(RemovalReason.DISCARDED);
            SpecialForces.LOGGER.error("SwatEntity spawned with an invalid gun and was terminated: " + gunId);
            return null;
        }
        AttachmentCacheProperty prop = new AttachmentCacheProperty();
        prop.eval(gun, gunIndexOptional.get().getGunData());
        this.updateCacheProperty(prop);
        float effectiveRange = prop.getCache(EffectiveRangeModifier.ID);
        this.currentGunAttackRadiusSqr = effectiveRange * effectiveRange * EFFECTIVE_RANGE_MULT * EFFECTIVE_RANGE_MULT;

        this.generateInventory();
        // TODO: give all loot table pools names

        this.copySpecialAttributes();

        if (!level.isClientSide) {
            // TODO: this should be called on added to world
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

        // TODO: make this into a command
        if (!level.isClientSide && player.isCreative() && this.isInvulnerable() && player.getMainHandItem().is(Items.WOODEN_AXE)) {
            this.remove(RemovalReason.KILLED);
            return InteractionResult.SUCCESS;
        }

        if (this.getState() == STATE_DEAD) {
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new SwatCorpseMenu(id, inv, this), this.getTypeName()));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public void heal(float healAmount) {
        // Can't heal from dying...
        if (this.getState() == STATE_DEAD) return;

        super.heal(healAmount);

        if (this.getHealth() > DOWN_HEALTH_THRESHOLD) {
            // Swat Entity has healed above threshold
            this.setState(STATE_ALIVE);
            this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED));
        }
    }

    @Override
    protected void actuallyHurt(@NotNull DamageSource damageSource, float damageAmount) {
        float dmg = damageAmount;
        dmg = this.getDamageAfterArmorAbsorb(damageSource, dmg);
        dmg = this.getDamageAfterMagicAbsorb(damageSource, dmg);
        dmg = Math.max(dmg - this.getAbsorptionAmount(), 0.0f);

        float hpAfterDmg = this.getHealth() - dmg;
        if (hpAfterDmg <= DOWN_HEALTH_THRESHOLD) {
            // Swat Entity goes down. It can attack, but can't move.
            // Can heal and be healed, if health goes above the threshold, it goes up.
            this.setState(STATE_DOWN);
            this.setSpeed(0.0f);

            if (hpAfterDmg <= 0.0f) {
                // Swat Entity is dead. Can't attack, move or be moved.
                // Will despawn after some time, will despawn faster if inventory is empty.
                this.setState(STATE_DEAD);
                this.setInvulnerable(true);
                this.setHealth(1.0f);
                this.setNoAi(true);
                return;
            }
        }

        super.actuallyHurt(damageSource, damageAmount);
    }

    @Override
    public void tick() {
        super.tick();
        this.taczTick();

        LivingEntity target = this.getTarget();
        if (this.tacz$data.isCrawling && (target == null || target.isDeadOrDying() || this.getState() != STATE_ALIVE)) {
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
        nbt.putByte("state", this.getState());

        ContainerHelper.saveAllItems(nbt, this.items);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

        if (nbt.contains("specialty")) this.setSpecialty(Specialty.SPECIALTY_BY_NAME.get(nbt.getString("specialty")));

        if (nbt.contains("state")) this.setState(nbt.getByte("state"));

        ContainerHelper.loadAllItems(nbt, this.items);

        // TODO: fix entities having their health set to 20 if it was lower when saving
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
        this.goalSelector.addGoal(0, new FloatGoal(this));
//        this.goalSelector.addGoal(1, new RetreatGoal(this));
        this.goalSelector.addGoal(2, new GunAttackGoal(this));
        // Use potion
        // RandomLookAroundGoal

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Pig.class, true));
    }

    public static AttributeSupplier.Builder createDefaultAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.FOLLOW_RANGE)
                .add(Attributes.ATTACK_DAMAGE)
                .add(Attributes.ATTACK_KNOCKBACK)
                .add(Attributes.ATTACK_SPEED);
    }

    private void copySpecialAttributes() {
        this.getAttributes().assignValues(this.getSpecialty().getAttributes());
        this.setHealth(this.getMaxHealth());
    }

    private void generateInventory() {
        Level level = this.level();
        if (!(level instanceof ServerLevel serverLevel)) return;
        LootTable lootTable = serverLevel.getServer().getLootData().getLootTable(this.getSpecialty().getLootTable());
        LootParams lootParams = new LootParams.Builder(serverLevel).create(LootContextParamSets.EMPTY);

        // TODO: shuffle and split itemstacks like how LootTable#fill does
        ObjectArrayList<ItemStack> itemStacks = lootTable.getRandomItems(lootParams);
        for (int i = itemStacks.size() - 1; i >= 0; --i) {
            ItemStack itemStack = itemStacks.remove(i);
            if (itemStack.is(ModTags.Items.RULE_HOTBAR) && this.getFreeHotbarIndex() != -1) {
                this.items.set(this.getFreeHotbarIndex(), itemStack);
            } else if (itemStack.canEquip(EquipmentSlot.HEAD, this) && this.armor.get(0).isEmpty()) {
                this.armor.set(0, itemStack);
            } else if (itemStack.canEquip(EquipmentSlot.CHEST, this) && this.armor.get(1).isEmpty()) {
                this.armor.set(1, itemStack);
            } else if (itemStack.canEquip(EquipmentSlot.LEGS, this) && this.armor.get(2).isEmpty()) {
                this.armor.set(2, itemStack);
            } else if (itemStack.canEquip(EquipmentSlot.FEET, this) && this.armor.get(3).isEmpty()) {
                this.armor.set(3, itemStack);
            } else if (itemStack.is(ModTags.Items.RULE_OFFHAND) && this.offhand.get(0).isEmpty()) {
                this.offhand.set(0, itemStack);
            } else {
                int index = this.getFreeInvIndex();
                if (index != -1) {
                   this.items.set(index, itemStack);
                }
            }
        }

    }

    @Override
    public boolean canBeLeashed(@NotNull Player player) {
        return false;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return target.canBeSeenAsEnemy();
    }

    @NotNull
    @Override
    protected Component getTypeName() {
        return Specialty.TYPE_NAMES.get(this.getSpecialty());
    }

    @NotNull
    @Override
    public EntityDimensions getDimensions(@NotNull Pose pose) {
        if (pose == Pose.SWIMMING || this.getState() != STATE_ALIVE) return BOX_DIMENSIONS;
        return super.getDimensions(pose);
    }

    @Override
    protected float getStandingEyeHeight(@NotNull Pose pose, @NotNull EntityDimensions dimensions) {
        if (pose == Pose.SWIMMING || this.getState() != STATE_ALIVE) return 0.4f;
        return 1.62f;
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STATE.equals(key)) {
            this.refreshDimensions();
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SPECIALTY, Specialty.COMMANDER);
        this.entityData.define(STATE, STATE_ALIVE);
    }

    public Specialty getSpecialty() {
        return this.entityData.get(SPECIALTY);
    }

    private void setSpecialty(Specialty specialty) {
        this.entityData.set(SPECIALTY, specialty);
    }

    public byte getState() {
        return this.entityData.get(STATE);
    }

    private void setState(byte state) {
        this.entityData.set(STATE, state);
    }

    public float getGunAttackRadiusSqr() {
        return this.currentGunAttackRadiusSqr;
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
    }

    @Override
    public void draw(Supplier<ItemStack> gunItemSupplier) {
        this.tacz$draw.draw(gunItemSupplier);
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

    public static final int SWAT_INVENTORY_SIZE = 36;
    public static final int SWAT_ARMOR_SIZE = 4;
    public static final int SWAT_OFFHAND_SIZE = 1;
    public static final int SWAT_CONTAINER_SIZE = SWAT_INVENTORY_SIZE + SWAT_ARMOR_SIZE + SWAT_OFFHAND_SIZE;
    private static final int HOTBAR_INDEX_START = 0;
    private static final int HOTBAR_INDEX_END = 8;
    private static final int INV_INDEX_START = 9;
    private static final int INV_INDEX_END = 35;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SWAT_INVENTORY_SIZE, ItemStack.EMPTY);
    private final NonNullList<ItemStack> armor = NonNullList.withSize(SWAT_ARMOR_SIZE, ItemStack.EMPTY);
    private final NonNullList<ItemStack> offhand = NonNullList.withSize(SWAT_OFFHAND_SIZE, ItemStack.EMPTY);
    private final List<NonNullList<ItemStack>> compartments = ImmutableList.of(this.items, this.armor, this.offhand);
    private LazyOptional<?> itemHandler = LazyOptional.of(() -> new InvWrapper(this));

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

    @Override
    public int getContainerSize() {
        return SWAT_CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (NonNullList<ItemStack> compartment : this.compartments) {
            for (ItemStack itemStack : compartment) {
                if (!itemStack.isEmpty()) return false;
            }
        }
        return true;
    }

    @NotNull
    @Override
    public ItemStack getItem(int index) {
        NonNullList<ItemStack> compartment = this.getCompartment(index);
        return compartment == null ? ItemStack.EMPTY : compartmentSafeGet(compartment, index);
    }

    @NotNull
    @Override
    public ItemStack removeItem(int index, int count) {
        if (count <= 0) return ItemStack.EMPTY;
        NonNullList<ItemStack> compartment = this.getCompartment(index);
        if (compartment == null) return ItemStack.EMPTY;
        ItemStack itemStack = compartmentSafeGet(compartment, index);
        if (itemStack.isEmpty()) return ItemStack.EMPTY;
        return itemStack.split(count);
    }

    @NotNull
    @Override
    public ItemStack removeItemNoUpdate(int index) {
        NonNullList<ItemStack> compartment = this.getCompartment(index);
        if (compartment == null) return ItemStack.EMPTY;
        ItemStack itemStack = compartmentSafeGet(compartment, index);
        if (itemStack.isEmpty()) return ItemStack.EMPTY;
        compartment.set(index, ItemStack.EMPTY);
        return itemStack;
    }

    @Override
    public void setItem(int index, @NotNull ItemStack itemStack) {
        NonNullList<ItemStack> compartment = this.getCompartment(index);
        if (compartment != null) {
            compartmentSafeSet(compartment, index, itemStack);
        }
    }

    private int getFreeHotbarIndex() {
        for (int i = HOTBAR_INDEX_START; i <= HOTBAR_INDEX_END; ++i) {
            if (this.items.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private int getFreeInvIndex() {
        for (int i = INV_INDEX_START; i <= INV_INDEX_END; ++i) {
            if (this.items.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack itemStacks) {
        return false;
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
        for (List<ItemStack> list : this.compartments) {
            list.clear();
        }
    }

    // Common code for finding NonNullList (items / armor / offhand) by slot index
    @Nullable
    private NonNullList<ItemStack> getCompartment(int index) {
        NonNullList<ItemStack> ret = null;
        NonNullList<ItemStack> compartment;
        for (Iterator<NonNullList<ItemStack>> iter = this.compartments.iterator(); iter.hasNext(); index -= compartment.size()) {
            compartment = iter.next();
            if (index < compartment.size()) {
                ret = compartment;
                break;
            }
        }
        return ret;
    }

    // Most of the container code in here was adapted from Inventory class used by player.
    // In Inventory methods somehow don't break even when they try to access index 36 of armor list (size 4)
    // I don't feel like finding out why and how, so we get this...
    private static ItemStack compartmentSafeGet(NonNullList<ItemStack> compartment, int index) {
        return compartment.get(index % compartment.size());
    }

    private static void compartmentSafeSet(NonNullList<ItemStack> compartment, int index, ItemStack itemStack) {
        compartment.set(index % compartment.size(), itemStack);
    }

    @Override
    public int getMaxStackSize() {
        return Container.super.getMaxStackSize();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new SwatCorpseMenu(containerId, playerInventory, this);
    }
}
