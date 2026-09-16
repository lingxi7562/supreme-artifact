package com.supreme.artifact.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 终焉之刃 (Blade of Finality)
 *
 * 手持武器，承载所有主动单体攻击模式（从万法归一分离）。
 * 8种抹杀模式，Shift+右键切换。
 *
 * 来源：
 * - 更好的砧板 (ManaitaPlus): 模式 0-3（普通/抹除/坠落/湮灭）
 * - 幻想终结 (FantasyEnding): 模式 4-5（血量覆写/无限死亡）
 * - 幻想终结 (FantasyEnding): 模式 6-7（梦幻终焉/梦幻之影）
 * - 永爱之刃 (Forever Love Sword): 防御机制（手持时）
 */
public class BladeOfFinalityItem extends SwordItem {

    private static final String MODE_KEY = "blade_mode";

    // 8种主动单体攻击模式
    public static final int MODE_NORMAL = 0;              // Float.MAX_VALUE + die() + 清零SynchedEntityData
    public static final int MODE_REMOVE = 1;              // 从所有服务器追踪系统中移除实体
    public static final int MODE_DOWN = 2;                // 普通 + 坠落效果
    public static final int MODE_ANNIHILATE = 3;          // 普通 + 湮灭效果
    public static final int MODE_DIRECT_HEALTH = 4;       // 绕过hurt()，直接操作SynchedEntityData血量
    public static final int MODE_INFINITY_DEATH = 5;      // 血量设为负无穷，无法复活
    public static final int MODE_FE_POWER = 6;            // 半额绝对真实伤害 + 穿透无敌帧 + 负面状态
    public static final int MODE_DS_POWER = 7;            // 穿透无敌帧 + 盾牌
    public static final int MODE_COUNT = 8;

    public BladeOfFinalityItem() {
        super(Tiers.NETHERITE, 0, -2.4F, new Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            int currentMode = stack.getOrCreateTag().getInt(MODE_KEY);
            int nextMode = (currentMode + 1) % MODE_COUNT;
            stack.getOrCreateTag().putInt(MODE_KEY, nextMode);
            player.displayClientMessage(
                    Component.literal("§c[终焉之刃]§r 攻击模式: §e" + getModeName(nextMode)), true);
            return InteractionResultHolder.sidedSuccess(stack, level);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        int mode = stack.getOrCreateTag().getInt(MODE_KEY);
        tooltip.add(Component.literal("§c攻击模式: §e" + getModeName(mode)));
        tooltip.add(Component.literal("§7模式描述: §8" + getModeDescription(mode)));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§7Shift+右键: 切换攻击模式"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§4§l终焉之刃 — 万物皆斩"));
        tooltip.add(Component.literal("§8融合: 更好的砧板 / 幻想终结"));
    }

    public static int getAttackMode(ItemStack stack) {
        return stack.getOrCreateTag().getInt(MODE_KEY);
    }

    public static String getModeName(int mode) {
        return switch (mode) {
            case MODE_NORMAL -> "普通";
            case MODE_REMOVE -> "抹除";
            case MODE_DOWN -> "坠落";
            case MODE_ANNIHILATE -> "湮灭";
            case MODE_DIRECT_HEALTH -> "血量覆写";
            case MODE_INFINITY_DEATH -> "无限死亡";
            case MODE_FE_POWER -> "梦幻终焉";
            case MODE_DS_POWER -> "梦幻之影";
            default -> "未知";
        };
    }

    public static String getModeDescription(int mode) {
        return switch (mode) {
            case MODE_NORMAL -> "Float.MAX_VALUE伤害 + 最大生命归零 + die()";
            case MODE_REMOVE -> "从所有追踪系统中移除实体";
            case MODE_DOWN -> "普通模式 + 坠落效果";
            case MODE_ANNIHILATE -> "普通模式 + 湮灭效果";
            case MODE_DIRECT_HEALTH -> "绕过hurt()，直接操作SynchedEntityData";
            case MODE_INFINITY_DEATH -> "血量设为负无穷，无法复活";
            case MODE_FE_POWER -> "半额绝对真实伤害 + 穿透无敌帧 + 负面状态";
            case MODE_DS_POWER -> "穿透无敌帧 + 盾牌，玩家攻击必中";
            default -> "未知模式";
        };
    }
}
