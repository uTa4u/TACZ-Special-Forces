package su.uTa4u.specialforces.client.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import su.uTa4u.specialforces.Util;
import su.uTa4u.specialforces.menus.SwatCorpseMenu;

public class SwatCorpseScreen extends AbstractContainerScreen<SwatCorpseMenu> {
    private static final ResourceLocation TEXTURE = Util.getResource("textures/gui/swat_corpse_inv.png");

    public SwatCorpseScreen(SwatCorpseMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 216;
        this.inventoryLabelX = 7;
        this.inventoryLabelY = 119;
        this.titleLabelX = 7;
        this.titleLabelY = 5;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        this.renderBackground(guiGraphics);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }
}
