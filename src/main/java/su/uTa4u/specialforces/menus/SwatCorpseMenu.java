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

    private final Container container;

    // Client side constructor
    public SwatCorpseMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(SwatEntity.SWAT_INVENTORY_SIZE));
    }

    // Server side constructor
    public SwatCorpseMenu(int containerId, Inventory playerInventory, Container container) {
        super(ModMenuTypes.SWAT_CORPSE.get(), containerId);
        this.container = container;

        // Add player slots
        int slot = 0;
        int i;
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

        // Add swat slots
        for (i = 0; i < 9; ++i) {
            // Swat hotbar 36 - 44
            this.addSlot(new Slot(container, slot++, 8 + i * 18, 100));
        }
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                // Swat inventory 45 - 71
                this.addSlot(new Slot(container, slot++, 8 + j * 18, 42 + i * 18));
            }
        }

        for (i = 0; i < 4; ++i) {
            // Swat armor 72 - 75
            final EquipmentSlot equipmentslot = ARMOR_SLOTS[i];
            this.addSlot(new Slot(container, slot++, 8 + i * 18, 16) {
                @Override
                public boolean mayPlace(@NotNull ItemStack itemStack) {
                    return itemStack.canEquip(equipmentslot, null);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                    return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_ARMOR_SLOTS[equipmentslot.getIndex()]);
                }
            });
        }

        // Swat offhand 76
        this.addSlot(new Slot(container, slot, 98, 16) {
            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int i) {
        // https://docs.minecraftforge.net/en/1.20.1/gui/menus/#:~:text=Across%20Minecraft%20implementations%2C%20this%20method%20is%20fairly%20consistent%20in%20its%20logic%3A
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.container.stillValid(player);
    }
}
