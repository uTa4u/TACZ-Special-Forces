package su.uTa4u.specialforces.entities;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.entity.shooter.*;
import com.tacz.guns.entity.sync.ModSyncedEntityData;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.EffectiveRangeModifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.uTa4u.specialforces.Mission;
import su.uTa4u.specialforces.ModTags;
import su.uTa4u.specialforces.Specialty;
import su.uTa4u.specialforces.Util;
import su.uTa4u.specialforces.config.CommonConfig;
import su.uTa4u.specialforces.entities.goals.GunAttackGoal;
import su.uTa4u.specialforces.entities.goals.GunAttackPosGoal;
import su.uTa4u.specialforces.entities.goals.PotionUseGoal;
import su.uTa4u.specialforces.entities.goals.RetreatGoal;
import su.uTa4u.specialforces.menus.SwatCorpseMenu;

import java.util.*;
import java.util.function.Supplier;

public class SwatEntity extends PathfinderMob implements IGunOperator, Container, MenuProvider {
    // private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NBT_KEY_DEAD_BODE_AGE = "DeadBodyAge";
    private static final String NBT_KEY_MISSION = "Mission";
    private static final String NBT_KEY_SQUAD = "Squad";
    private static final String NBT_KEY_SPECIALTY = "Specialty";
    private static final String NBT_KEY_STATE = "State";
    private static final String NBT_KEY_INVENTORY = "Inventory";
    private static final String NBT_KEY_SELECTED = "Selected";
    private static final String NBT_KEY_SQUAD_SUMMON_TIMER = "SquadTimer";
    private static final String NBT_KEY_FAILED_GUN_POS_COUNTER = "GunPosCounter";

    private static final EntityDimensions BOX_DIMENSIONS = EntityDimensions.scalable(0.6f, 0.6f);
    private static final EntityDataAccessor<Specialty> SPECIALTY = SynchedEntityData.defineId(SwatEntity.class, ModEntityDataSerializers.SPECIAL_FORCE_SPECIALTY);
    private static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(SwatEntity.class, EntityDataSerializers.BYTE);
    public static final byte STATE_ALIVE = 0;
    public static final byte STATE_DOWN = 1;
    public static final byte STATE_DEAD = 2;

    // The order by type in which guns should be used
    private static final List<String> GUN_TYPE_ORDER = List.of(Util.getGunTabTypeName(GunTabType.SNIPER), Util.getGunTabTypeName(GunTabType.RPG), Util.getGunTabTypeName(GunTabType.MG), Util.getGunTabTypeName(GunTabType.RIFLE), Util.getGunTabTypeName(GunTabType.SHOTGUN), Util.getGunTabTypeName(GunTabType.SMG), Util.getGunTabTypeName(GunTabType.PISTOL));

    // TODO: this should be a percentage value of max health depending on Specialty
    private static final float DOWN_HEALTH_THRESHOLD = 20.0f;

    private MeleeAttackGoal meleeAttackGoal;

    private int squadSummonTimer = 0;
    private short deadBodyAge = 0;
    private float currentGunAttackRadiusSqr;
    private int failedGunPosCounter = 0;

    // Only commanders have these field set to non-null
    @Nullable
    private Mission mission = null;
    @Nullable
    private List<SwatEntity> squad = null;

    protected SwatEntity(EntityType<SwatEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static SwatEntity create(Level level, Specialty specialty, Mission mission) {
        SwatEntity entity = new SwatEntity(ModEntities.SWAT_ENTITY.get(), level);
        entity.setSpecialty(specialty);
        entity.mission = mission;
        entity.squad = new ArrayList<>();
        return entity;
    }

    public static SwatEntity create(Level level, Specialty specialty) {
        return create(level, specialty, null);
    }

    @Nullable
    @Override
    @SuppressWarnings("deprecation")
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor levelAccessor, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType spawnType, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        if (spawnType == MobSpawnType.SPAWNER || spawnType == MobSpawnType.SPAWN_EGG) {
            this.setSpecialty(Specialty.getRandomSpecialty());
//            LOGGER.info("Random Spec: " + this.getSpecialty());
        }

        // TODO: give all loottables pool names
        this.generateInventory();

        this.copySpecialAttributes();

        return spawnData;
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        this.takeNextGun();
    }

