package com.supreme.artifact.event;

import com.supreme.artifact.item.SupremeCharmItem;
import com.supreme.artifact.util.ArtifactHelper;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 万法归一攻击系统 — 仅保留AoE/时控模式
 *
 * 所有主动单体攻击已分离至 BladeAttackHandler。
 *
 * 保留模式：
 * - Mode 0 (时停): 终末之环 — 冻结范围内所有实体
 * - Mode 1 (神格升华): 终末之环 — 神化形态，范围抹杀
 * - Mode 2 (百分比斩击): mhzy — 百分比伤害 + 横扫
 * - Mode 3 (终极): 融合所有AoE机制
 */
public final class OffenseHandler {

    private OffenseHandler() {}

    // Track time-stopped entities
    private static final Set<UUID> TIME_STOPPED = new HashSet<>();
    private static final int TIME_STOP_DURATION = 180; // 9 seconds

    // Reflection cache for SynchedEntityData internals
    private static Field itemsByIdField;
    private static Field entityDataField;
    private static boolean reflectionInitialized = false;

    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        bus.register(new OffenseHandler());
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerAttack(AttackEntityEvent event) {
        Player attacker = event.getEntity();
        Entity target = event.getTarget();

        if (!ArtifactHelper.hasCharm(attacker)) return;

        ItemStack charmStack = ArtifactHelper.getCharmStack(attacker);
        if (charmStack.isEmpty()) return;

        int mode = SupremeCharmItem.getAttackMode(charmStack);

        // Cancel the original attack
        event.setCanceled(true);

        // Apply AoE attack based on mode
        switch (mode) {
            case SupremeCharmItem.MODE_TIME_STOP:
                applyTimeStop(target, attacker);
                break;
            case SupremeCharmItem.MODE_DIVINE_ASCENSION:
                applyDivineAscension(target, attacker);
                break;
            case SupremeCharmItem.MODE_PERCENTAGE_SLASH:
                applyPercentageSlash(target, attacker);
                break;
            case SupremeCharmItem.MODE_ULTIMATE:
                applyUltimate(target, attacker);
                break;
        }
    }

    // ==================== 终末之环: Time Stop (Mode 0) ====================

    /**
     * Mode 0: Time Stop — Freeze all entities in range for 180 ticks (9 seconds)
     */
    private void applyTimeStop(Entity target, Player player) {
        if (target.level().isClientSide) return;

        AABB freezeArea = target.getBoundingBox().inflate(32.0);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, freezeArea);

