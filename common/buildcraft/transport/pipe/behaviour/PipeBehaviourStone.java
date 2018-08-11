/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import net.minecraft.nbt.NBTTagCompound;

import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventItem;

public class PipeBehaviourStone extends PipeBehaviourSeparate {
    public static final double SPEED_DELTA = 0.008;
    public static final double SPEED_TARGET = 0.01;

    public PipeBehaviourStone(IPipe pipe) {
        super(pipe);
    }

    public PipeBehaviourStone(IPipe pipe, NBTTagCompound nbt) {
        super(pipe, nbt);
    }

    @PipeEventHandler
    public static void modifySpeed(PipeEventItem.ModifySpeed event) {
        event.modifyTo(SPEED_TARGET, SPEED_DELTA);
    }
}