    @NotNull
    @Override
    protected InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        Level level = player.level();

        // For debug purposes
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
            this.goalSelector.enableControlFlag(Goal.Flag.MOVE);
            this.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
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
            // TODO: do something to stop it from moving completely
            // Why is this not enough?
            this.goalSelector.disableControlFlag(Goal.Flag.MOVE);
            // Whatever, this WILL be enough
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MobEffectInstance.INFINITE_DURATION, Short.MAX_VALUE));

            if (hpAfterDmg <= 0.0f) {
                // Swat Entity is dead. Can't attack, move or be moved.
                // Will despawn after some time, will despawn faster if inventory is empty.
                this.setState(STATE_DEAD);
                this.setInvulnerable(true);
                this.setHealth(1.0f);
                this.removeFreeWill();
                this.removeAllEffects();
                this.deadBodyAge = 0;
                // TODO: Maybe remove random items from inventory
                return;
            }
        }

        super.actuallyHurt(damageSource, damageAmount);
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity target = this.getTarget();
        if (this.tacz$data.isCrawling && (target == null || target.isDeadOrDying() || this.getState() != STATE_ALIVE)) {
            this.crawl(false);
        }

        if (!this.level().isClientSide) {
            this.taczTick();

            ServerLevel serverLevel = (ServerLevel) this.level();

            // Check if dead body should despawn
            if (this.getState() == STATE_DEAD) {
                this.deadBodyAge += 1;
                if (this.deadBodyAge >= CommonConfig.SWAT_ENTITY_DEAD_BODY_LIFESPAN.get()) {
                    this.discard();
                }
            }

            if (this.getSpecialty() == Specialty.COMMANDER && this.squad != null) {
                // Clear removed or dead swat entities
                this.squad.removeIf((swat) -> swat.isRemoved() || swat.getState() == STATE_DEAD);

                // Summon squad members
                this.squadSummonTimer += 1;
                if (this.mission != null && this.squadSummonTimer >= CommonConfig.SWAT_ENTITY_SQUAD_SUMMON_COOLDOWN.get()) {
                    this.squadSummonTimer = 0;

                    // Get specialties which are in squad at the moment
                    List<Specialty> existingSpecs = this.squad.stream().map(SwatEntity::getSpecialty).toList();
                    // Get specialties which are supposed to be in squad
                    List<Specialty> totalSpecs = this.mission.getParticipants();
                    // Get specialties which are missing in squad
                    List<Specialty> missingSpecs = new ArrayList<>(totalSpecs);
                    missingSpecs.removeAll(existingSpecs);

                    Vec3 pos = this.position();
                    for (Specialty spec : missingSpecs) {
                        SwatEntity squadMember = SwatEntity.create(serverLevel, spec);
                        squadMember.setPos(pos);
                        squadMember.setTarget(target);
                        ForgeEventFactory.onFinalizeSpawn(squadMember, serverLevel, serverLevel.getCurrentDifficultyAt(BlockPos.containing(squadMember.position())), MobSpawnType.MOB_SUMMONED, null, null);
                        if (serverLevel.addFreshEntity(squadMember)) {
                            this.squad.add(squadMember);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);

        nbt.putInt(NBT_KEY_FAILED_GUN_POS_COUNTER, this.failedGunPosCounter);

        nbt.putInt(NBT_KEY_SQUAD_SUMMON_TIMER, this.squadSummonTimer);

        nbt.putShort(NBT_KEY_DEAD_BODE_AGE, this.deadBodyAge);

        if (this.mission != null) {
            nbt.putString(NBT_KEY_MISSION, this.mission.getName());
        }

        if (this.squad != null) {
            ListTag squadTag = new ListTag();
            this.squad.forEach(swatEntity -> squadTag.add(NbtUtils.createUUID(swatEntity.getUUID())));
            nbt.put(NBT_KEY_SQUAD, squadTag);
        }

        nbt.putString(NBT_KEY_SPECIALTY, this.getSpecialty().getName());
        nbt.putByte(NBT_KEY_STATE, this.getState());

        nbt.put(NBT_KEY_INVENTORY, this.saveCompartments(new ListTag()));
        nbt.putInt(NBT_KEY_SELECTED, this.selected);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

        this.failedGunPosCounter = nbt.getInt(NBT_KEY_FAILED_GUN_POS_COUNTER);

        this.squadSummonTimer = nbt.getInt(NBT_KEY_SQUAD_SUMMON_TIMER);

        this.deadBodyAge = nbt.getShort(NBT_KEY_DEAD_BODE_AGE);

        if (nbt.contains(NBT_KEY_MISSION)) {
            Mission mission = Mission.byName(nbt.getString(NBT_KEY_MISSION));
            if (mission != null) this.mission = mission;
        }

        if (this.squad != null && this.level() instanceof ServerLevel serverLevel) {
            Tag tag = nbt.get(NBT_KEY_SQUAD);
            if (tag instanceof ListTag squadTag && !squadTag.isEmpty()) {
                for (Tag t : squadTag) {
                    Entity entity = serverLevel.getEntity(NbtUtils.loadUUID(t));
                    if (entity instanceof SwatEntity swatEntity) {
                        this.squad.add(swatEntity);
                    }
                }
            }
        }

        if (nbt.contains(NBT_KEY_SPECIALTY)) {
            Specialty spec = Specialty.byName(nbt.getString(NBT_KEY_SPECIALTY));
            if (spec != null) this.setSpecialty(spec);
        }

        if (nbt.contains(NBT_KEY_STATE)) this.setState(nbt.getByte(NBT_KEY_STATE));

        this.loadCompartments(nbt.getList(NBT_KEY_INVENTORY, 10));
        this.selected = nbt.getInt(NBT_KEY_SELECTED);
    }

    // TODO: Need to register special and mission goals here, since the order in which they are registered matters
    //  however it is not actually possible since when this method is run
    //  specialty is unset (set to Specialty.Commander by default) so is the mission.
    @Override
    protected void registerGoals() {
        this.meleeAttackGoal = new MeleeAttackGoal(this, 1.0f, true);

        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RetreatGoal(this));
        this.goalSelector.addGoal(3, new PotionUseGoal(this));
        this.goalSelector.addGoal(4, new GunAttackGoal(this));
        this.goalSelector.addGoal(5, new GunAttackPosGoal(this));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        if (GoalUtils.hasGroundPathNavigation(this)) {
            ((GroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
            this.goalSelector.addGoal(1, new BreakDoorGoal(this, 120, (d) -> true));
        }

        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this, SwatEntity.class).setAlertOthers()));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // TODO: Use potions like how Witch does
    }

    public boolean hasMeleeAttackGoal() {
        return this.goalSelector.getAvailableGoals().stream()
                .anyMatch((wrap) -> wrap.getGoal() == this.meleeAttackGoal);
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

        ObjectArrayList<ItemStack> itemStacks = lootTable.getRandomItems(lootParams);
        List<Integer> indices = this.getAvailableInvSlotsShuffled();
        Util.shuffleAndSplitItems(itemStacks, indices.size(), this.random);

        for (int i = itemStacks.size() - 1; i >= 0; --i) {
            ItemStack itemStack = itemStacks.remove(i);
            // Place certain items in certain places
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
            } else if (!itemStack.isEmpty()) {
                // The rest is placed in the random places
                this.items.set(indices.remove(indices.size() - 1), itemStack);
            }
        }
    }

    public void takeNextGun() {
        List<ItemStack> potentialNextGuns = new ArrayList<>();
        Map<ItemStack, CommonGunIndex> potentialNextGunIndexes = new HashMap<>();
        Map<ItemStack, Integer> potentialNextGunIndices = new HashMap<>();

        for (int i = 0; i < this.items.size(); ++i) {
            ItemStack gunItemStack = this.items.get(i);
            CompoundTag nbt = gunItemStack.getOrCreateTag();
            if (!nbt.contains("GunId")) continue;

            ResourceLocation gunId = ResourceLocation.tryParse(nbt.getString("GunId"));
            if (gunId == null) continue;

            Optional<CommonGunIndex> gunIndexOpt = TimelessAPI.getCommonGunIndex(gunId);
            if (gunIndexOpt.isEmpty()) continue;

            if (this.hasAmmoForGun(gunItemStack)) {
                CommonGunIndex gunIndex = gunIndexOpt.get();
                potentialNextGuns.add(gunItemStack);
                potentialNextGunIndexes.put(gunItemStack, gunIndex);
                potentialNextGunIndices.put(gunItemStack, i);
            }
        }

        // Out of guns... Let's go with fists
        if (potentialNextGuns.isEmpty()) {
            // TODO: remove GunAttackGoal and add MeleeAttackGoal instead
            return;
        }

        potentialNextGuns.sort((itemStack1, itemStack2) -> {
            CommonGunIndex gunIndex1 = potentialNextGunIndexes.get(itemStack1);
            CommonGunIndex gunIndex2 = potentialNextGunIndexes.get(itemStack2);
            int index1 = GUN_TYPE_ORDER.indexOf(gunIndex1.getType());
            int index2 = GUN_TYPE_ORDER.indexOf(gunIndex2.getType());
            return index1 - index2;
        });

        ItemStack nextGun = potentialNextGuns.get(0);

        // If nextGun is not on hotbar, we swap it there
        int index = potentialNextGunIndices.get(nextGun);
        if (index > HOTBAR_INDEX_END) {
            int hotbarIndex = this.getFreeHotbarIndex();
            if (hotbarIndex == -1) hotbarIndex = this.random.nextInt(HOTBAR_INDEX_END + 1);
            this.swapItems(index, hotbarIndex);
            index = hotbarIndex;
        }
        this.selected = index;

        this.currentGunAttackRadiusSqr = 0.0f;
        AttachmentCacheProperty prop = new AttachmentCacheProperty();
        prop.eval(nextGun, potentialNextGunIndexes.get(nextGun).getGunData());
        this.updateCacheProperty(prop);
        // This is sus ngl
        this.tacz$data.currentGunItem = () -> nextGun;
        float effectiveRange = prop.getCache(EffectiveRangeModifier.ID);
        this.currentGunAttackRadiusSqr = (float) (effectiveRange * effectiveRange * CommonConfig.SWAT_ENTITY_EFFECTIVE_RANGE_MULT.get() * CommonConfig.SWAT_ENTITY_EFFECTIVE_RANGE_MULT.get());
    }

    @NotNull
    @Override
    public ItemStack getItemBySlot(@NotNull EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.getSelectedItem();
        } else if (slot == EquipmentSlot.OFFHAND) {
            return this.offhand.get(0);
        } else if (slot.isArmor()) {
            return this.armor.get(3 - slot.getIndex());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(@NotNull EquipmentSlot slot, @NotNull ItemStack itemStack) {
        this.verifyEquippedItem(itemStack);
        if (slot == EquipmentSlot.MAINHAND) {
            this.onEquipItem(slot, this.items.set(this.selected, itemStack), itemStack);
        } else if (slot == EquipmentSlot.OFFHAND) {
            this.onEquipItem(slot, this.offhand.set(0, itemStack), itemStack);
        } else if (slot.getType() == EquipmentSlot.Type.ARMOR) {
            this.onEquipItem(slot, this.armor.set(3 - slot.getIndex(), itemStack), itemStack);
        }
    }

    @NotNull
    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return this.armor;
    }

    @NotNull
    @Override
    public Iterable<ItemStack> getHandSlots() {
        return Lists.newArrayList(this.getMainHandItem(), this.getOffhandItem());
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
        return this.getSpecialty().getTypeName();
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

    public boolean hasMission() {
        return this.mission != null;
    }

    public boolean hasSquad() {
        return this.squad != null;
    }

    public void summonSquadNextTick() {
        this.squadSummonTimer = CommonConfig.SWAT_ENTITY_SQUAD_SUMMON_COOLDOWN.get();
    }

    public void incFailedGunPosCounter() {
        this.failedGunPosCounter += 1;
    }

    public void resetFailedGunPosCounter() {
        this.failedGunPosCounter = 0;
    }

    public int getFailedGunPosCounter() {
        return this.failedGunPosCounter;
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

    // Adapted from AbstractMinecartContainer, ContainerEntity and Inventory

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
    private int selected;

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

    public void setSelected(int index) {
        this.selected = index;
    }

    public ItemStack getSelectedItem() {
        if (HOTBAR_INDEX_START <= this.selected && this.selected <= HOTBAR_INDEX_END) {
            return this.items.get(this.selected);
        } else {
            return ItemStack.EMPTY;
        }
    }

    @SuppressWarnings("unused")
    private int getFreeInvIndex() {
        for (int i = INV_INDEX_START; i <= INV_INDEX_END; ++i) {
            if (this.items.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private ListTag saveCompartments(ListTag listTag) {
        int index;
        CompoundTag slot;
        ItemStack itemStack;
        for (index = 0; index < this.items.size(); ++index) {
            itemStack = this.items.get(index);
            if (!itemStack.isEmpty()) {
                slot = new CompoundTag();
                slot.putByte("slot", (byte) index);
                itemStack.save(slot);
                listTag.add(slot);
            }
        }
        for (index = 0; index < this.armor.size(); ++index) {
            itemStack = this.armor.get(index);
            if (!itemStack.isEmpty()) {
                slot = new CompoundTag();
                slot.putByte("slot", (byte) (index + 100));
                itemStack.save(slot);
                listTag.add(slot);
            }
        }
        for (index = 0; index < this.offhand.size(); ++index) {
            itemStack = this.offhand.get(index);
            if (!itemStack.isEmpty()) {
                slot = new CompoundTag();
                slot.putByte("slot", (byte) (index + 150));
                itemStack.save(slot);
                listTag.add(slot);
            }
        }

        return listTag;
    }

    private void loadCompartments(ListTag listTag) {
        this.items.clear();
        this.armor.clear();
        this.offhand.clear();
        for (int i = 0; i < listTag.size(); ++i) {
            CompoundTag slot = listTag.getCompound(i);
            int index = slot.getByte("slot") & 255;
            ItemStack itemStack = ItemStack.of(slot);
            if (!itemStack.isEmpty()) {
                // 0 <= index is always true
                if (index < this.items.size()) {
                    this.items.set(index, itemStack);
                } else if (100 <= index && index < this.armor.size() + 100) {
                    this.armor.set(index - 100, itemStack);
                } else if (150 <= index && index < this.offhand.size() + 150) {
                    this.offhand.set(index - 150, itemStack);
                }
            }
        }
    }

    private List<Integer> getAvailableInvSlotsShuffled() {
        ObjectArrayList<Integer> indices = new ObjectArrayList<>();
        for (int i = INV_INDEX_START; i <= INV_INDEX_END; ++i) {
            if (this.items.get(i).isEmpty()) {
                indices.add(i);
            }
        }
        net.minecraft.Util.shuffle(indices, this.random);
        return indices;
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

    public void swapItems(int index1, int index2) {
        if (index1 < 0 || index1 >= this.items.size()) return;
        if (index2 < 0 || index2 >= this.items.size()) return;
        ItemStack temp = this.items.get(index1);
        this.items.set(index1, this.items.get(index2));
        this.items.set(index2, temp);
    }

    // Adapted from AbstractGunItem#canReload
    public boolean hasAmmoForGun(ItemStack gunItemStack) {
        for (ItemStack ammoItemStack : this.items) {
            Item ammoItem = ammoItemStack.getItem();
            if (ammoItem instanceof IAmmo iAmmo) {
                if (iAmmo.isAmmoOfGun(gunItemStack, ammoItemStack)) {
                    return true;
                }
            }
            if (ammoItem instanceof IAmmoBox iAmmoBox) {
                if (iAmmoBox.isAllTypeCreative(ammoItemStack) || iAmmoBox.isCreative(ammoItemStack)) {
                    return true;
                }
                if (iAmmoBox.isAmmoBoxOfGun(gunItemStack, ammoItemStack)) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Integer> getIndicesWithItem(Item item) {
        List<Integer> indices = new ArrayList<>();
        for (int i = HOTBAR_INDEX_START; i <= INV_INDEX_END; ++i) {
            if (this.items.get(i).is(item)) {
                indices.add(i);
            }
        }
        return indices;
    }

    // Common code for finding NonNullList (items / armor / offhand) by slot index
    @Nullable
    private NonNullList<ItemStack> getCompartment(int index) {
        NonNullList<ItemStack> ret = null;
        NonNullList<ItemStack> compartment;
        for (Iterator<NonNullList<ItemStack>> iterator = this.compartments.iterator(); iterator.hasNext(); index -= compartment.size()) {
            compartment = iterator.next();
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