        for (LivingEntity entity : entities) {
            if (entity == player) continue;

            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TIME_STOP_DURATION, 255, false, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, TIME_STOP_DURATION, 255, false, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, TIME_STOP_DURATION, 0, false, false, false));

            entity.setDeltaMovement(0, 0, 0);
            entity.setNoGravity(true);
            TIME_STOPPED.add(entity.getUUID());
            entity.setGlowingTag(true);
        }

        // Apply basic ManaitaPlus attack to primary target
        attackManaita(target, player);
    }

    // ==================== 终末之环: Divine Ascension (Mode 1) ====================

    /**
     * Mode 1: Divine Ascension — Transform into God form and obliterate all nearby entities
     */
    private void applyDivineAscension(Entity target, Player player) {
        if (target.level().isClientSide) return;

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 255, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 10, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 255, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 255, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 255, false, false, false));

        player.setHealth(player.getMaxHealth());
        player.setGlowingTag(true);

        // Attack primary target
        attackManaita(target, player);

        // AoE obliteration
        AABB divineArea = player.getBoundingBox().inflate(64.0);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, divineArea);
        for (LivingEntity entity : entities) {
            if (entity == player) continue;
            if (entity instanceof Player) continue;
            attackManaita(entity, player);
        }
    }

    // ==================== mhzy: Percentage Slash (Mode 2) ====================

    /**
     * Mode 2: Percentage Slash — Matches mhzy SplashAttackGoal
     * Damage = 5 + maxHealth * 0.001 + health * 0.001
     * For players or health > 600: additional maxHealth * 0.01
     * Plus 10-block knockback + sweep attack on nearby entities
     */
    private void applyPercentageSlash(Entity target, Player player) {
        if (target.level().isClientSide) return;
        if (!(target instanceof LivingEntity living)) return;

        // Calculate percentage damage
        float baseDamage = 5.0F;
        float percentDamage = baseDamage
                + living.getMaxHealth() * 0.001F
                + living.getHealth() * 0.001F;

        // Extra damage for players or high-HP entities
        if (living.getHealth() > 600 || living instanceof Player) {
            percentDamage += living.getMaxHealth() * 0.01F;
        }

        // Apply damage via standard hurt
        living.hurt(living.damageSources().playerAttack(player), percentDamage);

        // Strong knockback — 10 blocks
        Vec3 knockbackDir = living.position().subtract(player.position());
        if (knockbackDir.lengthSqr() < 0.0001) {
            knockbackDir = new Vec3(
                    (Math.random() - Math.random()) * 0.01,
                    0,
                    (Math.random() - Math.random()) * 0.01
            );
        }
        Vec3 knockback = living.getDeltaMovement().add(knockbackDir.normalize().scale(10.0F));
        living.setDeltaMovement(knockback);
        living.hurtMarked = true;

        // Sync motion to client if target is a player
        if (living instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(living));
        }

        // Sweep attack — damage nearby entities
        float sweepRadius = 6.5F;
        Vec3 hitLocation = living.position();
        AABB sweepArea = AABB.ofSize(hitLocation, sweepRadius * 2.0F, sweepRadius, sweepRadius * 2.0F);
        List<Entity> nearbyEntities = player.level().getEntities(player, sweepArea);

        for (Entity entity : nearbyEntities) {
            if (entity == player || entity == living) continue;
            if (entity instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            if (entity instanceof LivingEntity nearbyLiving) {
                // Sweep damage
                float sweepDamage = 1.0F + player.getRandom().nextFloat();
                nearbyLiving.hurt(nearbyLiving.damageSources().playerAttack(player), sweepDamage);

                // Also apply percentage damage to nearby entities
                float nearbyPercentDamage = 5.0F
                        + nearbyLiving.getMaxHealth() * 0.001F
                        + nearbyLiving.getHealth() * 0.001F;
                nearbyLiving.hurt(nearbyLiving.damageSources().playerAttack(player), nearbyPercentDamage);

                // Knockback nearby entities too
                Vec3 nearbyKnockback = nearbyLiving.position().subtract(player.position());
                if (nearbyKnockback.lengthSqr() > 0.0001) {
                    nearbyLiving.setDeltaMovement(
                            nearbyLiving.getDeltaMovement().add(nearbyKnockback.normalize().scale(10.0F))
                    );
                    nearbyLiving.hurtMarked = true;
                    if (nearbyLiving instanceof ServerPlayer sp) {
                        sp.connection.send(new ClientboundSetEntityMotionPacket(nearbyLiving));
                    }
                }
            }
        }
    }

    // ==================== Ultimate Mode (Mode 3) ====================

    /**
     * Mode 3: Ultimate — Combines all AoE attack mechanisms
     */
    private void applyUltimate(Entity target, Player player) {
        if (target.level().isClientSide) return;
        if (!(target instanceof LivingEntity living)) return;

        // Step 1: Percentage damage
        float percentDamage = 5.0F + living.getMaxHealth() * 0.01F + living.getHealth() * 0.001F;
        living.hurt(living.damageSources().playerAttack(player), percentDamage);

        // Step 2: Direct health override to NEGATIVE_INFINITY
        setTrueHealthDirect(living, Float.NEGATIVE_INFINITY);

        // Step 3: MAX_HEALTH = 0
        AttributeInstance maxHealthAttr = living.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(0.0F);
        }

        // Step 4: Zero all float SynchedEntityData
        zeroSynchedFloatData(living);

        // Step 5: Set last hurt + die
        living.setLastHurtByPlayer(player);
        living.handleEntityEvent((byte) 2);
        living.die(living.damageSources().playerAttack(player));

        // Step 6: Strong knockback
        Vec3 knockbackDir = living.position().subtract(player.position());
        if (knockbackDir.lengthSqr() > 0.0001) {
            living.setDeltaMovement(
                    living.getDeltaMovement().add(knockbackDir.normalize().scale(10.0F))
            );
            living.hurtMarked = true;
            if (living instanceof ServerPlayer sp) {
                sp.connection.send(new ClientboundSetEntityMotionPacket(living));
            }
        }

        // Step 7: Sweep nearby entities
        AABB sweepArea = living.getBoundingBox().inflate(16.0);
        List<LivingEntity> nearbyEntities = player.level().getEntitiesOfClass(LivingEntity.class, sweepArea);
        for (LivingEntity entity : nearbyEntities) {
            if (entity == player || entity == living) continue;
            if (entity instanceof Player p && (p.isCreative() || p.isSpectator())) continue;

            // Apply infinity death to all nearby entities
            setTrueHealthDirect(entity, Float.NEGATIVE_INFINITY);
            AttributeInstance attr = entity.getAttribute(Attributes.MAX_HEALTH);
            if (attr != null) attr.setBaseValue(0.0F);
            zeroSynchedFloatData(entity);
            entity.setLastHurtByPlayer(player);
            entity.die(entity.damageSources().playerAttack(player));
        }
    }

    // ==================== Shared Utility Methods ====================

    /**
     * Basic ManaitaPlus attack — Float.MAX_VALUE + MAX_HEALTH=0 + die()
     */
    private void attackManaita(Entity target, Player player) {
        if (target.level().isClientSide) return;

        if (target instanceof net.minecraftforge.entity.PartEntity<?> partEntity) {
            attackManaita(partEntity.getParent(), player);
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

    /**
     * Get the entityData field value via reflection (entityData is protected)
     */
    @SuppressWarnings("unchecked")
    private static SynchedEntityData getEntityData(Entity entity) throws Exception {
        return (SynchedEntityData) entityDataField.get(entity);
    }

    /**
     * Directly manipulate the health value in SynchedEntityData.
     */
    private void setTrueHealthDirect(LivingEntity living, float targetHealth) {
        if (!reflectionInitialized || itemsByIdField == null || entityDataField == null) {
            living.setHealth(targetHealth);
            return;
        }

        try {
            SynchedEntityData entityData = getEntityData(living);
            @SuppressWarnings("unchecked")
            var itemsById = (java.util.Map<Integer, SynchedEntityData.DataItem<?>>) itemsByIdField.get(entityData);

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
     * Zero all float SynchedEntityData
     */
    private void zeroSynchedFloatData(LivingEntity living) {
        if (!reflectionInitialized || itemsByIdField == null || entityDataField == null) return;
        try {
            SynchedEntityData entityData = getEntityData(living);
            @SuppressWarnings("unchecked")
            var itemsById = (java.util.Map<Integer, SynchedEntityData.DataItem<?>>) itemsByIdField.get(entityData);

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

    /**
     * Tick handler to manage time-stopped entities
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.level.isClientSide) return;

        TIME_STOPPED.removeIf(uuid -> {
            Entity entity = event.level.getEntity((int) uuid.getLeastSignificantBits());
            if (entity instanceof LivingEntity living) {
                if (!living.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) ||
                        living.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration() <= 1) {
                    living.setNoGravity(false);
                    living.setGlowingTag(false);
                    return true;
                }
                living.setDeltaMovement(0, 0, 0);
            }
            return entity == null;
        });
    }
}
