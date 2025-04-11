package su.uTa4u.specialforces.entities.goals;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import su.uTa4u.specialforces.Specialty;
import su.uTa4u.specialforces.entities.SwatEntity;

import java.util.*;
import java.util.function.Predicate;

public class PotionUseGoal extends Goal {
    private static final UUID SPEED_MODIFIER_DRINKING_UUID = UUID.fromString("5CD17E52-A79A-43D3-A529-90FDE04B181E");
    private static final AttributeModifier SPEED_MODIFIER_DRINKING = new AttributeModifier(SPEED_MODIFIER_DRINKING_UUID, "Drinking speed penalty", -0.25, AttributeModifier.Operation.ADDITION);

    private static final Map<Predicate<SwatEntity>, MobEffect> POSSIBLE_EFFECTS = new HashMap<>();

    private final SwatEntity swat;
    private int potionIndex = -1;

    public PotionUseGoal(SwatEntity swat) {
        this.swat = swat;
    }

    // TODO: compress this into a Map<Predicate<SwatEntity>, MobEffect> or smth idk
    @Override
    public boolean canUse() {
        if (this.swat.getState() == SwatEntity.STATE_DEAD) return false;
        List<Integer> indices = this.swat.getIndicesWithItem(Items.POTION);
        if (indices.isEmpty()) return false;
        if (this.swat.getHealth() < this.swat.getMaxHealth() / 2.0f) {
            int index = this.findPotionIndex(indices, MobEffects.HEAL);
            if (index == -1) {
                return false;
            } else {
                this.potionIndex = index;
                return true;
            }
        } else if (this.swat.getHealth() < this.swat.getMaxHealth() * 3.0f / 4.0f) {
            int index = this.findPotionIndex(indices, MobEffects.REGENERATION);
            if (index == -1) {
                return false;
            } else {
                this.potionIndex = index;
                return true;
            }
        } else if (this.swat.getHealth() == this.swat.getMaxHealth()) {
            int index = this.findPotionIndex(indices, MobEffects.HEALTH_BOOST);
            if (index == -1) {
                return false;
            } else {
                this.potionIndex = index;
                return true;
            }
        } else if (this.swat.isOnFire()) {
            int index = this.findPotionIndex(indices, MobEffects.FIRE_RESISTANCE);
            if (index == -1) {
                return false;
            } else {
                this.potionIndex = index;
                return true;
            }
        } else if (this.swat.level().isNight()) {
            int index = this.findPotionIndex(indices, MobEffects.NIGHT_VISION);
            if (index == -1) {
                return false;
            } else {
                this.potionIndex = index;
                return true;
            }
        } else if (this.swat.isUnderWater()) {
            int index = this.findPotionIndex(indices, MobEffects.WATER_BREATHING);
            if (index == -1) {
                return false;
            } else {
                this.potionIndex = index;
                return true;
            }
        } else if (this.swat.getSpecialty() == Specialty.SPY) {
            int index = this.findPotionIndex(indices, MobEffects.INVISIBILITY);
            if (index == -1) {
                return false;
            } else {
                this.potionIndex = index;
                return true;
            }
        } else if (this.swat.getSpecialty() == Specialty.BULLDOZER) {
            int index = this.findPotionIndex(indices, MobEffects.DAMAGE_RESISTANCE);
            if (index == -1) {
                return false;
            } else {
                this.potionIndex = index;
                return true;
            }
        } else if (this.swat.getSpecialty() == Specialty.SCOUT) {
            int index = this.findPotionIndex(indices, MobEffects.JUMP);
            if (index == -1) {
                return false;
            } else {
                this.potionIndex = index;
                return true;
            }
        } else if (this.swat.hasMeleeAttackGoal()) {
            int index = this.findPotionIndex(indices, MobEffects.DAMAGE_BOOST);
            if (index == -1) {
                return false;
            } else {
                this.potionIndex = index;
                return true;
            }
        } else if (this.swat.fallDistance > 10.0f) {
            int index = this.findPotionIndex(indices, MobEffects.SLOW_FALLING);
            if (index == -1) {
                return false;
            } else {
                this.potionIndex = index;
                return true;
            }
        } else if (this.swat.getSpecialty() == Specialty.MEDIC) {
            int index = this.findPotionIndex(indices, MobEffects.MOVEMENT_SPEED);
            if (index == -1) {
                return false;
            } else {
                this.potionIndex = index;
                return true;
            }
        } else if (this.swat.getSpecialty() == Specialty.ENGINEER) {
            int index = this.findPotionIndex(indices, MobEffects.DIG_SPEED);
            if (index == -1) {
                return false;
            } else {
                this.potionIndex = index;
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.swat.getUseItemRemainingTicks() > 0;
    }

    @Override
    public void start() {
        this.swat.setSelected(this.potionIndex);
        if (!this.swat.isSilent()) {
            this.swat.level().playSound(null, this.swat.getX(), this.swat.getY(), this.swat.getZ(), SoundEvents.WITCH_DRINK, this.swat.getSoundSource(), 1.0f, 0.8f + this.swat.getRandom().nextFloat() * 0.4f);
        }
        AttributeInstance attr = this.swat.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) {
            attr.removeModifier(SPEED_MODIFIER_DRINKING);
            attr.addTransientModifier(SPEED_MODIFIER_DRINKING);
        }
        this.swat.startUsingItem(InteractionHand.MAIN_HAND);
    }

    @Override
    public void stop() {
        this.potionIndex = -1;
        AttributeInstance attr = this.swat.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) {
            attr.removeModifier(SPEED_MODIFIER_DRINKING);
        }
        this.swat.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GLASS_BOTTLE));
        this.swat.takeNextGun();
    }

    @Override
    public void tick() {
//        if (this.usingTime-- <= 0) {
//            ItemStack potion = this.swat.getMainHandItem();
//            this.swat.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
//            if (potion.is(Items.POTION)) {
//                for (MobEffectInstance effect : PotionUtils.getMobEffects(potion)) {
//                    this.swat.addEffect(new MobEffectInstance(effect));
//                }
//            }
//            AttributeInstance attr = this.swat.getAttribute(Attributes.MOVEMENT_SPEED);
//            if (attr != null) {
//                attr.removeModifier(SPEED_MODIFIER_DRINKING);
//            }
//        }
    }

    private int findPotionIndex(List<Integer> indices, MobEffect effect) {
        List<Integer> res = new ArrayList<>();
        for (int i : indices) {
            if (this.hasEffect(PotionUtils.getPotion(this.swat.getItem(i)), effect)) {
                res.add(i);
            }
        }
        if (res.isEmpty()) return -1;
        return res.get(this.swat.getRandom().nextInt(res.size()));
    }

    private boolean hasEffect(Potion potion, MobEffect effect) {
        for (MobEffectInstance e : potion.getEffects()) {
            if (e.getEffect() == effect) {
                return true;
            }
        }
        return false;
    }

    static {
        POSSIBLE_EFFECTS.put((swat) -> swat.getHealth() < swat.getMaxHealth() / 2.0f, MobEffects.HEAL);
        POSSIBLE_EFFECTS.put((swat) -> swat.getHealth() < swat.getMaxHealth() * 3.0f / 4.0f, MobEffects.REGENERATION);
        POSSIBLE_EFFECTS.put((swat) -> swat.getHealth() == swat.getMaxHealth(), MobEffects.HEALTH_BOOST);
        POSSIBLE_EFFECTS.put((swat) -> swat.isOnFire(), MobEffects.FIRE_RESISTANCE);
        POSSIBLE_EFFECTS.put((swat) -> swat.isUnderWater(), MobEffects.WATER_BREATHING);
        POSSIBLE_EFFECTS.put((swat) -> swat.fallDistance > 10.0f, MobEffects.SLOW_FALLING);
        POSSIBLE_EFFECTS.put((swat) -> swat.level().isNight(), MobEffects.NIGHT_VISION);
        POSSIBLE_EFFECTS.put((swat) -> swat.hasMeleeAttackGoal(), MobEffects.DAMAGE_BOOST);
        POSSIBLE_EFFECTS.put((swat) -> swat.getSpecialty() == Specialty.SPY, MobEffects.INVISIBILITY);
        POSSIBLE_EFFECTS.put((swat) -> swat.getSpecialty() == Specialty.BULLDOZER, MobEffects.DAMAGE_RESISTANCE);
        POSSIBLE_EFFECTS.put((swat) -> swat.getSpecialty() == Specialty.SCOUT, MobEffects.JUMP);
        POSSIBLE_EFFECTS.put((swat) -> swat.getSpecialty() == Specialty.MEDIC, MobEffects.MOVEMENT_SPEED);
        POSSIBLE_EFFECTS.put((swat) -> swat.getSpecialty() == Specialty.ENGINEER, MobEffects.DIG_SPEED);
    }
}
