package com.supreme.artifact.damage;

import com.supreme.artifact.SupremeArtifactMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Custom damage sources that bypass all vanilla and mod defenses.
 * 
 * 三种伤害类型：
 * 1. supreme - 基础绝对伤害，绕过护甲、无敌、抗性
 * 2. fe_power - 梦幻终焉伤害，绕过所有防御（护甲、冷却、效果、附魔、无敌、抗性、盾牌）
 * 3. ds_power - 梦幻之影伤害，绕过无敌和盾牌
 * 
 * 所有伤害类型使用独立的supreme_artifact命名空间，避免与其他mod冲突
 */
public final class ModDamageSources {

    private ModDamageSources() {}

    // 基础绝对伤害
    public static final ResourceKey<DamageType> SUPREME_KEY = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(SupremeArtifactMod.MOD_ID, "supreme")
    );

    // 梦幻终焉伤害 (FE Power) - 绕过所有防御
    public static final ResourceKey<DamageType> FE_POWER_KEY = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(SupremeArtifactMod.MOD_ID, "fe_power")
    );

    // 梦幻之影伤害 (DS Power) - 绕过无敌和盾牌
    public static final ResourceKey<DamageType> DS_POWER_KEY = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(SupremeArtifactMod.MOD_ID, "ds_power")
    );

    /**
     * 创建基础绝对伤害源
     * 绕过护甲、无敌、抗性
     */
    public static DamageSource supreme(Entity attacker) {
        Level level = attacker.level();
        return new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SUPREME_KEY),
                attacker
        );
    }

    /**
     * 创建梦幻终焉伤害源 (FE Power)
     * 绕过所有防御：护甲、冷却、效果、附魔、无敌、抗性、盾牌
     * 通常将一半输出转化为绝对真实伤害
     */
    public static DamageSource fePower(Entity attacker) {
        Level level = attacker.level();
        return new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(FE_POWER_KEY),
                attacker
        );
    }

    /**
     * 创建梦幻之影伤害源 (DS Power)
     * 绕过无敌帧和盾牌
     * 玩家主动攻击时可穿透无敌帧
     */
    public static DamageSource dsPower(Entity attacker) {
        Level level = attacker.level();
        return new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DS_POWER_KEY),
                attacker
        );
    }
}
