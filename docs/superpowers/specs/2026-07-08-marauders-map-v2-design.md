# 活点地图蓝牙雷达 · v2 设计文档

- 日期：2026-07-08
- 状态：已确认设计，待实现
- 范围：在已跑通的 MVP 基础上做需求细化，定位「极客玩具 / 可视化」

---

## 1. 背景与目标

MVP 已能在安卓上扫描 BLE 设备并以羊皮纸雷达呈现。本版目标是把它从「能跑的 demo」
打磨成一个**简约好看、可读、能记住过去**的玩具：周围蓝牙设备变成活点地图上的光点，
有名字用真名，无名给魔法绰号当背景氛围；点光点能看它的「档案」。

核心原则：**简约、好看、不堆功能**。

---

## 2. 已锁定需求（范围）

| 维度 | 决定 |
|------|------|
| 主用途 | 极客玩具 / 可视化，把周围蓝牙世界画出来 |
| 空间模型 | 抽象雷达：自己在圆心，距离=RSSI 估算，方向=示意（不做真方向） |
| 扫描范围 | 仅 BLE（不做经典蓝牙 / WiFi） |
| 可读性 | 有广播名→真名；无名→魔法绰号且画得更淡更小当背景；默认聚焦最近 N 个；点光点看详情 |
| 记忆 | 本地持久化「时间线 + 按设备档案」（Room），仅在详情里查看 |
| 风格 | 简约好看，羊皮纸基调，单屏为主 |

---

## 3. 非目标 / 明确不做（YAGNI）

- 经典蓝牙扫描、WiFi 扫描
- 真实方向 / 多天线 AoA / 双机三角定位
- 真地图底图 / 室内平面图叠加
- 独立的历史 / 档案浏览页、导出 CSV/JSON、分享
- 独立设置页（RSSI 校准、距离范围、聚焦数 N 的调节 UI）
- iOS / 跨平台
- 联网、账号、云端同步（数据不出本机）

> 这些列为未来可选项，但本版不做。

---

## 4. 架构与数据流

采用 **单 Activity + Jetpack Compose + MVVM + Room**，遵循「扫描为实时源、DB 做旁路日志」：

```
BleScanner (实时回调)
     │  onDeviceSeen(ScanResult)
     ▼
DeviceRepository
     ├─ 维护内存中的实时设备表（驱动雷达，快）
     ├─ 轻量协程：把档案/出现记录节流写入 Room
     └─ 暴露 devices: StateFlow<List<UiDevice>> 与 getProfile(mac)
     │
     ▼
MapViewModel (UI 状态: 设备列表 / 扫描中 / 聚焦数N / 筛选词)
     │
     ▼
Compose 界面 (雷达 + 列表 + 详情卡)
```

- 雷达由 Repository 的内存实时表驱动，**不**每帧读 DB，保证流畅。
- 详情卡点开时，ViewModel 调用 `repository.getProfile(mac)` 从 Room 读历史。

---

## 5. 数据模型

### 5.1 UiDevice（界面实时模型）
| 字段 | 说明 |
|------|------|
| `mac: String` | 唯一键 |
| `name: String` | 真名或魔法绰号 |
| `isNamed: Boolean` | true=有广播名；false=绰号 |
| `rssi: Int` | 当前信号 |
| `distance: Double` | RSSI 估算距离（米） |
| `angle: Float` | 雷达方位角（MAC 哈希，示意） |
| `firstSeen: Long?` | 档案中的首次出现（来自 DB，可为 null） |
| `seenCount: Int` | 累计出现次数（来自 DB） |
| `recentSightings: List<Sighting>` | 近期出现时间线（详情用） |

### 5.2 Room 实体

```kotlin
@Entity(tableName = "device_profiles")
data class DeviceProfileEntity(
    @PrimaryKey val mac: String,
    val displayName: String,
    val isNamed: Boolean,
    val firstSeen: Long,
    val lastSeen: Long,
    val seenCount: Int
)

@Entity(
    tableName = "sightings",
    indices = [Index("mac")]
)
data class SightingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mac: String,
    val timestamp: Long,
    val rssi: Int,
    val distance: Double
)
```

`DeviceDao`：`upsertProfile`、`getProfile(mac)`、`recordSighting`、
`getRecentSightings(mac, limit)`、`incrementSeenCount` 等。

---

## 6. 命名与聚焦规则

### 6.1 命名（NameProvider）
- 设备有广播名 → 用真名，`isNamed = true`
- 无广播名 → 从**固定魔法绰号表**（Moony / Wormtail / Padfoot / Prongs / …中世纪风）
  按 MAC 哈希稳定取一个，`isNamed = false`
- 同 MAC 永远得到同一绰号（稳定，不闪烁）

### 6.2 雷达呈现
- 所有发现的设备都画成光点
- **最近 N 个**（按 distance 升序）高亮：点更大、带文字标签
- 更远的光点更小、更淡
- `isNamed = false` 的整体比 `isNamed = true` 更淡（背景氛围感）

