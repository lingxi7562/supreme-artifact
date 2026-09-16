package com.supreme.artifact.event;

import com.supreme.artifact.damage.ModDamageSources;
import com.supreme.artifact.item.BladeOfFinalityItem;
import com.supreme.artifact.util.ArtifactHelper;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 终焉之刃事件处理器 — 承载所有主动单体攻击模式
 *
 * 从 OffenseHandler 分离的非AoE、非被动反击机制：
 * - Mode 0/2/3 (ManaitaPlus): 普通/坠落/湮灭 — Float.MAX_VALUE + die()
 * - Mode 1 (ManaitaPlus): 抹除 — 从服务器追踪系统中移除实体
 * - Mode 4 (mhzy): 血量覆写 — 绕过hurt()，直接操作SynchedEntityData
 * - Mode 5 (mhzy): 无限死亡 — 血量设为负无穷
 * - Mode 6 (mhzy): 梦幻终焉 — 半额绝对真实伤害 + 穿透无敌帧
 * - Mode 7 (mhzy): 梦幻之影 — 穿透无敌帧 + 盾牌
 */
public final class BladeAttackHandler {

    private BladeAttackHandler() {}

    // Reflection cache for SynchedEntityData internals
    private static Field itemsByIdField;
    private static Field entityDataField;
    private static boolean reflectionInitialized = false;

    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        bus.register(new BladeAttackHandler());
        initReflection();
    }

    /**
     * Initialize reflection for direct health manipulation
     */
    private static void initReflection() {
        try {
            itemsByIdField = SynchedEntityData.class.getDeclaredField("itemsById");
            itemsByIdField.setAccessible(true);

            entityDataField = Entity.class.getDeclaredField("entityData");
            entityDataField.setAccessible(true);

            reflectionInitialized = true;
        } catch (Exception e) {
            // Fallback: reflection not available
        }
    }

    /**
     * Get the entityData field value via reflection (entityData is protected)
     */
    @SuppressWarnings("unchecked")
    private static SynchedEntityData getEntityData(Entity entity) throws Exception {
        return (SynchedEntityData) entityDataField.get(entity);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerAttack(AttackEntityEvent event) {
        Player attacker = event.getEntity();
        Entity target = event.getTarget();

        if (!ArtifactHelper.hasBlade(attacker)) return;

        ItemStack bladeStack = ArtifactHelper.getBladeStack(attacker);
        if (bladeStack.isEmpty()) return;

        int mode = BladeOfFinalityItem.getAttackMode(bladeStack);

        // Cancel the original attack
        event.setCanceled(true);

        // Apply attack based on mode
        switch (mode) {
            case BladeOfFinalityItem.MODE_NORMAL:
            case BladeOfFinalityItem.MODE_DOWN:
            case BladeOfFinalityItem.MODE_ANNIHILATE:
                attackManaita(target, attacker, mode);
                break;
            case BladeOfFinalityItem.MODE_REMOVE:
                removeOnServer(target);
                break;
            case BladeOfFinalityItem.MODE_DIRECT_HEALTH:
                applyDirectHealthOverride(target, attacker);
                break;
            case BladeOfFinalityItem.MODE_INFINITY_DEATH:
                applyInfinityDeath(target, attacker);
                break;
            case BladeOfFinalityItem.MODE_FE_POWER:
                applyFePowerAttack(target, attacker);
                break;
            case BladeOfFinalityItem.MODE_DS_POWER:
                applyDsPowerAttack(target, attacker);
                break;
        }
    }

    // ==================== ManaitaPlus Attack Modes (0/2/3) ====================

    /**
     * Mode 0/2/3: ManaitaPlus attack — Float.MAX_VALUE + MAX_HEALTH=0 + die() + zero float data
     */
    private void attackManaita(Entity target, Player player, int mode) {
        if (target.level().isClientSide) return;

        if (target instanceof net.minecraftforge.entity.PartEntity<?> partEntity) {
            attackManaita(partEntity.getParent(), player, mode);
            return;
        }

        target.hurt(target.damageSources().playerAttack(player), Float.MAX_VALUE);
        target.handleEntityEvent((byte) 2);

        if (target instanceof LivingEntity living) {
            AttributeInstance attribute = living.getAttribute(Attributes.MAX_HEALTH);
            if (attribute != null) {
                attribute.setBaseValue(0.0F);
            }
            living.setLastHurtByPlayer(player);
            living.die(living.damageSources().playerAttack(player));
            zeroSynchedFloatData(living);
        }
    }

    // ==================== ManaitaPlus Entity Removal (Mode 1) ====================

    /**
     * Mode 1: Completely remove entity from all server tracking systems.
     * Uses public API for entity removal since direct field access is not
     * available in official mappings.
     */
    private void removeOnServer(Entity target) {
        if (target.level().isClientSide) return;

        if (target instanceof LivingEntity living) {
            living.hurt(living.damageSources().generic(), Float.MAX_VALUE);
            living.die(living.damageSources().generic());
        }

        target.remove(Entity.RemovalReason.DISCARDED);
    }

    // ==================== mhzy: Direct Health Override (Mode 4) ====================

    /**
     * Mode 4: Direct Health Override — Bypasses hurt() entirely,
     * directly manipulates SynchedEntityData health value.
     */
    private void applyDirectHealthOverride(Entity target, Player player) {
        if (target.level().isClientSide) return;
        if (!(target instanceof LivingEntity living)) return;

        float damage = Float.MAX_VALUE;
        setTrueHealthDirect(living, -damage);
        living.handleEntityEvent((byte) 2);
        living.setLastHurtByPlayer(player);

        if (living.getHealth() <= 0) {
            living.die(living.damageSources().playerAttack(player));
            zeroSynchedFloatData(living);
        }
    }

    // ==================== mhzy: Infinity Death (Mode 5) ====================

    /**
     * Mode 5: Infinity Death — health becomes negative infinity,
     * making any form of resurrection impossible.
     */
    private void applyInfinityDeath(Entity target, Player player) {
        if (target.level().isClientSide) return;
        if (!(target instanceof LivingEntity living)) return;

        setTrueHealthDirect(living, Float.NEGATIVE_INFINITY);
        living.handleEntityEvent((byte) 3);
        living.setLastHurtByPlayer(player);
        zeroSynchedFloatData(living);

        AttributeInstance maxHealthAttr = living.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(0.0F);
        }

        living.die(living.damageSources().playerAttack(player));

        if (!living.isRemoved()) {
            living.setHealth(0.0F);
        }
    }

    // ==================== Fantasy Ending: FE Power (Mode 6) ====================

    /**
     * Mode 6: FE Power (梦幻终焉伤害)
     * - Bypasses all defenses
     * - Converts half damage to absolute true damage
     * - Applies negative status effects
     */
    private void applyFePowerAttack(Entity target, Player player) {
        if (target.level().isClientSide) return;
        if (!(target instanceof LivingEntity living)) return;

        float baseDamage = 100.0F;

        living.invulnerableTime = 0;

        living.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        living.removeEffect(MobEffects.FIRE_RESISTANCE);
        living.removeEffect(MobEffects.REGENERATION);
        living.removeEffect(MobEffects.ABSORPTION);

        float absoluteDamage = baseDamage * 0.5F;
        float feDamage = baseDamage - absoluteDamage;

        float currentHealth = living.getHealth();
        float newHealth = Math.max(0.0F, currentHealth - absoluteDamage);
        living.setHealth(newHealth);
        living.hurtMarked = true;

        if (feDamage > 0) {
            living.hurt(ModDamageSources.fePower(player), feDamage);
        }

        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));

        if (living.getHealth() < living.getMaxHealth() * 0.3F) {
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
        }

        living.setLastHurtByPlayer(player);

        if (living.getHealth() <= 0) {
            living.die(living.damageSources().playerAttack(player));
            zeroSynchedFloatData(living);
        }
    }

    // ==================== Fantasy Ending: DS Power (Mode 7) ====================

    /**
     * Mode 7: DS Power (梦幻之影伤害)
     * - Bypasses invulnerability frames and shield
     * - Resets target invulnerability time
     */
    private void applyDsPowerAttack(Entity target, Player player) {
        if (target.level().isClientSide) return;
        if (!(target instanceof LivingEntity living)) return;

        float baseDamage = 80.0F;

        living.invulnerableTime = 0;
        living.hurt(ModDamageSources.dsPower(player), baseDamage);
        living.setLastHurtByPlayer(player);

        if (living.getHealth() <= 0) {
            living.die(living.damageSources().playerAttack(player));
            zeroSynchedFloatData(living);
        }
    }

    // ==================== Shared Utility Methods ====================

    /**
     * Directly manipulate the health value in SynchedEntityData.
     * Finds the actual DataItem that stores health and sets it directly.
     */
    private void setTrueHealthDirect(LivingEntity living, float targetHealth) {
        if (!reflectionInitialized || itemsByIdField == null || entityDataField == null) {
            living.setHealth(targetHealth);
            return;
        }

        try {
            SynchedEntityData entityData = getEntityData(living);
            @SuppressWarnings("unchecked")
            Map<Integer, SynchedEntityData.DataItem<?>> itemsById =
                    (Map<Integer, SynchedEntityData.DataItem<?>>) itemsByIdField.get(entityData);

            for (SynchedEntityData.DataItem<?> item : itemsById.values()) {
                if (item.getValue() instanceof Float floatValue) {
                    if (Math.abs(floatValue - living.getHealth()) < 0.01F) {
                        setDirectHealthValue(living, item, targetHealth);
                        return;
                    }
                }
            }

            // Fallback: use standard setHealth
            living.setHealth(targetHealth);
        } catch (Exception e) {
            living.setHealth(targetHealth);
        }
    }

    /**
     * Set the value of a SynchedEntityData.DataItem directly via reflection.
     */
    @SuppressWarnings("unchecked")
    private void setDirectHealthValue(LivingEntity living, SynchedEntityData.DataItem<?> dataItem, float value) {
        try {
            Field valueField = SynchedEntityData.DataItem.class.getDeclaredField("value");
            valueField.setAccessible(true);
            valueField.set(dataItem, value);
            dataItem.setDirty(true);
            // Sync via the public setHealth API
            living.setHealth(value);
        } catch (Exception e) {
            // Fallback: use standard setHealth
            living.setHealth(value);
        }
    }

    /**
     * Zero all float SynchedEntityData (matches ManaitaPlus)
     */
    private void zeroSynchedFloatData(LivingEntity living) {
        if (!reflectionInitialized || itemsByIdField == null || entityDataField == null) return;
        try {
            SynchedEntityData entityData = getEntityData(living);
            @SuppressWarnings("unchecked")
            Map<Integer, SynchedEntityData.DataItem<?>> itemsById =
                    (Map<Integer, SynchedEntityData.DataItem<?>>) itemsByIdField.get(entityData);

            for (SynchedEntityData.DataItem<?> item : itemsById.values()) {
                if (item.getValue() instanceof Float) {
                    @SuppressWarnings("unchecked")
                    SynchedEntityData.DataItem<Float> floatItem = (SynchedEntityData.DataItem<Float>) item;
                    floatItem.setValue(0.0F);
                }
            }
        } catch (Exception e) {
            // Silently fail
        }
    }
}
