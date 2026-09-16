package com.supreme.artifact.event;

import com.supreme.artifact.util.ArtifactHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.projectile.ArrowLooseEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 万法归一攻击处理器 - 处理终末之环融合后的攻击机制
 * 
 * 包含：
 * - 星辰箭矢伤害源（无视护甲）
 * - 箭矢加速（7倍速度，3倍伤害）
 * - 攻击附加负面效果
 */
public class CharmAttackHandler {
    
    // 星辰箭矢伤害类型
    public static final ResourceKey<DamageType> STAR_ARROW = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation("supreme_artifact", "star_arrow")
    );
    
    /**
     * 处理玩家攻击事件 - 使用星辰箭矢伤害源
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        
        if (!ArtifactHelper.hasCharm(player)) return;
        
        // 如果目标是生物，使用星辰箭矢伤害源
        if (target instanceof LivingEntity living) {
            // 取消原伤害事件
            event.setCanceled(true);
            
            // 计算伤害（基于玩家攻击力）
            float damage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            
            // 使用星辰箭矢伤害源（无视护甲）
            DamageSource source = createStarArrowDamage(player);
            living.hurt(source, damage);
            
            // 附加负面效果
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2)); // 虚弱III
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2)); // 缓慢III
        }
    }
    
    /**
     * 处理箭矢发射事件 - 加速箭矢
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onArrowLoose(ArrowLooseEvent event) {
        Player player = event.getEntity();
        
        if (!ArtifactHelper.hasCharm(player)) return;
        
        ItemStack bow = event.getBow();
        if (bow.isEmpty()) return;
        
        // 标记箭矢为星辰箭矢
        bow.getOrCreateTag().putBoolean("supreme_artifact:star_arrow", true);
    }
    
    /**
     * 处理实体加入世界事件 - 应用箭矢加速
     */
    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof Player player)) return;
        
        if (!ArtifactHelper.hasCharm(player)) return;
        
        // 检查是否为星辰箭矢
        ItemStack bow = player.getMainHandItem();
        if (bow.getTag() != null && bow.getTag().getBoolean("supreme_artifact:star_arrow")) {
            // 加速箭矢（7倍速度，3倍伤害）
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(7.0));
            arrow.setBaseDamage(arrow.getBaseDamage() * 3.0);
            
            // 标记为星辰箭矢
            arrow.getPersistentData().putBoolean("supreme_artifact:star_arrow", true);
            
            // 清除弓的标记
            bow.getTag().putBoolean("supreme_artifact:star_arrow", false);
        }
    }
    
    /**
     * 创建星辰箭矢伤害源
     */
    private DamageSource createStarArrowDamage(Player player) {
        return player.level().damageSources().playerAttack(player);
    }
}
