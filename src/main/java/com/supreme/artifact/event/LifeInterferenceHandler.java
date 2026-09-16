package com.supreme.artifact.event;

import com.supreme.artifact.SupremeArtifactMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 生命妨害值系统 - 复刻自 mhzy (幻想终结) mod
 * 
 * 核心机制：
 * 1. 每个实体记录上次受到的"妨害伤害值" (lastInterferenceHurt)
 * 2. 如果在无敌时间内 (invulnerableTime > 10)，新伤害必须大于上次的妨害值才能生效
 * 3. 只有差值部分才会造成伤害
 * 4. 这防止了多次小额伤害快速击杀实体
 * 
 * 兼容性保证：
 * - 使用独立的属性注册表
 * - 使用 NBT 存储额外数据
 * - 使用 EventPriority.LOWEST 确保在其他 mod 之后处理
 * - 不修改原版类，使用 Mixin 或事件系统
 */
@Mod.EventBusSubscriber(modid = SupremeArtifactMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LifeInterferenceHandler {
    
    // 属性注册
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(
        ForgeRegistries.ATTRIBUTES, SupremeArtifactMod.MODID
    );
    
    // 生命妨害值抗性属性 (0.0 - 2.0, 默认 1.0)
    // 值越高，对妨害伤害的抗性越强
    public static final RegistryObject<Attribute> LIFE_INTERFERENCE_RESISTANCE = ATTRIBUTES.register(
        "life_interference_resistance",
        () -> new RangedAttribute(
            "attribute.supreme_artifact.life_interference_resistance",
            1.0D, 0.0D, 2.0D
        ).setSyncable(true)
    );
    
    // 生命妨害值伤害属性 (0.0 - MAX, 默认 0.0)
    // 增加实体造成的妨害伤害
    public static final RegistryObject<Attribute> LIFE_INTERFERENCE_DAMAGE = ATTRIBUTES.register(
        "life_interference_damage",
        () -> new RangedAttribute(
            "attribute.supreme_artifact.life_interference_damage",
            0.0D, 0.0D, Double.MAX_VALUE / 2.0D
        ).setSyncable(true)
    );
    
    // NBT 键名 - 添加命名空间前缀避免冲突
    private static final String NBT_LAST_INTERFERENCE_HURT = "supreme_artifact:LastInterferenceHurt";
    private static final String NBT_CUSTOM_INVULNERABLE_TIME = "supreme_artifact:CustomInvulnerableTime";
    
    /**
     * 获取实体的上次妨害伤害值
     */
    public static float getLastInterferenceHurt(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.contains(NBT_LAST_INTERFERENCE_HURT) 
            ? data.getFloat(NBT_LAST_INTERFERENCE_HURT) 
            : 0.0F;
    }
    
    /**
     * 设置实体的上次妨害伤害值
     */
    public static void setLastInterferenceHurt(LivingEntity entity, float value) {
        CompoundTag data = entity.getPersistentData();
        data.putFloat(NBT_LAST_INTERFERENCE_HURT, value);
    }
    
    /**
     * 获取实体的自定义无敌时间
     */
    public static int getCustomInvulnerableTime(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.contains(NBT_CUSTOM_INVULNERABLE_TIME) 
            ? data.getInt(NBT_CUSTOM_INVULNERABLE_TIME) 
            : 0;
    }
    
    /**
     * 设置实体的自定义无敌时间
     */
    public static void setCustomInvulnerableTime(LivingEntity entity, int time) {
        CompoundTag data = entity.getPersistentData();
        data.putInt(NBT_CUSTOM_INVULNERABLE_TIME, time);
    }
    
    /**
     * 应用生命妨害值逻辑
     * 
     * @param entity 目标实体
     * @param damage 原始伤害值
     * @return 实际应该造成的伤害值
     */
    public static float applyInterferenceLogic(LivingEntity entity, float damage) {
        int customInvulTime = getCustomInvulnerableTime(entity);
        
        // 如果在无敌时间内
        if (customInvulTime > 10) {
            float lastHurt = getLastInterferenceHurt(entity);
            
            // 如果新伤害小于等于上次伤害，完全免疫
            if (damage <= lastHurt) {
                return 0.0F;
            }
            
            // 只造成差值伤害
            return damage - lastHurt;
        }
        
        // 不在无敌时间内，记录这次伤害值
        setLastInterferenceHurt(entity, damage);
        return damage;
    }
    
    /**
     * 在实体受到伤害时处理妨害值逻辑
     * 使用 EventPriority.LOWEST 确保在其他 mod 之后处理
     * 注意：当玩家装备 SupremeCharm 时，DefenseHandler 已在 HIGHEST 优先级取消事件
     * 因此此处理器不会对装备 SupremeCharm 的玩家生效
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        // 如果事件已被取消，不处理
        if (event.isCanceled()) return;
        
        LivingEntity entity = event.getEntity();
        
        // 跳过玩家（玩家由 DefenseHandler 处理）
        if (entity instanceof Player) return;
        
        float originalDamage = event.getAmount();
        
        // 应用妨害值逻辑
        float actualDamage = applyInterferenceLogic(entity, originalDamage);
        
        // 应用妨害值抗性属性
        if (entity.getAttributes().hasAttribute(LIFE_INTERFERENCE_RESISTANCE.get())) {
            double resistance = entity.getAttributeValue(LIFE_INTERFERENCE_RESISTANCE.get());
            // 抗性公式: damage * (2 - resistance)
            // resistance = 1.0 时，伤害不变
            // resistance = 2.0 时，伤害为 0
            // resistance = 0.0 时，伤害翻倍
            actualDamage *= (float) (2.0D - resistance);
        }
        
        // 更新事件伤害值
        event.setAmount(Math.max(0.0F, actualDamage));
        
        // 设置自定义无敌时间 (20 ticks = 1 秒)
        if (actualDamage > 0) {
            setCustomInvulnerableTime(entity, 20);
        }
    }
    
    /**
     * 每 tick 减少自定义无敌时间
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        int customInvulTime = getCustomInvulnerableTime(entity);
        
        if (customInvulTime > 0) {
            setCustomInvulnerableTime(entity, customInvulTime - 1);
        }
    }
    
    /**
     * 注册属性到所有实体类型
     */
    @SubscribeEvent
    public static void onAttributeModification(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(entityType -> {
            ATTRIBUTES.getEntries().forEach(attributeRegistryObject -> {
                event.add(entityType, attributeRegistryObject.get());
            });
        });
    }
}
