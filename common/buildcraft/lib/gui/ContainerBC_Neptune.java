/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import buildcraft.api.core.BCDebugging;
import buildcraft.api.core.BCLog;

import buildcraft.lib.gui.slot.IPhantomSlot;
import buildcraft.lib.gui.slot.SlotPhantom;
import buildcraft.lib.misc.StackUtil;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.net.IPayloadWriter;
import buildcraft.lib.net.MessageContainer;
import buildcraft.lib.net.MessageManager;
import buildcraft.lib.net.PacketBufferBC;
import buildcraft.lib.tile.item.IItemHandlerAdv;

public abstract class ContainerBC_Neptune extends Container {
    public static final boolean DEBUG = BCDebugging.shouldDebugLog("lib.container");

    protected static final IdAllocator IDS = new IdAllocator("container");
    /** Generic "data" id. Used by all containers which only have 1 id to write out (no point in making EVERY container
     * have an {@link IdAllocator} if they only allocate one. */
    public static final int NET_DATA = IDS.allocId("DATA");
    public static final int NET_WIDGET = IDS.allocId("WIDGET");
    public static final int NET_SET_PHANTOM = IDS.allocId("SET_PHANTOM");
    public static final int NET_SET_PHANTOM_MULTI = IDS.allocId("NET_SET_PHANTOM_MULTI");

    public final EntityPlayer player;
    private final List<Widget_Neptune<?>> widgets = new ArrayList<>();

    public ContainerBC_Neptune(EntityPlayer player) {
        this.player = player;
    }

    /** @return The {@link IdAllocator} that allocates all ID's for this class, and its parent classes. All subclasses
     *         should override this if they allocate their own ids after calling
     *         {@link IdAllocator#makeChild(String)} */
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    protected void addFullPlayerInventory(int startX, int startY) {
        for (int sy = 0; sy < 3; sy++) {
            for (int sx = 0; sx < 9; sx++) {
                addSlotToContainer(new Slot(player.inventory, sx + sy * 9 + 9, startX + sx * 18, startY + sy * 18));
            }
        }

        for (int sx = 0; sx < 9; sx++) {
            addSlotToContainer(new Slot(player.inventory, sx, startX + sx * 18, startY + 58));
        }
    }

    protected void addFullPlayerInventory(int startY) {
        addFullPlayerInventory(8, startY);
    }

    protected <W extends Widget_Neptune<?>> W addWidget(W widget) {
        if (widget == null) throw new NullPointerException("widget");
        widgets.add(widget);
        return widget;
    }

    public ImmutableList<Widget_Neptune<?>> getWidgets() {
        return ImmutableList.copyOf(widgets);
    }

    @Nullable
    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        Slot slot = slotId < 0 ? null : this.inventorySlots.get(slotId);
        if (slot == null) {
            return super.slotClick(slotId, dragType, clickType, player);
        }

