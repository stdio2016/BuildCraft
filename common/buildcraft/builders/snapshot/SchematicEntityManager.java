/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import buildcraft.api.core.InvalidInputDataException;
import buildcraft.api.schematics.ISchematicEntity;
import buildcraft.api.schematics.SchematicEntityContext;
import buildcraft.api.schematics.SchematicEntityFactory;
import buildcraft.api.schematics.SchematicEntityFactoryRegistry;

public class SchematicEntityManager {
    @SuppressWarnings("WeakerAccess")
    public static ISchematicEntity getSchematicEntity(SchematicEntityContext context) {
        for (SchematicEntityFactory<?> schematicEntityFactory : Lists.reverse(SchematicEntityFactoryRegistry.getFactories())) {
            if (schematicEntityFactory.predicate.test(context)) {
                ISchematicEntity schematicEntity = schematicEntityFactory.supplier.get();
                schematicEntity.init(context);
                return schematicEntity;
            }
        }
        return null;
    }

    @SuppressWarnings("WeakerAccess")
    public static <S extends ISchematicEntity> S createCleanCopy(S schematicBlock) {
        return SchematicEntityFactoryRegistry
            .getFactoryByInstance(schematicBlock)
            .supplier
            .get();
    }

    @Nonnull
    public static <S extends ISchematicEntity> NBTTagCompound writeToNBT(S schematicEntity) {
        NBTTagCompound schematicEntityTag = new NBTTagCompound();
        schematicEntityTag.setString(
            "name",
            SchematicEntityFactoryRegistry
                .getFactoryByInstance(schematicEntity)
                .name
                .toString()
        );
        schematicEntityTag.setTag("data", schematicEntity.serializeNBT());
        return schematicEntityTag;
    }

    @Nonnull
    public static ISchematicEntity readFromNBT(NBTTagCompound schematicEntityTag) throws InvalidInputDataException {
        ResourceLocation name = new ResourceLocation(schematicEntityTag.getString("name"));
        SchematicEntityFactory<?> factory = SchematicEntityFactoryRegistry.getFactoryByName(name);
        if (factory == null) {
            throw new InvalidInputDataException("Unknown schematic type " + name);
        }
        ISchematicEntity schematicEntity = factory.supplier.get();
        NBTTagCompound data = schematicEntityTag.getCompoundTag("data");
        try {
            schematicEntity.deserializeNBT(data);
            return schematicEntity;
        } catch (InvalidInputDataException e) {
            throw new InvalidInputDataException("Failed to load the schematic from " + data, e);
        }
    }
}
