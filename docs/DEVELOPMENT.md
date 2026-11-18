# Photidy 开发说明

面向开发者。使用者文档见 [README](../README.md)。

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

`local.properties`（不提交）需要指向本机 SDK，用 Android Studio 打开项目时会自动生成：

```properties
sdk.dir=C\:\\Android\\sdk
```

## 构建与验证

```bash
./gradlew :app:assembleDebug    # 调试包
./gradlew :app:installDebug     # 安装到设备/模拟器
./gradlew test                  # JVM 单元测试
./gradlew lintDebug             # 静态检查
./gradlew connectedDebugAndroidTest  # 界面测试（需已连接设备）
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

首次启动会显示 **Allow access** 引导卡，授予照片权限后切换到设备照片库；拒绝则保留内置演示照片库。

## 技术栈

| 层 | 选型 |
| --- | --- |
| 语言与界面 | Kotlin 2.3、Jetpack Compose、Material 3 |
| 架构 | 多模块、Navigation Compose、Coroutines / StateFlow、ViewModel |
| 本地存储 | DataStore（设置与当前整理快照）、MediaStore（只读媒体） |
| 图片 | Coil（网格按目标尺寸解码缩略图） |
| 本地 AI | TensorFlow Lite + 三档量化模型（MobileNet V1 / EfficientNet-Lite0 / Lite2，约 4–8 MB），可选下载、默认关闭 |
| 构建 | Gradle Kotlin DSL、版本目录、`build-logic` 约定插件 |

业务规则尽量放在 `core:model` 的纯函数里（去重分组、月份分组、统计、会话队列），因此大部分逻辑都能在没有设备的 JVM 上直接测试。所有整理状态只有 `core:data` 一个写入口，页面之间互不依赖，导航由 `app` 串联。

## 模块结构

```
app/                       应用入口、底部导航、主题装配、启动器图标
build-logic/               Gradle 约定插件
core/
  model/                   数据模型、筛选、统计、去重、时间线、会话状态机（纯 Kotlin）
  data/                    DataStore、MediaStore 查询、系统确认式写入、AI 模型与推理
  designsystem/            设计令牌、主题、玻璃表面、流体云、通用组件与导航栏
  platform/                设备能力探测、照片权限判断
  localization/            多语言资源、语言切换、格式化
feature/
  home/ filters/ organizer/ review/ insights/ favorites/ timeline/ duplicates/
  search/ settings/ photodetail/
```

依赖方向：`app → feature → core:designsystem → core:model/localization`，`core:data → core:model`；feature 之间不互相依赖，导航由 `app` 串联。

## 架构约定

- **唯一写入口**：所有整理状态写入 `PhotoRepository`，界面只读 Flow。
- **纯函数优先**：筛选、去重分组、时间线分组、统计、会话状态机都在 `core:model`，可直接 JVM 单测。
- **系统确认式文件操作**：删除 / 移动只通过 `MediaEditor` 发起 `MediaStore.createDeleteRequest` / `createWriteRequest`，应用不直接改动文件。
- **本地 AI**：模型文件下载到应用私有目录，推理用 TensorFlow Lite 在本机完成；`core:model` 里的标签映射是纯函数。可选 AI 默认关闭。三档模型（轻量 MobileNet V1、均衡 EfficientNet-Lite0、精准 EfficientNet-Lite2）共用同一套推理代码，输入尺寸与张量类型在运行时从模型读取；标签来自内置的 ImageNet 1000 类列表。切换档位会丢弃旧档位标签并用新模型重新识别。
- **导航**：五个底部标签（整理 / 照片 / 搜索 / 收藏 / 回顾）为顶层路由，整理、详情、筛选等页面以全屏路由压栈，隐藏底栏。
- **动效**：按压缩放、卡片动效与底栏选中位移都遵循 `PhotidyTheme.animationsEnabled`，关闭动画时立即到位。
- **视觉令牌**：颜色、间距、圆角、阴影、动效参数统一来自 `core:designsystem`，不在页面硬编码。

## 测试要求

- 会话状态机、筛选、统计、设备库映射、去重与时间线的修改必须补充或更新单元测试
- 每完成一个阶段运行 `./gradlew :app:assembleDebug`、`./gradlew test`、`./gradlew lintDebug`
- 无模拟器/设备时，交付说明中必须写明“未运行”的验证项
- 关键组件提供 `@Preview`

## 发布

仓库采用 `dev` → `main` 工作流：

- 日常开发推送到 `dev`，不触发构建或发布
- 合并到 `main` 时，`.github/workflows/release.yml` 读取 `gradle.properties` 的版本号，构建 release APK 并创建 GitHub Release（tag 为 `v<versionName>`）
- 也可以在 Actions 页面手动 `Run workflow`

发版前修改：

```properties
photidy.versionCode=11
photidy.versionName=1.1.0
```

签名通过仓库 Secrets 注入；未配置时自动回退 debug 签名（产物名带 `-debug-signed`，仅供测试）：

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

## 文档

- `README.md`：面向使用者的中文说明（功能、下载、隐私）
- `README.en.md`：面向使用者的英文说明
- `docs/DEVELOPMENT.md`：本文件，技术细节与发布流程
- `CHANGELOG.md`：每个版本的变更
