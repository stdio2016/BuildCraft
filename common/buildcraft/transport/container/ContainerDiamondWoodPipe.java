/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.container;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;

import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

import buildcraft.api.transport.pipe.IPipeHolder.PipeMessageReceiver;

import buildcraft.lib.gui.ContainerBC_Neptune;
import buildcraft.lib.gui.ContainerPipe;
import buildcraft.lib.gui.slot.SlotPhantom;
import buildcraft.lib.net.PacketBufferBC;
import buildcraft.lib.tile.item.ItemHandlerSimple;

import buildcraft.transport.pipe.behaviour.PipeBehaviourWoodDiamond;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWoodDiamond.FilterMode;

public class ContainerDiamondWoodPipe extends ContainerPipe {
    private final PipeBehaviourWoodDiamond behaviour;
    private final ItemHandlerSimple filterInv;

    public ContainerDiamondWoodPipe(EntityPlayer player, PipeBehaviourWoodDiamond behaviour) {
        super(player, behaviour.pipe.getHolder());
        this.behaviour = behaviour;
        this.filterInv = behaviour.filters;
        behaviour.pipe.getHolder().onPlayerOpen(player);

        addFullPlayerInventory(79);

        for (int i = 0; i < 9; i++) {
            addSlotToContainer(new SlotPhantom(filterInv, i, 8 + i * 18, 18));
        }
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        behaviour.pipe.getHolder().onPlayerClose(player);
    }

    public void sendNewFilterMode(FilterMode newFilterMode) {
        this.sendMessage(NET_DATA, (buffer) -> buffer.writeEnumValue(newFilterMode));
    }

    @Override
    public void readMessage(int id, PacketBufferBC buffer, Side side, MessageContext ctx) throws IOException {
        super.readMessage(id, buffer, side, ctx);
        if (side == Side.SERVER) {
            behaviour.filterMode = buffer.readEnumValue(FilterMode.class);
            behaviour.pipe.getHolder().scheduleNetworkUpdate(PipeMessageReceiver.BEHAVIOUR);
        }
    }
}
