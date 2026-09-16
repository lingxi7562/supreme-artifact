package com.supreme.artifact.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

/**
 * Supreme Charm Item - 万法归一
 *
 * 融合终末之环后的终极饰品，包含所有攻防机制。
 * 槽位：项链/护符/头部（兼容终末之环原槽位）
 *
 * 保留机制：
 * - 8层防御体系（被动反击）
 * - 4种AoE/时控攻击模式
 * - 终末之环：维度增益、复活、飞行、生物友好
 * - Double.POSITIVE_INFINITY 属性加成
 *
 * 来源：
 * - 终末之环 (Goety: Revelation): 时停、神格升华、维度增益、复活
 * - 幻想终结 (FantasyEnding): 百分比斩击（横扫）
 * - 咸鱼套装 (Salted Fish Set): 绝对防御
 */
public class SupremeCharmItem extends Item implements ICurioItem {

    private static final String MODE_KEY = "attack_mode";

    // 4种AoE/时控攻击模式
    public static final int MODE_TIME_STOP = 0;           // 冻结64×64×64范围内所有实体9秒
    public static final int MODE_DIVINE_ASCENSION = 1;    // 进入神化形态，范围抹杀
    public static final int MODE_PERCENTAGE_SLASH = 2;    // 按目标最大生命值百分比伤害 + 横扫
    public static final int MODE_ULTIMATE = 3;            // 融合所有AoE攻击机制
    public static final int MODE_COUNT = 4;

    // 终末之环特性标记
    private static final String RESURRECTION_COOLDOWN = "halo_resurrection_cooldown";

    // 伤害限制配置
    public static final float MAX_SINGLE_DAMAGE = 100.0F;  // 单次伤害上限
    public static final float HEALTH_THRESHOLD = 0.5F;     // 血量异常检测阈值（低于最大生命值的0.5%视为异常）

