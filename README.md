# 活点地图 · 蓝牙雷达 (Marauder's Map Bluetooth Scanner)

一个运行在安卓手机上的、活点地图（Marauder's Map）风格的应用：
扫描周围的蓝牙低功耗（BLE）设备，用旋转雷达 + 羊皮纸 UI 把它们标成"光点"。

## 技术栈
- Kotlin + Jetpack Compose（原生 Android）
- `BluetoothLeScanner` 做 BLE 扫描
- RSSI 估算距离，旋转扫描扇形可视化

## 目录结构
```
MaraudersMap/
├── app/build.gradle.kts
├── app/src/main/AndroidManifest.xml
├── app/src/main/java/com/marauders/map/
│   ├── MainActivity.kt                 # 入口：权限 / 蓝牙开关 / 绑定 UI
│   ├── ble/BluetoothScanner.kt         # BLE 扫描核心逻辑
│   ├── model/ScannedDevice.kt          # 设备数据模型
│   └── ui/                             # 主题 + 雷达/列表 UI
├── build.gradle.kts
└── settings.gradle.kts
```

## 如何运行
1. 安装 **Android Studio**（Hedgehog 或更新版本）。
2. 用 Android Studio 打开 `MaraudersMap/` 目录（首次打开会自动生成 Gradle Wrapper 并下载依赖）。
3. 手机开启 **开发者选项 → USB 调试**，用数据线连电脑。
4. 点击 Android Studio 的 ▶ Run，选择你的手机。
5. App 内点「开始侦测」，按提示授予蓝牙/定位权限即可。

> 说明：Android 12+ 使用 `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT`；
> Android 11 及以下需要 `ACCESS_FINE_LOCATION`（系统要求定位权限才能返回蓝牙扫描结果）。

## 云构建（GitHub Actions，无需本地装 SDK）
本仓库自带 `.github/workflows/build-apk.yml`，推到 GitHub 后会自动在云端打包出
`app-debug.apk`，下载即可安装到手机（无需在你电脑上装 Android Studio / SDK）。

步骤：
1. 在 `MaraudersMap/` 目录初始化 git 仓库并推到 GitHub（**直接把这个文件夹作为独立仓库**，
   不要把它包在 `Org Table` 里，否则 Gradle 工程不在根目录，工作流找不到 `gradlew`）：
   ```bash
   cd MaraudersMap
   git init
   git add .
   git commit -m "feat: 活点地图蓝牙雷达"
   git branch -M main
   git remote add origin https://github.com/<你的用户名>/<仓库名>.git
   git push -u origin main
   ```
2. 打开 GitHub 仓库 → **Actions** 标签页，能看到 "Build Debug APK" 工作流已自动运行
   （或手动点 **Run workflow**）。
3. 构建成功后，在对应 workflow 运行的 **Artifacts** 区下载 `marauders-map-debug`，
   解压得到 `app-debug.apk`。
4. 手机开启"未知来源安装"，把 APK 传过去安装即可。

> 提示：云端只生成 **debug** 包（无需签名）。若要发布到应用商店需自行配置签名密钥。

## 已知限制与可增强点
- **方位角是示意值**：当前用 MAC 地址哈希固定方向，并非真实测向。
  若要做到"谁在哪个方向"，需要多天线/AoA、IMU 或 RSSI 三角定位，属于后续增强。
- **距离是估算值**：基于 RSSI 的自由空间模型，受遮挡、人体、设备发射功率影响很大，
  可用 `BluetoothScanner.MEASURED_POWER / PATH_LOSS` 现场校准。
- 可扩展：接入真实地图底图、按设备类型分类着色、记录设备轨迹、导出 CSV 等。
