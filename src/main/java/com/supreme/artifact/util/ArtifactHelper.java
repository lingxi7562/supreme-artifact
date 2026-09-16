package com.supreme.artifact.util;

import com.supreme.artifact.init.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.Map;
import java.util.Optional;

/**
 * Utility for checking whether a player has the Supreme Charm equipped via Curios.
 */
public final class ArtifactHelper {

    private ArtifactHelper() {}

    /**
     * Returns true if the player has a Supreme Charm equipped in any Curios slot.
     */
    public static boolean hasCharm(Player player) {
        if (player == null || player.level().isClientSide) return false;

        return CuriosApi.getCuriosInventory(player)
                .map(handler -> {
                    Map<String, ICurioStacksHandler> slots = handler.getCurios();
                    for (ICurioStacksHandler stacksHandler : slots.values()) {
                        for (int i = 0; i < stacksHandler.getStacks().getSlots(); i++) {
                            ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                            if (stack.getItem() == ModItems.SUPREME_CHARM.get()) {
                                return true;
                            }
                        }
                    }
                    return false;
                })
                .orElse(false);
    }

    /**
     * Returns the equipped Supreme Charm ItemStack, or ItemStack.EMPTY if not equipped.
     */
    public static ItemStack getCharmStack(Player player) {
        if (player == null) return ItemStack.EMPTY;

        return CuriosApi.getCuriosInventory(player)
                .map(handler -> {
                    Map<String, ICurioStacksHandler> slots = handler.getCurios();
                    for (ICurioStacksHandler stacksHandler : slots.values()) {
                        for (int i = 0; i < stacksHandler.getStacks().getSlots(); i++) {
                            ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                            if (stack.getItem() == ModItems.SUPREME_CHARM.get()) {
                                return stack;
                            }
                        }
                    }
                    return ItemStack.EMPTY;
                })
                .orElse(ItemStack.EMPTY);
    }

    /**
     * Returns true if the player is holding the Blade of Finality in main hand or off hand.
     */
    public static boolean hasBlade(Player player) {
        if (player == null) return false;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        return mainHand.getItem() == ModItems.BLADE_OF_FINALITY.get()
                || offHand.getItem() == ModItems.BLADE_OF_FINALITY.get();
    }

    /**
     * Returns the equipped Blade of Finality ItemStack, or ItemStack.EMPTY if not held.
     */
    public static ItemStack getBladeStack(Player player) {
        if (player == null) return ItemStack.EMPTY;

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() == ModItems.BLADE_OF_FINALITY.get()) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() == ModItems.BLADE_OF_FINALITY.get()) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }
}
