# Photidy · 相册轻理

> A quieter way to organize your photos.
> 轻轻整理，留住美好。

Photidy 是一款安静、精致、隐私优先的本地照片整理应用。用四向滑动手势整理相册，在 Android 系统对话框确认后真实删除或移动照片，全程离线、无账号、无广告、无追踪。

当前版本 **1.0.0**（minSdk 29 / targetSdk 36）。

## 功能

- **四向手势整理**：拖动跟随、方向判定、速度/距离阈值、回弹、飞出、撤销；四个方向可自定义动作
- **真实照片库**：MediaStore 读取照片与视频，相册按 bucket 聚合，支持完整 / 部分 / 拒绝三档权限
- **系统确认的删除与移动**：经 `createDeleteRequest` / `createWriteRequest` 由用户逐次确认，应用不申请全盘文件权限，也不直接改动文件
- **本地智能筛选**：截图、可能重复、连拍、长视频、大文件，纯元数据启发式，不解码像素、不联网；范围限定在当前相册
- **可选本地 AI 识别**：设置中显式开启后下载约 4 MB 量化小模型（TensorFlow Lite），在本机完成粗分类（食物 / 宠物 / 文档 / 自然 / 建筑），照片永不上传
- **搜索**：按文件名、相册名与日期片段检索（`IMG1003` 也能命中 `IMG_1003`）
- **时间线**：照片按拍摄月份分组，顶部月份芯片可快速跳转到任意月份
- **重复照片对比**：按大小与分辨率分组，并排选出一张保留，其余加入待删除
- **收藏与批量整理**：收藏视图独立成册；整理页可切换批量模式，多选后一次性批量保留或加入待删除
- **回顾与洞察**：照片构成环图、每月柱状图（逐条生长）、相册占用排行与可回收空间，全部本地统计
- **排序**：最新、最早、最大、随机（会话开始时洗牌一次，预览保持稳定）
- **三种视觉风格**：Minimal / Liquid Glass / Fluid Cloud，效果强度可调，支持深色模式
- **中英双语**：应用内切换（AppCompat per-app locale），进度不丢失
- **本地持久化**：主题、语言、手势映射、照片来源、整理进度、AI 标签保存在 DataStore

## 技术栈

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Coroutines / StateFlow · ViewModel · DataStore · Coil · TensorFlow Lite · Gradle Kotlin DSL（版本目录 + `build-logic` 约定插件）

## 环境要求

| 项目 | 版本 |
| --- | --- |
| JDK | 17+ |
| Android SDK Platform | API 36 |
| Android Build Tools | 36.0.0 |
| Gradle | 8.14.3（wrapper 自动下载） |
| AGP | 8.13.2 |
| Kotlin | 2.3.21 |
| Compose BOM | 2026.06.01 |

`local.properties`（不提交到版本库）需要指向本机 SDK，用 Android Studio 打开项目时会自动生成。手动创建时 Windows 路径需要转义反斜杠：

```properties
sdk.dir=C\:\\Android\\sdk
```

## 构建

```bash
./gradlew :app:assembleDebug    # 调试包
./gradlew :app:installDebug     # 安装到设备/模拟器
./gradlew test                  # JVM 单元测试
./gradlew lintDebug             # 静态检查
./gradlew connectedDebugAndroidTest  # 界面测试（需已连接设备）
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

首次启动会显示 **Allow access** 引导卡，授予照片权限后切换到设备照片库；拒绝则保留内置演示照片库。

## 发布与 GitHub Actions

仓库采用 `dev` → `main` 的工作流：

- 日常开发推送到 `dev`，**不会**触发任何构建或发布
- 合并到 `main` 时，`.github/workflows/release.yml` 自动读取 `gradle.properties` 中的版本号，构建 release APK 并创建 GitHub Release（tag 为 `v<versionName>`，APK 作为附件）
- 也可以在 Actions 页面手动 `Run workflow`

发版前只需改 `gradle.properties`：

```properties
photidy.versionCode=10
photidy.versionName=1.0.0
```

签名通过仓库 Secrets 注入；未配置时自动回退 debug 签名（产物名带 `-debug-signed`，仅供测试，不能上架）：

| Secret | 说明 |
| --- | --- |
| `KEYSTORE_BASE64` | keystore 文件内容的 Base64 |
| `KEYSTORE_PASSWORD` | keystore 密码 |
| `KEY_ALIAS` | 签名 alias |
| `KEY_PASSWORD` | 签名密码 |

生成 Base64（PowerShell）：

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('C:\keys\photidy-release.jks'))
```

本地构建 release：

```bash
./gradlew :app:assembleRelease
```

签名信息通过 `gradle.properties` 或命令行属性注入，不写入仓库：

```properties
photidy.keystore.path=C\:\\keys\\photidy-release.jks
photidy.keystore.storePassword=******
photidy.keystore.keyAlias=photidy
photidy.keystore.keyPassword=******
```

## 项目结构

```
app/                       应用入口、导航、主题装配、启动器图标
build-logic/               Gradle 约定插件
core/
  model/                   数据模型、筛选、统计、会话状态机（纯 Kotlin，可 JVM 单测）
  data/                    DataStore、MediaStore 查询、系统确认式媒体写入、AI 模型下载与推理
  designsystem/            设计令牌、主题、玻璃表面、流体云、通用组件
  platform/                设备能力探测、照片权限判断
  localization/            中英文资源、语言切换、格式化
feature/
  home/ filters/ organizer/ review/ insights/ favorites/ timeline/ duplicates/ search/ settings/ photodetail/
```

依赖方向：`app → feature → core:designsystem → core:model/localization`，`core:data → core:model`；feature 之间不互相依赖。

## 隐私

- INTERNET 权限仅用于下载可选的本地 AI 模型文件；照片、元数据与日志永不上传，不接入广告、分析或崩溃上报 SDK
- 照片仅通过 MediaStore 读取，不复制、不上传
- 删除与相册移动必须经 Android 系统对话框逐次确认
- 智能筛选只使用文件名、相册名、文件大小与分辨率等元数据
- 本地 AI 识别默认关闭，由用户显式开启后下载小模型，推理全程在本机完成
- 云备份与设备迁移数据全部排除

## 已知限制

- 设备库分页读取（上限 20000 张 / 200 个相册），尚未实现增量扫描；媒体索引在内存中，进程重启后重新查询
- 真实删除与移动需要 Android 11（API 30）及以上
- 智能筛选基于元数据启发式，不做像素级相似度
- 本地 AI 模型首次下载依赖网络（约 4 MB），仅做粗粒度分类，不提供人脸或文字识别
- 液态玻璃使用半透明渐变与高光模拟，未使用实时背景模糊
- 尚未提供压缩、EXIF 编辑与云同步

## 许可

[MIT](LICENSE)
