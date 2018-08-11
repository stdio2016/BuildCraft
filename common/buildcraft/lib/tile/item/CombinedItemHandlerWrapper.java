package buildcraft.lib.tile.item;

import net.minecraft.item.ItemStack;

import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

import buildcraft.api.inventory.IItemHandlerFiltered;

/** Combines several {@link IItemHandlerModifiable} into one class. Extends forge's {@link CombinedInvWrapper} in order
 * to do this.
 * <p>
 * Also provides {@link IItemHandlerFiltered#getFilter(int)} if the wrapped handlers support it. */
public class CombinedItemHandlerWrapper extends CombinedInvWrapper implements IItemHandlerFiltered {

    public CombinedItemHandlerWrapper(IItemHandlerModifiable... itemHandler) {
        super(itemHandler);
    }

    @Override
    public ItemStack getFilter(int slot) {
        int index = getIndexForSlot(slot);
        IItemHandlerModifiable handler = getHandlerFromIndex(index);
        slot = getSlotFromIndex(slot, index);
        if (handler instanceof IItemHandlerFiltered) {
            return ((IItemHandlerFiltered) handler).getFilter(slot);
        }
        return handler.getStackInSlot(slot);
    }
}