        ItemStack playerStack = player.inventory.getItemStack();
        if (slot instanceof IPhantomSlot) {
            IPhantomSlot phantom = (IPhantomSlot) slot;
            if (playerStack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else if (!StackUtil.canMerge(playerStack, StackUtil.asNonNull(slot.getStack()))) {
                ItemStack copy = playerStack.copy();
                copy.setCount(1);
                slot.putStack(copy);
            } else if (phantom.canAdjustCount()) {
                ItemStack stack = slot.getStack();
                if (stack.getCount() < stack.getMaxStackSize()) {
                    stack.grow(1);
                    slot.putStack(stack);
                }
            }
            return playerStack;
        }
        return super.slotClick(slotId, dragType, clickType, player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        Slot firstSlot = this.inventorySlots.get(0);
        int playerInventorySize = 36;
        boolean playerInventoryFirst = firstSlot.inventory instanceof InventoryPlayer;

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (inventorySlots.size() == playerInventorySize) return ItemStack.EMPTY;
            if (playerInventoryFirst) {
                if (index < playerInventorySize) {
                    if (!this.mergeItemStack(itemstack1, playerInventorySize, this.inventorySlots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.mergeItemStack(itemstack1, 0, playerInventorySize, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (index < this.inventorySlots.size() - playerInventorySize) {
                    if (!this.mergeItemStack(itemstack1, this.inventorySlots.size() - playerInventorySize,
                        this.inventorySlots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.mergeItemStack(itemstack1, 0, this.inventorySlots.size() - playerInventorySize,
                    true)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

        return itemstack;
    }

    public static ItemStack safeCopy(ItemStack in) {
        return in == null ? null : in.copy();
    }

    // Package-private so that the widget itself can send this
    void sendWidgetData(Widget_Neptune<?> widget, IPayloadWriter writer) {
        int widgetId = widgets.indexOf(widget);
        if (widgetId == -1) {
            if (DEBUG) {
                throw new IllegalArgumentException(
                    "Invalid Widget Request! (" + (widget == null ? "null" : widget.getClass()) + ")");
            } else {
                BCLog.logger.warn("[lib.container] Received an invalid widget sending request!");
                BCLog.logger
                    .warn("[lib.container]   Widget {id = " + widgetId + ", class = " + widget.getClass() + "}");
                BCLog.logger.warn("[lib.container]   Container {class = " + getClass() + "}");
                BCLog.logger.warn(
                    "[lib.container]   Player {class = " + player.getClass() + ", name = " + player.getName() + "}");
            }
        } else {
            sendMessage(NET_WIDGET, (buffer) -> {
                buffer.writeShort(widgetId);
                writer.write(buffer);
            });
        }
    }

    public final void sendMessage(int id) {
        Side side = player.world.isRemote ? Side.CLIENT : Side.SERVER;
        sendMessage(id, (buffer) -> writeMessage(id, buffer, side));
    }

    public final void sendMessage(int id, IPayloadWriter writer) {
        PacketBufferBC payload = PacketBufferBC.write(writer);
        MessageContainer message = new MessageContainer(windowId, id, payload);
        if (player.world.isRemote) {
            MessageManager.sendToServer(message);
        } else {
            MessageManager.sendTo(message, (EntityPlayerMP) player);
        }
    }

    public void writeMessage(int id, PacketBufferBC buffer, Side side) {}

    public void readMessage(int id, PacketBufferBC buffer, Side side, MessageContext ctx) throws IOException {
        if (id == NET_WIDGET) {
            int widgetId = buffer.readUnsignedShort();
            if (widgetId < 0 || widgetId >= widgets.size()) {
                if (DEBUG) {
                    String string = "Received unknown or invalid widget ID " + widgetId + " on side " + side;
                    if (side == Side.SERVER) {
                        string += " (for player " + player.getName() + ")";
                    }
                    BCLog.logger.warn(string);
                }
            } else {
                Widget_Neptune<?> widget = widgets.get(widgetId);
                if (side == Side.SERVER) {
                    widget.handleWidgetDataServer(ctx, buffer);
                } else if (side == Side.CLIENT) {
                    widget.handleWidgetDataClient(ctx, buffer);
                }
            }
        } else if (side == Side.SERVER) {
            if (id == NET_SET_PHANTOM) {
                readSingleSetPhantom(buffer, ctx);
            } else if (id == NET_SET_PHANTOM_MULTI) {
                int count = buffer.readUnsignedByte();
                for (int i = 0; i < count; i++) {
                    readSingleSetPhantom(buffer, ctx);
                }
            }
        }
    }

    private void readSingleSetPhantom(PacketBufferBC buffer, MessageContext ctx) throws IOException {
        int idx = buffer.readVarInt();
        ItemStack stack = buffer.readItemStack();
        if (idx >= 0 && idx < inventorySlots.size()) {
            Slot s = inventorySlots.get(idx);
            if (s instanceof SlotPhantom) {
                SlotPhantom ph = (SlotPhantom) s;
                IItemHandlerAdv handler = ph.itemHandler;
                if (handler instanceof IItemHandlerModifiable && handler.canSet(ph.handlerIndex, stack)) {
                    ((IItemHandlerModifiable) handler).setStackInSlot(ph.handlerIndex, stack);
                } else {
                    // log rather than throw an exception because of bugged/naughty clients
                    String s2 = "[lib.container] Received an illegal phantom slot setting request! ";
                    s2 += "[The item handler disallowed the replacement] (Client = ";
                    s2 += ctx.getServerHandler().player.getName() + ", slot_index = " + idx;
                    s2 += ", stack = " + stack + ")";
                    BCLog.logger.warn(s2);
                }
                return;
            }
        }

        // log rather than throw an exception because of bugged/naughty clients
        String s2 = "[lib.container] Received an illegal phantom slot setting request! ";
        s2 += "[Didn't find a phantom slot for the given index] (Client = ";
        s2 += ctx.getServerHandler().player.getName() + ", slot_index = " + idx;
        s2 += ", stack = " + stack + ")";
        BCLog.logger.warn(s2);
    }

    /** @throws IllegalArgumentException if a {@link SlotPhantom} couldn't be found with that handler and index */
    public void sendSetPhantomSlot(IItemHandler handler, int index, ItemStack to) {
        sendSetPhantomSlot(findPhantomSlot(handler, index), to);
    }

    /** @param stacks The list of stacks to send. NOTE: this list CAN include nulls -- that indicates that the item
     *            should not be changed.
     * @throws IllegalArgumentException if {@link List#size() stacks.size()} differs from {@link IItemHandler#getSlots()
     *             handler.getSlots()}, or if a {@link SlotPhantom} couldn't be found for that handler and any of the
     *             indexes associated with it. */
    public void sendSetPhantomSlots(IItemHandler handler, List<ItemStack> stacks) {
        if (handler.getSlots() < stacks.size()) {
            throw new IllegalStateException("Too many ItemStacks's in the list to change, compared to the "
                + "size of the inventory! (list = " + stacks + ", handler = " + handler + ")");
        }
        int[] indexes = new int[stacks.size()];
        NonNullList<ItemStack> destinationStacks = NonNullList.create();
        int i2 = 0;
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack == null) {
                continue;
            }
            destinationStacks.add(stack);
            indexes[i2] = findPhantomSlot(handler, i);
            i2++;
        }
        indexes = Arrays.copyOf(indexes, i2);
        sendSetPhantomSlots(indexes, destinationStacks);
    }

    /** @throws IllegalArgumentException if a phantom slot cannot be found */
    private int findPhantomSlot(IItemHandler handler, int index) {
        int i = 0;
        for (Slot slot : inventorySlots) {
            if (slot instanceof SlotPhantom) {
                SlotPhantom ph = (SlotPhantom) slot;
                if (ph.itemHandler == handler && ph.handlerIndex == index) {
                    return i;
                }
            }
            i++;
        }
        throw new IllegalArgumentException("Couldn't find a slot for " + index + " @ " + handler + " in " + getClass());
    }

    public void sendSetPhantomSlot(SlotPhantom slot, ItemStack to) {
        int index = inventorySlots.indexOf(slot);
        if (index == -1) {
            throw new IllegalArgumentException("Couldn't find a slot for " + slot + " in " + getClass());
        }
        sendSetPhantomSlot(index, to);
    }

    private void sendSetPhantomSlot(int phIndex, ItemStack to) {
        sendMessage(NET_SET_PHANTOM, (buffer) -> {
            buffer.writeVarInt(phIndex);
            buffer.writeItemStack(to);
        });
    }

    private void sendSetPhantomSlots(int[] indexes, NonNullList<ItemStack> stacks) {
        if (indexes.length != stacks.size()) {
            throw new IllegalArgumentException("Sizes don't match! (" + indexes.length + " vs " + stacks.size() + ")");
        }
        sendMessage(NET_SET_PHANTOM_MULTI, (buffer) -> {
            buffer.writeByte(indexes.length);
            for (int i = 0; i < indexes.length; i++) {
                int index = indexes[i];
                ItemStack stack = stacks.get(i);
                buffer.writeVarInt(index);
                buffer.writeItemStack(stack);
            }
        });
    }
}
