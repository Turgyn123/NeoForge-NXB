package net.narutoxboruto.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.narutoxboruto.attachments.MainAttachment;
import net.narutoxboruto.attachments.jutsus.JutsuStorage;
import net.narutoxboruto.items.jutsus.AbstractJutsuItem;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Container menu for the Jutsu Storage - like an ender chest but for jutsus only.
 * 54 slots (6 rows of 9), only accepts AbstractJutsuItem instances.
 */
public class JutsuStorageMenu extends AbstractContainerMenu {

    private static final int STORAGE_ROWS = 6;
    private static final int STORAGE_COLS = 9;
    private static final int STORAGE_SIZE = STORAGE_ROWS * STORAGE_COLS; // 54 slots

    private final ItemStackHandler storageHandler;
    private final Player player;

    // Client constructor (called from network)
    public JutsuStorageMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new ItemStackHandler(STORAGE_SIZE));
    }

    // Server constructor
    public JutsuStorageMenu(int containerId, Inventory playerInventory, ItemStackHandler storageHandler) {
        super(ModMenuTypes.JUTSU_STORAGE.get(), containerId);
        this.player = playerInventory.player;
        this.storageHandler = storageHandler;

        // Add jutsu storage slots (6 rows of 9)
        for (int row = 0; row < STORAGE_ROWS; row++) {
            for (int col = 0; col < STORAGE_COLS; col++) {
                int slotIndex = col + row * STORAGE_COLS;
                int x = 8 + col * 18;
                int y = 18 + row * 18;
                this.addSlot(new JutsuSlot(storageHandler, slotIndex, x, y));
            }
        }

        // Add player inventory slots (3 rows of 9)
        int inventoryStartY = 140;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = 8 + col * 18;
                int y = inventoryStartY + row * 18;
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, x, y));
            }
        }

        // Add player hotbar slots
        int hotbarY = inventoryStartY + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
        }
    }

    /**
     * Custom slot that only accepts jutsu items.
     */
    private static class JutsuSlot extends SlotItemHandler {
        public JutsuSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof AbstractJutsuItem;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            // If clicking in storage area (0-53)
            if (index < STORAGE_SIZE) {
                // Move to player inventory
                if (!this.moveItemStackTo(stackInSlot, STORAGE_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Clicking in player inventory - move to storage (only if it's a jutsu item)
                if (stackInSlot.getItem() instanceof AbstractJutsuItem) {
                    if (!this.moveItemStackTo(stackInSlot, 0, STORAGE_SIZE, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY; // Can't move non-jutsu items to storage
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // Always valid as long as menu is open
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Save storage back to player data when menu closes
        if (!player.level().isClientSide()) {
            JutsuStorage storage = player.getData(MainAttachment.JUTSU_STORAGE);
            for (int i = 0; i < STORAGE_SIZE; i++) {
                storage.setItem(i, storageHandler.getStackInSlot(i));
            }
            player.setData(MainAttachment.JUTSU_STORAGE, storage);
        }
    }

    public ItemStackHandler getStorageHandler() {
        return storageHandler;
    }
    
    // Pagination support - currently showing all 54 slots on one page
    // This can be expanded later when more jutsus are added
    private int currentPage = 0;
    private static final int SLOTS_PER_PAGE = 54;
    
    public int getMaxPages() {
        // For now, always 1 page. Can be expanded based on total jutsu count
        // return Math.max(1, (int) Math.ceil((double) getTotalJutsuCount() / SLOTS_PER_PAGE));
        return 1;
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(int page) {
        this.currentPage = Math.max(0, Math.min(page, getMaxPages() - 1));
    }
}
