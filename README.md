# 万法归一 (Supreme Artifact)

**Minecraft Forge 1.20.1 Mod**

融合多个顶级OP mod核心机制的终极神器mod，包含完整的攻防体系，能够应对任何mod的挑战。

---

## 📖 简介

万法归一mod整合了以下mod的核心机制：

- **更好的砧板 (ManaitaPlus)** - 实体抹除、血量操控
- **终末之环 (Goety: Revelation)** - 时停、神格升华、维度增益、复活机制
- **最终之剑 (Final Sword)** - 终极攻击
- **幻想终结 (FantasyEnding)** - FE/DS伤害系统、生命妨害值
- **咸鱼套装 (Salted Fish Set)** - 绝对防御
- **永爱之刃 (Forever Love Sword)** - 绝对防御机制

---

## 🎮 物品列表

### 1. 万法归一 (Supreme Charm)

**类型**: Curios饰品（项链/护符/头部槽位）

**核心特性**:
- 4种AoE攻击模式，可切换
- 8层防御体系
- 终末之环融合机制（维度增益、复活、伤害减免）
- Double.POSITIVE_INFINITY 属性加成
- 永久附魔光效

#### 攻击模式

| 模式 | 名称 | 机制 |
|------|------|------|
| 0 | 时停 | 冻结64×64×64范围内所有实体9秒（180 tick） |
| 1 | 神格升华 | 进入神化形态，力量/速度/抗性全满，范围抹杀 |
| 2 | 百分比斩击 | 按目标最大生命值百分比伤害 + 10格击退 + 横扫 |
| 3 | 终极 | 融合所有AoE攻击机制 |

**切换方式**: Shift + 右键

#### 防御机制

**8层防御体系**:

1. **L1 - LivingAttackEvent拦截** (HIGHEST优先级)
   - 取消所有攻击事件
   - 重置fallDistance/hurtTime/deathTime

2. **L2 - LivingHurtEvent拦截** (HIGHEST优先级)
   - 将伤害设为0

3. **L3 - LivingDeathEvent拦截** (HIGHEST优先级)
   - 取消死亡事件
   - 恢复满血

4. **L4 - LivingFallEvent拦截** (HIGHEST优先级)
   - 取消坠落伤害

5. **L5 - 负面效果清除**
   - 每tick清除所有负面药水效果

6. **L6 - 爆炸免疫**
   - 从爆炸影响列表中移除

7. **L7 - 传送免疫**
   - 取消所有强制传送

8. **L8 - 生命恢复**
   - 每tick恢复满血
   - 清除火焰

**终末之环融合机制**:

- **伤害减免**:
  - 远程伤害减免 80%
  - 虚空/坠落伤害减免 90%
  - 维度减伤: 主世界30% / 下界50% / 末地70%

- **复活机制**:
  - 80%概率免疫死亡
  - 触发满血复活 + 全局时停5秒 + 10秒无敌
  - 60秒冷却时间

- **维度增益**:
  - 主世界: 白天恢复I / 夜晚夜视
  - 下界: 力量III + 火焰抗性
  - 末地: 抗性III + 16格内敌对生物虚弱III/缓慢II

- **隐藏特性**:
  - 生物友好: 敌对生物不会主动攻击
  - 永久飞行能力
  - 移动速度+50%

**属性加成**:
- 护甲: Double.POSITIVE_INFINITY
- 韧性: Double.POSITIVE_INFINITY
- 击退抗性: Double.POSITIVE_INFINITY
- 最大生命: Double.POSITIVE_INFINITY
- 攻击伤害: Double.POSITIVE_INFINITY
- 攻击速度: Double.POSITIVE_INFINITY
- 移动速度: +50%

---

### 2. 终焉之刃 (Blade of Finality)

**类型**: 手持武器（主手/副手）

**核心特性**:
- 8种单体攻击模式，可切换
- 永久附魔光效

#### 攻击模式

| 模式 | 名称 | 机制 |
|------|------|------|
| 0 | 普通 | Float.MAX_VALUE伤害 + 最大生命归零 + die() + 清零SynchedEntityData |
| 1 | 抹除 | 从所有服务器追踪系统中完全移除实体 |
| 2 | 坠落 | 普通模式 + 坠落效果 |
| 3 | 湮灭 | 普通模式 + 湮灭效果 |
| 4 | 血量覆写 | 绕过hurt()，直接操作SynchedEntityData血量数据 |
| 5 | 无限死亡 | 血量设为Float.NEGATIVE_INFINITY，无法复活 |
| 6 | 梦幻终焉 | FE伤害：50%绝对真实伤害 + 穿透无敌帧 + 负面状态 |
| 7 | 梦幻之影 | DS伤害：穿透无敌帧 + 盾牌，玩家攻击必中 |

**切换方式**: Shift + 右键

---

## 🔧 伤害类型系统

### FE Power (梦幻终焉伤害)

**标签**:
- bypasses_armor
- bypasses_cooldown
- bypasses_effects
- bypasses_enchantments
- bypasses_invulnerability
- bypasses_resistance
- bypasses_shield
- always_hurts_ender_dragons

**机制**:
- 绕过所有防御
- 50%转化为绝对真实伤害（直接操作血量）
- 50%保持FE伤害
- 清除目标保护效果（抗性、防火、恢复、吸收）
- 附加虚弱II和缓慢II
- 目标血量低于30%时附加凋零I

### DS Power (梦幻之影伤害)

**标签**:
- bypasses_invulnerability
- bypasses_shield
- always_hurts_ender_dragons

**机制**:
- 绕过无敌帧和盾牌
- 玩家主动攻击时穿透无敌帧
- 重置目标无敌时间

### 生命妨害值系统