    public SupremeCharmItem() {
        super(new Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Enchantment glint
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            // Shift+Right-click: switch attack mode
            int currentMode = stack.getOrCreateTag().getInt(MODE_KEY);
            int nextMode = (currentMode + 1) % MODE_COUNT;
            stack.getOrCreateTag().putInt(MODE_KEY, nextMode);
            player.displayClientMessage(
                Component.literal("§6[万法归一]§r 攻击模式: §e" + getModeName(nextMode)), true);
            return InteractionResultHolder.sidedSuccess(stack, hand);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        int mode = stack.getOrCreateTag().getInt(MODE_KEY);
        tooltip.add(Component.literal("§6攻击模式: §e" + getModeName(mode)));
        tooltip.add(Component.literal("§7模式描述: §8" + getModeDescription(mode)));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§7Shift+右键: 切换攻击模式"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§5§l万法归一 — 攻防一体"));
        tooltip.add(Component.literal("§8被动防御 + AoE时控 | 单体攻击请装备终焉之刃"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§7终末之环特性:"));
        tooltip.add(Component.literal("  §e• 维度增益 (主世界/下界/末地)"));
        tooltip.add(Component.literal("  §e• 80%概率复活 + 时停"));
        tooltip.add(Component.literal("  §e• 永久飞行 + 生物友好"));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        // Use Double.POSITIVE_INFINITY for absolute defense
        var map = HashMultimap.<Attribute, AttributeModifier>create();
        map.put(Attributes.ARMOR, new AttributeModifier(uuid, "supreme_armor", Double.POSITIVE_INFINITY, AttributeModifier.Operation.ADDITION));
        map.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(uuid, "supreme_tough", Double.POSITIVE_INFINITY, AttributeModifier.Operation.ADDITION));
        map.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(uuid, "supreme_kb", Double.POSITIVE_INFINITY, AttributeModifier.Operation.ADDITION));
        map.put(Attributes.MAX_HEALTH, new AttributeModifier(uuid, "supreme_hp", Double.POSITIVE_INFINITY, AttributeModifier.Operation.ADDITION));
        map.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(uuid, "supreme_atk", Double.POSITIVE_INFINITY, AttributeModifier.Operation.ADDITION));
        map.put(Attributes.ATTACK_SPEED, new AttributeModifier(uuid, "supreme_spd", Double.POSITIVE_INFINITY, AttributeModifier.Operation.ADDITION));
        // 终末之环属性
        map.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(uuid, "supreme_move", 0.5, AttributeModifier.Operation.MULTIPLY_TOTAL));
        return map;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player && !player.level().isClientSide) {
            // L8: 生命恢复 - 每tick恢复满血
            player.setHealth(player.getMaxHealth());

            // 血量异常检测与恢复
            if (player.getHealth() < player.getMaxHealth() * HEALTH_THRESHOLD) {
                // 检测到异常血量，强制恢复
                player.setHealth(player.getMaxHealth());
                player.invulnerableTime = 40; // 给予2秒无敌时间
            }

            // L8: 清除火焰
            if (player.isOnFire()) {
                player.clearFire();
            }

            // L5: 负面效果清除 - 每tick清除所有负面药水效果
            player.getActiveEffects().removeIf(effect -> !effect.getEffect().isBeneficial());

            // 终末之环：永久飞行能力
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }

            // 永爱之刃：设置无敌时间
            player.invulnerableTime = 20;

            // 永爱之刃：重置坠落距离
            player.fallDistance = 0.0F;

            // 永爱之刃：重置饥饿值和饱和度
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0F);

            // 防护：每5秒检查并恢复关键属性
            if (player.tickCount % 100 == 0) {
                restoreCriticalAttributes(player);
            }
        }
    }

    /**
     * 恢复关键属性，防止被其他mod清除或修改
     */
    private void restoreCriticalAttributes(Player player) {
        // 确保无敌时间持续
        if (player.invulnerableTime < 20) {
            player.invulnerableTime = 20;
        }

        // 确保飞行能力
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }

        // 清除所有负面效果
        player.getActiveEffects().removeIf(effect -> !effect.getEffect().isBeneficial());

        // 防护：检查并恢复属性修改器
        restoreAttributeModifiers(player);
    }

    /**
     * 恢复属性修改器，防止被其他mod移除
     */
    private void restoreAttributeModifiers(Player player) {
        // 检查关键属性是否存在我们的修改器
        var modifiers = player.getAttributes().getAttributes();
        boolean needsRestore = false;

        // 检查护甲属性
        var armorAttr = player.getAttribute(Attributes.ARMOR);
        if (armorAttr != null && !hasModifier(armorAttr, "supreme_armor")) {
            needsRestore = true;
        }

        // 如果需要恢复，重新应用所有属性
        if (needsRestore) {
            // 触发属性更新
            player.refreshAttributes();
        }
    }

    /**
     * 检查属性是否包含指定名称的修改器
     */
    private boolean hasModifier(net.minecraft.world.entity.ai.attributes.AttributeInstance attr, String name) {
        for (var modifier : attr.getModifiers()) {
            if (modifier.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        String slot = slotContext.identifier();
        // 兼容原终末之环的头部槽位
        return "necklace".equals(slot) || "charm".equals(slot) || "head".equals(slot) || "halo".equals(slot);
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) {
            // 终末之环：装备时给予飞行能力
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) {
            // 卸下时检查是否还有其他万法归一
            boolean hasOther = false;
            for (ItemStack item : player.getInventory().items) {
                if (item.getItem() instanceof SupremeCharmItem && item != stack) {
                    hasOther = true;
                    break;
                }
            }

            // 如果没有其他万法归一，移除飞行能力（除非是创造模式）
            if (!hasOther && !player.isCreative()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }

    public static int getAttackMode(ItemStack stack) {
        return stack.getOrCreateTag().getInt(MODE_KEY);
    }

    public static String getModeName(int mode) {
        return switch (mode) {
            case MODE_TIME_STOP -> "时停";
            case MODE_DIVINE_ASCENSION -> "神格升华";
            case MODE_PERCENTAGE_SLASH -> "百分比斩击";
            case MODE_ULTIMATE -> "终极";
            default -> "未知";
        };
    }

    public static String getModeDescription(int mode) {
        return switch (mode) {
            case MODE_TIME_STOP -> "冻结所有实体9秒 (180 tick)";
            case MODE_DIVINE_ASCENSION -> "神化形态，范围抹杀";
            case MODE_PERCENTAGE_SLASH -> "按百分比扣血 + 10格击退 + 横扫";
            case MODE_ULTIMATE -> "融合所有AoE攻击机制";
            default -> "未知模式";
        };
    }
}