### 6.3 聚焦数 N
- 默认 `FOCUS_COUNT = 8`（常量，易改；本版无设置 UI）
- 雷达上一律画全部，N 只决定「高亮 + 带标签」的数量

---

## 7. 界面规范

### 7.1 主屏（雷达，单屏）
- 顶部：标题「活点地图」+ 名言「我庄严宣誓，我干的都不是好事。」
- 中部：雷达 `Canvas`（主角）：羊皮纸底、同心距离环、旋转扫描扇形、设备光点
- 下部：最近 N 个设备的**紧凑列表 / chip 行**（名字 + 距离 + dBm），与雷达联动
- 控制条：「开始 / 停止侦测」按钮 + 「侦测到 X 个设备」
- 无 BLE / 蓝牙关 / 权限拒：沿用并补强提示文案

### 7.2 设备详情卡（DeviceDetailSheet，底部弹层）
点雷达光点 **或** 列表项打开，展示：
- 名字（真名或绰号）、MAC
- 当前 RSSI、估算距离
- 首次出现时间、累计出现次数
- 近期出现时间线（mini 列表，来自 `getRecentSightings`）

### 7.3 轻量筛选（可选，默认不实现）
- 一条按名字/绰号过滤的搜索框，可同时过滤列表并高亮雷达上的匹配点
- **本版默认不做**；列为后续小增强，避免一开始把界面做复杂

---

## 8. 持久化策略

- 使用 Room，数据库仅本机，不涉及联网
- `DeviceProfile`：设备首次出现写 `firstSeen`，之后更新 `lastSeen`、累加 `seenCount`
- `Sighting`：**节流写入**——同一 MAC 距上次记录超过 `SIGHTING_MIN_INTERVAL_MS`
  （默认 2 分钟）才插一条，避免扫描每秒灌库
- 读数：详情卡按需 `getRecentSightings(mac, limit=20)`

---

## 9. 错误处理与边界

- 无 BLE 硬件 / 蓝牙关闭 / 权限被拒：显示明确提示（沿用 MVP 逻辑并补文案）
- 设备过多：靠「聚焦 N + 远处淡显」保持可读；Canvas 重绘开销可接受
- 数据库膨胀：`Sighting` 已节流；后续可加「只保留 30 天」自动清理（本版不急）
- RSSI 估算偏差大：用固定 `MEASURED_POWER=-59`、`PATH_LOSS=2.2`（可调常量，无 UI）

---

## 10. 测试策略

- 单元测试：
  - `rssiToDistance(rssi)` 在已知 RSSI 下距离合理
  - `NameProvider` 同 MAC 产出稳定且落在绰号表内
  - 「取最近 N 个」逻辑正确（含并列、不足 N 的情况）
  - `Sighting` 节流：短时间内重复出现不重复插库
- UI 预览：提供假 `BleScanner` / 假 Repository，便于 Compose Preview 看效果

---

## 11. 关键参数默认值

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `FOCUS_COUNT` | 8 | 高亮 + 带标签的最近设备数 |
| `MAX_RANGE_M` | 15.0 | 雷达标绘最大量程（超出压到边缘） |
| `MEASURED_POWER` | -59 | 1 米参考 RSSI |
| `PATH_LOSS` | 2.2 | 环境衰减系数 |
| `SIGHTING_MIN_INTERVAL_MS` | 120_000 | 同一设备两次 Sighting 记录最小间隔 |

---

## 12. 实现要点（文件级改动）

新增：
- `data/local/AppDatabase.kt`、`DeviceProfileEntity.kt`、`SightingEntity.kt`、`DeviceDao.kt`
- `data/NameProvider.kt`
- `data/DeviceRepository.kt`
- `ui/MapViewModel.kt`
- `ui/DeviceDetailSheet.kt`

修改：
- `ble/BluetoothScanner.kt`：扫描回调通知 Repository（或 Repository 订阅 scanner 输出）
- `ui/MaraudersMapScreen.kt`：雷达改用 `UiDevice`；加详情卡；应用聚焦 N 与淡显规则
- `MainActivity.kt`：装配 Repository + ViewModel，替代直接持有 Scanner
- `model/ScannedDevice.kt`：可合并进 `UiDevice` 或保留作内部传输

---

## 13. 验收标准

1. 打开 App → 点「开始侦测」→ 授权后雷达开始旋转并出现光点
2. 有名字的设备显示真名；无名设备显示稳定魔法绰号且明显更淡
3. 最近 8 个设备高亮带标签，其余更小更淡
4. 点任意光点 / 列表项弹出详情卡，显示名字、MAC、RSSI、距离、首次见、出现次数、近期时间线
5. 退出再进入、或隔几分钟再扫同一设备：档案的「首次见 / 出现次数」被正确累积
6. 设备极多时画面仍可读、不卡
7. 单测通过（距离、命名稳定、取最近 N、Sighting 节流）
8. 数据仅存本机，无联网行为
