package com.supreme.artifact.init;

import com.supreme.artifact.SupremeArtifactMod;
import com.supreme.artifact.item.BladeOfFinalityItem;
import com.supreme.artifact.item.SupremeCharmItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SupremeArtifactMod.MOD_ID);

    public static final RegistryObject<SupremeCharmItem> SUPREME_CHARM =
            ITEMS.register("supreme_charm", SupremeCharmItem::new);

    public static final RegistryObject<BladeOfFinalityItem> BLADE_OF_FINALITY =
            ITEMS.register("blade_of_finality", BladeOfFinalityItem::new);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
