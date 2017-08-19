/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.robotics.container;

import net.minecraft.entity.player.EntityPlayer;

import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.slot.SlotBase;
import buildcraft.lib.gui.slot.SlotOutput;

import buildcraft.robotics.tile.TileZonePlanner;

public class ContainerZonePlanner extends ContainerBCTile<TileZonePlanner> {
    public ContainerZonePlanner(EntityPlayer player, TileZonePlanner tile) {
        super(player, tile);
        addFullPlayerInventory(88, 146);

        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                addSlotToContainer(new SlotBase(tile.invPaintbrushes, x * 4 + y, 8 + x * 18, 146 + y * 18));
            }
        }
        addSlotToContainer(new SlotBase(tile.invInputPaintbrush, 0, 8, 125));
        addSlotToContainer(new SlotBase(tile.invInputMapLocation, 0, 26, 125));
        addSlotToContainer(new SlotOutput(tile.invInputResult, 0, 74, 125));
        addSlotToContainer(new SlotBase(tile.invOutputPaintbrush, 0, 233, 9));
        addSlotToContainer(new SlotBase(tile.invOutputMapLocation, 0, 233, 27));
        addSlotToContainer(new SlotOutput(tile.invOutputResult, 0, 233, 75));
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}
