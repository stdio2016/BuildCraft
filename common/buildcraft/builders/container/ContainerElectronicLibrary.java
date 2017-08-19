/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.container;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;

import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.ContainerBC_Neptune;
import buildcraft.lib.gui.slot.SlotBase;
import buildcraft.lib.gui.slot.SlotOutput;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.net.PacketBufferBC;
import buildcraft.lib.tile.TileBC_Neptune;

import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.tile.TileElectronicLibrary;

public class ContainerElectronicLibrary extends ContainerBCTile<TileElectronicLibrary> {
    private static final IdAllocator IDS = ContainerBC_Neptune.IDS.makeChild("electronic_library");
    private static final int ID_SELECTED = IDS.allocId("SELECTED");

    public ContainerElectronicLibrary(EntityPlayer player, TileElectronicLibrary tile) {
        super(player, tile);
        addFullPlayerInventory(138);

        addSlotToContainer(new SlotOutput(tile.invDownOut, 0, 175, 57));
        addSlotToContainer(new SlotBase(tile.invDownIn, 0, 219, 57));

        addSlotToContainer(new SlotBase(tile.invUpIn, 0, 175, 79));
        addSlotToContainer(new SlotOutput(tile.invUpOut, 0, 219, 79));
    }

    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }
    
    @Override
    public boolean canInteractWith(EntityPlayer entityplayer) {
        return true;
    }

    public void sendSelectedToServer(Snapshot.Key selected) {
        sendMessage(ID_SELECTED, buffer -> {
            buffer.writeBoolean(selected != null);
            if (selected != null) {
                selected.writeToByteBuf(buffer);
            }
        });
    }

    @Override
    public void readMessage(int id, PacketBufferBC buffer, Side side, MessageContext ctx) throws IOException {
        super.readMessage(id, buffer, side, ctx);
        if (side == Side.SERVER) {
            if (id == ID_SELECTED) {
                if (buffer.readBoolean()) {
                    tile.selected = new Snapshot.Key(buffer);
                } else {
                    tile.selected = null;
                }
                tile.sendNetworkUpdate(TileBC_Neptune.NET_RENDER_DATA);
            }
        }
    }
}
