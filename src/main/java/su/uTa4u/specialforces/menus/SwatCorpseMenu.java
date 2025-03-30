package su.uTa4u.specialforces.menus;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.uTa4u.specialforces.entities.SwatEntity;

public class SwatCorpseMenu extends AbstractContainerMenu {
    private static final ResourceLocation[] EMPTY_ARMOR_SLOTS = new ResourceLocation[]{InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET};
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static final int PLAYER_SLOTS_START = 41;
    private static final int PLAYER_SLOTS_END = 76;

    private final Container container;

    // Client side constructor
    public SwatCorpseMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(SwatEntity.SWAT_CONTAINER_SIZE));
    }

    // Server side constructor
    public SwatCorpseMenu(int containerId, Inventory playerInventory, Container container) {
        super(ModMenuTypes.SWAT_CORPSE.get(), containerId);
        this.container = container;

        int slot = 0;
        int i;
        // Add swat slots
        for (i = 0; i < 9; ++i) {
            // Swat hotbar 0 - 8
            this.addSlot(new Slot(container, slot++, 8 + i * 18, 100) {
                @Override
                public boolean mayPlace(@NotNull ItemStack itemStack) {
                    return false;
                }
            });
        }
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                // Swat inventory 9 - 35
                this.addSlot(new Slot(container, slot++, 8 + j * 18, 42 + i * 18) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack itemStack) {
                        return false;
                    }
                });
            }
        }

        for (i = 0; i < 4; ++i) {
            // Swat armor 36 - 39
            final EquipmentSlot equipmentslot = ARMOR_SLOTS[i];
            this.addSlot(new Slot(container, slot++, 8 + i * 18, 16) {
                @Override
                public boolean mayPlace(@NotNull ItemStack itemStack) {
                    return false;
                }

                @Override
                public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                    return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_ARMOR_SLOTS[equipmentslot.getIndex()]);
                }
            });
        }

        // Swat offhand 40
        this.addSlot(new Slot(container, slot++, 98, 16) {
            @Override
            public boolean mayPlace(@NotNull ItemStack itemStack) {
                return false;
            }

            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });

        // Add player slots
        slot = 0;
        for (i = 0; i < 9; ++i) {
            // Player hotbar 0 - 8
            this.addSlot(new Slot(playerInventory, slot++, 8 + i * 18, 188));
        }
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                // Player inventory 9 - 35
                this.addSlot(new Slot(playerInventory, slot++, 8 + j * 18, 130 + i * 18));
            }
        }

    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack quickMovedStack = ItemStack.EMPTY;
        Slot quickMovedSlot = this.slots.get(index);
        if (quickMovedSlot.hasItem()) {
            ItemStack rawStack = quickMovedSlot.getItem();
            quickMovedStack = rawStack.copy();
            if (quickMovedSlot.container == this.container) {
                // Moving from container to player inventory
                if (!this.moveItemStackTo(rawStack, PLAYER_SLOTS_START, PLAYER_SLOTS_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to container is disabled
                return ItemStack.EMPTY;
            }
        }
        return quickMovedStack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.container.stillValid(player);
    }
}