**机制**:
- 记录实体上次受到的妨害伤害值
- 无敌时间内（>10 tick），新伤害必须大于上次值才能生效
- 只造成差值伤害，防止多次小额伤害快速击杀
- 仅对非玩家实体生效

**属性**:
- `life_interference_resistance`: 生命妨害抗性 (0.0-2.0)
- `life_interference_damage`: 生命妨害伤害加成

---

## 🛡️ 防御系统架构

### 单一防御源

防御系统由**万法归一** (Curios饰品) 提供完整防御：
- 8层防御体系
- 伤害减免
- 复活机制
- 维度增益

### 事件优先级链

| 事件 | 处理器 | 优先级 | 说明 |
|------|--------|--------|------|
| LivingAttackEvent | DefenseHandler | HIGHEST | 多源防御拦截 |
| LivingHurtEvent | DefenseHandler | HIGHEST | 伤害减免/取消 |
| LivingHurtEvent | FantasyDamageHandler | HIGHEST | FE/DS伤害处理 |
| LivingHurtEvent | LifeInterferenceHandler | LOWEST | 生命妨害值（仅非玩家） |
| LivingDeathEvent | DefenseHandler | HIGHEST | 死亡取消/复活 |
| AttackEntityEvent | OffenseHandler | HIGHEST | 万法归一AoE攻击 |
| AttackEntityEvent | BladeAttackHandler | HIGHEST | 终焉之刃单体攻击 |

### 防御优先级

- **万法归一**: 提供完整防御 + 伤害减免 + 复活机制 + 维度增益

---

## 🛡️ 兼容性保证

### 命名空间隔离

- 所有自定义内容使用 `supreme_artifact` 命名空间
- NBT键名添加 `supreme_artifact:` 前缀
- 伤害类型、属性、标签均独立注册

### 实体过滤

- 处理器之间通过检查装备状态避免重复处理
- 生命妨害值仅对非玩家实体生效

### 可选依赖

以下mod为可选依赖，安装后获得更好的兼容性：
- `manaita_plus` (更好的砧板)
- `goety_revelation` (诡厄巫法：启示录)
- `fantasy_ending` (幻想终结)
- `forever_love_sword` (永爱之刃)

---

## 📦 依赖

**必需**:
- Minecraft 1.20.1
- Forge 47.2.0+
- Curios API 5.14.1+

**可选**:
- ManaitaPlus (更好的砧板)
- Goety Revelation (诡厄巫法：启示录)
- FantasyEnding (幻想终结)
- Forever Love Sword (永爱之刃)

---

## 🎯 获取方式

### 创造模式

```
/give @p supreme_artifact:supreme_charm
/give @p supreme_artifact:blade_of_finality
```

### 合成配方

无合成配方，仅通过创造模式获取。

---

## 🎮 使用说明

### 万法归一

1. 装备到Curios项链、护符或头部槽位
2. Shift + 右键切换攻击模式（4种AoE）
3. 自动激活8层防御体系 + 终末之环机制
4. 攻击时根据当前模式应用对应AoE效果
5. 根据所在维度自动获得增益

### 终焉之刃

1. 手持主手或副手
2. Shift + 右键切换攻击模式（8种单体）
3. 攻击时根据当前模式应用对应单体效果

---

## 🔬 技术细节

### 反射机制

使用反射访问私有字段实现高级功能：
- `SynchedEntityData.itemsById`: 直接操作血量数据
- `SynchedEntityData.DataItem.value`: 修改同步数据值

### 实体抹除

从服务器所有追踪系统中移除实体：
- `entityManager.visibleEntityStorage`
- `entityManager.knownUuids`
- `entityTickList`
- `sectionStorage`
- `chunkMap.entityMap`

### 血量操控

绕过hurt()流程，直接操作血量：
1. 通过反射找到存储血量的DataItem
2. 直接设置DataItem的value字段
3. 标记为dirty触发客户端同步
4. 绕过所有基于事件的防御机制

### 单一防御检测

```java
// DefenseHandler 中所有防御检查统一使用 hasCharm()
if (ArtifactHelper.hasCharm(player)) {
    // 触发完整防御体系
}
```

---

## ⚠️ 注意事项

1. **极其强大**: 本mod物品极其强大，可能破坏游戏平衡
2. **仅测试用**: 建议仅在测试或创造模式中使用
3. **性能影响**: 大范围攻击和时停可能影响服务器性能
4. **兼容性**: 已测试与主流mod兼容，但不保证100%无冲突
5. **防御叠加**: 多个防御源不会叠加效果，但提供冗余保障

---

## 📝 版本历史

### v2.0.0 (当前)

- 终末之环机制完全融合至万法归一
- 新增终焉之刃（承载所有单体攻击模式）
- 重构防御系统，统一由万法归一提供
- 万法归一攻击模式从12种精简为4种AoE
- 添加永爱之刃防御机制

### v1.0.0

- 初始发布
- 整合5个mod的核心机制
- 12种攻击模式
- 8层防御体系
- 完整的FE/DS伤害系统
- 生命妨害值系统
- 终末之环完整实现

---

## 📄 许可证

All Rights Reserved

---

## 🙏 致谢

感谢以下mod的开发者：
- ManaitaPlus (更好的砧板)
- Goety Revelation (诡厄巫法：启示录)
- Final Sword (最终之剑)
- FantasyEnding (幻想终结)
- Salted Fish Set (咸鱼套装)
- Forever Love Sword (永爱之刃)

---

## 🐛 问题反馈

如遇到兼容性问题或bug，请提供：
- Minecraft版本
- Forge版本
- 已安装的mod列表
- 错误日志
- 复现步骤

---

**万法归一 — 攻防一体，万法皆破**
