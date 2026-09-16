package com.supreme.artifact.event;

import com.supreme.artifact.item.SupremeCharmItem;
import com.supreme.artifact.util.ArtifactHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Defense system - 万法归一防御体系
 *
 * 融合终末之环后的完整防御机制：
 * - L1-L4: 事件拦截 (ManaitaPlus)
 * - L5-L8: 状态清除 (ManaitaPlus)
 * - 终末之环：伤害减免、复活机制、维度增益、生物友好
 */
public final class DefenseHandler {

    private DefenseHandler() {}

    // 终末之环：复活冷却标记
    private static final String RESURRECTION_COOLDOWN = "halo_resurrection_cooldown";

    // 终末之环：自定义伤害类型
    public static final ResourceKey<DamageType> HALO_FIRE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation("supreme_artifact", "halo_fire")
    );

    public static final ResourceKey<DamageType> STAR_ARROW = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation("supreme_artifact", "star_arrow")
    );

    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        bus.register(new DefenseHandler());
    }

    // ==================== L1-L4: 事件拦截 (ManaitaPlus) ====================

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (ArtifactHelper.hasCharm(player)) {
                event.setCanceled(true);
                player.setHealth(player.getMaxHealth());
                player.fallDistance = 0;
                player.hurtTime = 0;
                player.deathTime = 0;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (ArtifactHelper.hasCharm(player)) {
                // 终末之环：伤害减免
                applyDamageReduction(player, event);

                // 限制单次伤害量
                float originalDamage = event.getAmount();
                if (originalDamage > SupremeCharmItem.MAX_SINGLE_DAMAGE) {
                    event.setAmount(SupremeCharmItem.MAX_SINGLE_DAMAGE);
                }

                if (event.getAmount() <= 0) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (ArtifactHelper.hasCharm(player)) {
                // 终末之环：复活机制
                if (tryResurrection(player)) {
                    event.setCanceled(true);
                    return;
                }
                event.setCanceled(true);
                player.setHealth(player.getMaxHealth());
                player.fallDistance = 0;
                player.hurtTime = 0;
                player.deathTime = 0;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (ArtifactHelper.hasCharm(player)) {
                event.setCanceled(true);
                player.setHealth(player.getMaxHealth());
                player.fallDistance = 0;
                player.hurtTime = 0;
                player.deathTime = 0;
            }
        }
    }

    // ==================== L5-L8: 状态清除 (ManaitaPlus) ====================

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;
        if (!ArtifactHelper.hasCharm(player)) return;

        // Remove all non-beneficial effects
        player.getActiveEffects().removeIf(effect -> !effect.getEffect().isBeneficial());

        // 终末之环：末地光环效果
        applyEndAura(player);
    }

    // ==================== 终末之环：伤害减免 ====================

    /**
     * 终末之环：伤害减免机制
     */
    private void applyDamageReduction(Player player, LivingHurtEvent event) {
        DamageSource source = event.getSource();
        float damage = event.getAmount();

        // 远程伤害减免 80%
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) {
            event.setAmount(damage * 0.2f);
            return;
        }

        // 虚空/坠落伤害减免 90%
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) ||
            source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) {
            event.setAmount(damage * 0.1f);
            return;
        }

        // 维度减伤
        Level level = player.level();
        if (level.dimension() == Level.OVERWORLD) {
            event.setAmount(damage * 0.7f); // 主世界 30% 减伤
        } else if (level.dimension() == Level.NETHER) {
            event.setAmount(damage * 0.5f); // 下界 50% 减伤
        } else if (level.dimension() == Level.END) {
            event.setAmount(damage * 0.3f); // 末地 70% 减伤
        }
    }

    // ==================== 终末之环：复活机制 ====================

    /**
     * 终末之环：复活机制
     * 80%概率免疫死亡，触发满血复活 + 全局时停5秒 + 10秒无敌
     * 60秒冷却时间
     */
    private boolean tryResurrection(Player player) {
        // 检查复活冷却（60秒）
        long currentTime = player.level().getGameTime();
        long lastResurrection = player.getPersistentData().getLong(RESURRECTION_COOLDOWN);

        if (currentTime - lastResurrection < 1200) { // 60秒 = 1200 ticks
            return false; // 冷却中，无法复活
        }

        // 80% 概率免疫死亡
        if (player.level().random.nextFloat() < 0.8f) {
            player.setHealth(player.getMaxHealth());

            // 设置复活冷却
            player.getPersistentData().putLong(RESURRECTION_COOLDOWN, currentTime);

            // 触发全局时停
            if (player.level() instanceof ServerLevel serverLevel) {
                applyTimeStop(serverLevel, player, 100); // 5秒时停
            }

            // 给予长时间无敌
            player.invulnerableTime = 200; // 10秒无敌

            // 发送消息
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6万法归一§r 发动了复活！"));
            return true;
        }

        return false;
    }

    /**
     * 终末之环：应用时停效果
     */
    private void applyTimeStop(ServerLevel level, Player player, int duration) {
        // 给所有其他玩家施加缓慢效果
        for (ServerPlayer otherPlayer : level.players()) {
            if (otherPlayer != player) {
                otherPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 255));
                otherPlayer.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 255));
            }
        }

        // 给所有生物施加缓慢效果
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(64.0))) {
            if (entity != player) {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 255));
            }
        }
    }

    // ==================== 终末之环：维度增益 ====================

    /**
     * 终末之环：玩家登录/维度变化时应用维度增益
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!ArtifactHelper.hasCharm(player)) return;
        applyDimensionBuffs(player);
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        if (!ArtifactHelper.hasCharm(player)) return;
        applyDimensionBuffs(player);
    }

    /**
     * 终末之环：应用维度增益
     */
    private void applyDimensionBuffs(Player player) {
        Level level = player.level();

        if (level.dimension() == Level.OVERWORLD) {
            // 主世界：昼夜恢复效果
            boolean isDay = level.isDay();
            if (isDay) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0)); // 恢复 I
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0)); // 夜视
            }
        } else if (level.dimension() == Level.NETHER) {
            // 下界：攻击增强与排斥
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 2)); // 力量 III
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0)); // 火焰抗性
        } else if (level.dimension() == Level.END) {
            // 末地：初始增益
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 2)); // 抗性 III
        }
    }

    /**
     * 终末之环：末地光环效果 - 对周围敌人施加虚弱与诅咒
     * 持续生效，每20tick(1秒)应用一次
     */
    private void applyEndAura(Player player) {
        // 每20tick检查一次（1秒）
        if (player.tickCount % 20 != 0) return;

        Level level = player.level();

        // 仅在末地生效
        if (level.dimension() != Level.END) return;

        // 获取周围16格内的所有生物
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(16.0))) {
            if (entity == player) continue;
            if (entity instanceof Enemy || entity instanceof Mob) {
                // 施加虚弱和诅咒
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2)); // 虚弱 III
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1)); // 缓慢 II
            }
        }
    }

    // ==================== 终末之环：效果免疫 ====================

    /**
     * 终末之环：免疫负面状态
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPotionAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!ArtifactHelper.hasCharm(player)) return;

        // 免疫所有负面效果
        if (!event.getEffectInstance().getEffect().isBeneficial()) {
            event.setCanceled(true);
        }
    }

    // ==================== 终末之环：生物友好 ====================

    /**
     * 终末之环：敌对生物不会主动攻击玩家
     */
    @SubscribeEvent
    public void onMobAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!ArtifactHelper.hasCharm(player)) return;

        Entity attacker = event.getSource().getEntity();

        // 敌对生物不会主动攻击玩家
        if (attacker instanceof Mob mob && mob instanceof Enemy) {
            // 取消攻击
            event.setCanceled(true);
        }
    }
}
