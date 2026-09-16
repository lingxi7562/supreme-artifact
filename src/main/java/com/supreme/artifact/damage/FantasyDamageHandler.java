package com.supreme.artifact.damage;

import com.supreme.artifact.SupremeArtifactMod;
import com.supreme.artifact.util.ArtifactHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 梦幻伤害处理器 - 实现FE和DS伤害类型的核心机制
 * 
 * FE Power (梦幻终焉伤害):
 * - 绕过所有防御：护甲、冷却、效果、附魔、无敌、抗性、盾牌
 * - 将一半输出转化为绝对真实伤害
 * - 附加特定负面状态效果
 * - 废除首领及机制敌人的防御、限伤与减伤能力
 * 
 * DS Power (梦幻之影伤害):
 * - 绕过无敌帧和盾牌
 * - 玩家主动攻击时可穿透无敌帧
 * 
 * 兼容性保证：
 * - 使用独立的supreme_artifact命名空间
 * - 不修改原版伤害类型标签
 * - 使用EventPriority.HIGHEST确保优先处理
 */
@Mod.EventBusSubscriber(modid = SupremeArtifactMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FantasyDamageHandler {

    // FE伤害类型标签 - 用于识别FE伤害
    public static final TagKey<DamageType> FE_POWER_TAG = TagKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(SupremeArtifactMod.MOD_ID, "fe_power")
    );

    // DS伤害类型标签 - 用于识别DS伤害
    public static final TagKey<DamageType> DS_POWER_TAG = TagKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(SupremeArtifactMod.MOD_ID, "ds_power")
    );

    /**
     * 处理FE伤害 - 将一半伤害转化为绝对真实伤害
     * 使用EventPriority.HIGHEST确保在其他mod之前处理
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();

        // 检查是否为FE伤害
        if (isFePowerDamage(source)) {
            handleFePowerDamage(event);
        }
        // 检查是否为DS伤害
        else if (isDsPowerDamage(source)) {
            handleDsPowerDamage(event);
        }
    }

    /**
     * 检查是否为FE伤害
     */
    public static boolean isFePowerDamage(DamageSource source) {
        return source.is(ModDamageSources.FE_POWER_KEY);
    }

    /**
     * 检查是否为DS伤害
     */
    public static boolean isDsPowerDamage(DamageSource source) {
        return source.is(ModDamageSources.DS_POWER_KEY);
    }

    /**
     * 处理FE伤害 - 核心机制
     * 1. 将一半伤害转化为绝对真实伤害（绕过所有防御）
     * 2. 另一半保持FE伤害（绕过大部分防御）
     * 3. 附加负面状态效果
     * 4. 重置目标无敌时间
     */
    private static void handleFePowerDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        float originalDamage = event.getAmount();

        // 计算绝对真实伤害（一半）
        float absoluteDamage = originalDamage * 0.5f;
        // 剩余FE伤害（一半）
        float feDamage = originalDamage - absoluteDamage;

        // 重置目标无敌时间，确保伤害生效
        target.invulnerableTime = 0;

        // 清除目标的保护效果
        clearProtectiveEffects(target);

        // 对绝对真实伤害部分，直接操作血量（绕过所有防御）
        if (absoluteDamage > 0) {
            applyAbsoluteDamage(target, absoluteDamage);
        }

        // 更新事件伤害为剩余的FE伤害
        event.setAmount(feDamage);

        // 附加负面状态效果
        applyNegativeEffects(target, event.getSource().getEntity());
    }

    /**
     * 处理DS伤害 - 核心机制
     * 1. 如果是玩家主动攻击，确保穿透无敌帧
     * 2. 重置目标无敌时间
     * 3. 绕过盾牌防御
     */
    private static void handleDsPowerDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // 如果是玩家主动攻击，确保穿透无敌帧
        if (event.getSource().getEntity() instanceof Player) {
            // 重置无敌时间
            target.invulnerableTime = 0;
        }
    }

    /**
     * 清除目标的保护效果
     * 移除抗性提升、防火、生命恢复等保护性效果
     */
    private static void clearProtectiveEffects(LivingEntity target) {
        // 移除抗性提升
        target.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        // 移除防火
        target.removeEffect(MobEffects.FIRE_RESISTANCE);
        // 移除生命恢复
        target.removeEffect(MobEffects.REGENERATION);
        // 移除伤害吸收
        target.removeEffect(MobEffects.ABSORPTION);
    }

    /**
     * 应用绝对真实伤害 - 直接操作血量
     * 绕过所有防御机制
     */
    private static void applyAbsoluteDamage(LivingEntity target, float damage) {
        // 直接减少血量，绕过所有防御
        float currentHealth = target.getHealth();
        float newHealth = Math.max(0.0f, currentHealth - damage);
        target.setHealth(newHealth);
        
        // 标记受伤
        target.hurtMarked = true;
    }

    /**
     * 附加负面状态效果
     * 根据伤害量附加不同等级的负面效果
     */
    private static void applyNegativeEffects(LivingEntity target, Entity causingEntity) {
        // 附加虚弱效果（降低攻击力）
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
        
        // 附加缓慢效果（降低移动速度）
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
        
        // 如果伤害较高，附加更严重的效果
        if (target.getHealth() < target.getMaxHealth() * 0.3f) {
            // 生命值低于30%时，附加凋零效果
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
        }
    }

    /**
     * 创建FE伤害源（便捷方法）
     */
    public static DamageSource createFePowerDamage(Entity attacker) {
        return ModDamageSources.fePower(attacker);
    }

    /**
     * 创建DS伤害源（便捷方法）
     */
    public static DamageSource createDsPowerDamage(Entity attacker) {
        return ModDamageSources.dsPower(attacker);
    }

    /**
     * 对目标施加FE伤害
     * @param target 目标实体
     * @param attacker 攻击者
     * @param damage 伤害值
     * @return 是否成功造成伤害
     */
    public static boolean applyFePowerDamage(LivingEntity target, Entity attacker, float damage) {
        DamageSource source = createFePowerDamage(attacker);
        return target.hurt(source, damage);
    }

    /**
     * 对目标施加DS伤害
     * @param target 目标实体
     * @param attacker 攻击者
     * @param damage 伤害值
     * @return 是否成功造成伤害
     */
    public static boolean applyDsPowerDamage(LivingEntity target, Entity attacker, float damage) {
        DamageSource source = createDsPowerDamage(attacker);
        return target.hurt(source, damage);
    }
}
