# Changelog

## Unreleased

### Added

- 筛选页新增“随机顺序”，每次开始整理时打乱一次队列
- 整理页顶部新增“重新选择文件夹”入口，可随时退出当前范围重新筛选
- 新整理默认只包含**未整理**的照片，已整理的照片不会再次出现（可在筛选页切回“全部照片”）

### Changed

- 整理页滑动反馈改为大尺寸图标徽章（76dp 圆形图标 + 文字），并移动到手势不易遮挡的一侧
- 底部操作条图标放大到 48dp / 24dp
- 液态玻璃新增玻璃背景光池，卡片透光更明显；流体云提高云层可见度
- 效果强度增加 32% 下限，避免调低后液态玻璃与流体云看起来和纯净模式一样

### Fixed

- 效果强度被调到很低时，液态玻璃与流体云几乎无法察觉

## 1.0.0 — 2026-11-15

首个可发布版本：在真实照片库之上补齐**经系统确认的删除与相册移动**，并加入本地智能筛选与发布配置。

### Added

- 真实删除（Android 11+）
  - `core:platform` 新增 `MediaWriteAccess`：判断系统是否支持确认式写入
  - `core:data` 新增 `MediaEditor`：`MediaStore.createDeleteRequest` / `createWriteRequest` 生成系统确认 `PendingIntent`，`moveToAlbum` 通过 `IS_PENDING` + `RELATIVE_PATH` 完成真实移动
  - 待删除清单支持多选删除：二次确认对话框 → 系统对话框确认 → 真实删除 → 列表与会话同步清理
  - `SessionEngine.forgetPhotos`：删除后裁剪队列、平移 cursor、清理撤销栈（纯函数 + 4 个单测）
- 真实相册移动
  - 整理页选择相册后发起系统写入授权，成功后更新 `RELATIVE_PATH` 并重新扫描
  - 演示照片、缺少 `RELATIVE_PATH` 的相册与 Android 10 及以下继续走“仅记录计划”的路径
- 本地智能筛选（`core:model` 的 `SmartInsights`，纯函数）
  - 截图识别：文件名 / 相册名关键词匹配，排除视频
  - 可能重复：按文件大小与分辨率精确分组
  - 筛选页新增“智能筛选”分区，可与相册 / 媒体类型 / 时间叠加
- 发布配置
  - release 启用 R8 混淆与资源压缩，`proguard-rules.pro` 落地
  - 发布签名通过 `gradle.properties`（`photidy.keystore.*`）注入；未配置时回退 debug 签名以便冒烟测试
  - 版本号提升至 1.0.0（versionCode 10）
- 启动与刷新
  - 冷启动时先渲染主题背景与应用标记，等 DataStore 恢复设置后再进入首页，避免演示库与默认主题闪现
  - 应用回到前台时重新读取 MediaStore，外部新增或删除的照片无需重启即可看到

### Changed

- `PhotoItem` 的 `contentUri` 改为按媒体类型拼接 `MediaStore.Images.Media` / `Video.Media` 的类型化 URI；此前使用 `MediaStore.Files` 的集合 URI 会被 `createDeleteRequest` 拒绝
- `Album` 新增 `relativePath`，由 `MediaLibrary` 从 `MediaStore.MediaColumns.RELATIVE_PATH` 读取
- 待删除清单的安全提示改为“在系统确认之前，不会删除任何文件”
- 详情页提示改为“只读预览，不会修改你设备上的任何文件”
- 待删除清单底部操作条改为“全选 / 移出 N / 删除 N”，并修正内容区高度导致操作条被遮挡的问题
- 设置页隐私说明更新为：删除或移动照片时系统会再次确认
- `OrganizerRoute` 使用 `LocalResources` 读取文案，避免配置变化后显示旧语言

### Fixed

- 系统删除对话框不弹出、直接提示需要 Android 11（集合 URI 类型不合法）
- 删除 / 移动后会话队列与 cursor 残留已不存在的照片

### Notes

- 删除与移动始终由系统对话框二次确认；应用不申请全盘文件访问权限，也不直接删除文件。
- 智能筛选仅使用元数据，不解码像素、不联网、不引入模型文件。
- 已知限制见 README。

### Repository

- 新增 `LICENSE`（MIT）与 `.gitattributes`（统一换行、二进制标记）
- `.gitignore` 覆盖所有模块的 `build/`、`.kotlin/`、IDE 配置、签名文件与 `dist/`，仓库只保留源码与必要文档
- 新增 `.github/workflows/release.yml`：合并到 `main` 时读取 `gradle.properties` 版本号，构建 release APK 并创建 GitHub Release；`dev` 分支不触发；签名密钥缺失时回退 debug 签名
- 文档精简为 README + CHANGELOG，移除面向单机环境的测试手册与过程文档

## 0.2.0 — 2026-11-14

真实照片库（只读）。用户可以整理设备上已有的照片与视频，所有操作仍只写入本地计划。

### Added

- 设备照片库
  - `core:platform` 新增 `PhotoAccess`：按系统版本组合只读权限，区分完整授权与部分授权
  - `core:data` 新增 `MediaLibrary`：MediaStore 只读查询照片与视频，按 bucket 聚合相册
  - 纯函数 `buildDeviceLibrary` 负责行到模型的映射，便于 JVM 单测
  - `PhotoRepository` 支持演示库 / 设备库切换、异步扫描与 `LibraryState` 状态流
- 图片加载
  - 接入 Coil 2.7.0（compose + video），视频使用首帧解码
  - 新增 `PhotoImage`：真实媒体走 Coil，演示照片继续使用程序化绘制
- 权限与来源选择
  - Manifest 增加 `READ_MEDIA_IMAGES`、`READ_MEDIA_VIDEO`、`READ_MEDIA_VISUAL_USER_SELECTED`、`READ_EXTERNAL_STORAGE`（≤ API 32）
  - 首页新增“使用你自己的照片”引导卡，包含拒绝提示与保留演示库选项
  - 设置新增照片来源分区：设备库 / 演示库单选、照片数量、重新扫描、允许访问
  - `LibraryMode` 与选择状态持久化到 DataStore，冷启动后自动恢复
- 国际化
  - 中英文新增资料库来源、引导卡、设置分区、设备照片复数文案
  - `AlbumNames` 支持未知 bucket 的“其他”回退
- 单元测试：设备库映射、视频识别、空名称回退、相册聚合与排序、limit 截断、未知 bucket

### Changed

- `PhotoItem` 新增 `contentUri` / `albumTitle`，宽高比计算增加零值保护
- 真实相册优先显示 MediaStore 的 bucket 名称，演示相册继续使用本地化名称
- “演示照片”安全提示改为通用只读提示（`review_safety_note` / `detail_safety_note`）
- 隐私说明更新：仅申请读取权限，照片只在本机读取
- 版本号提升至 0.2.0（versionCode 2）

### Fixed

- 照片权限被拒或仅部分授权时，界面不会误报完整照片库
- 启动时恢复持久化的照片来源，避免设备库被静默重置为演示库

### Notes

- 本阶段仍不删除、不移动、不修改任何真实文件。
- 设备库一次性读取上限 8000 张 / 40 个相册，分页、增量扫描与 Room 索引留待后续版本。

## 0.1.0 — 2026-11-14

首个可运行阶段：Photidy UI 原型。

### Added

- 多模块 Android 工程（app / core / feature），Gradle Kotlin DSL、版本目录、build-logic 约定插件。
- 设计系统 `core:designsystem`
  - 颜色、字体、间距、圆角、动画令牌
  - Minimal / Liquid Glass / Fluid Cloud 三种视觉风格
  - 玻璃表面、流体云背景、程序化演示照片（10 种场景）
  - 按钮、芯片、分段控件、滑块、开关、设置行、空状态、指标卡、确认对话框等组件
- 数据层 `core:data`
  - 56 条确定性演示照片（多分辨率、横竖图、视频、7 个相册）
  - DataStore 设置持久化
  - DataStore JSON 快照保存照片状态、整理会话与撤销栈
  - 轻量服务定位器 `PhotidyGraph`
- 会话状态机 `core:model`
  - 四向手势动作映射、提交、跳过、撤销、完成
  - 筛选（相册 / 媒体类型 / 时间 / 状态 / 排序）
  - 统计（整理数量、待删除、收藏、月度分布、本次会话）
- 页面
  - 首页：继续/开始整理、待删除、回顾、设置、照片库概览
  - 筛选：相册、媒体类型、时间、排序、整理状态、匹配数量
  - 整理：拖动跟随、旋转、方向动作预览、阈值判定、飞出/回弹、按钮替代、撤销、相册选择、完成页、平板右侧队列
  - 待删除清单：网格、多选、移出、清空确认、预计空间
  - 回顾：指标、完成比例环、12 个月柱状图、会话摘要
  - 设置：主题、视觉风格、效果强度、动画风格与开关、触觉反馈、四向手势、手势测试、语言、关于
  - 详情：大图、元数据、收藏/删除/保留（演示状态）
- 国际化
  - 英文默认资源 + 简体中文资源，全部界面文案资源化
  - AppCompat per-app locale，应用内切换与持久化
  - 本地化日期、数字、字节、百分比
- 自适应与无障碍
  - 手机 / 平板断点布局，双栏内容
  - 48dp 触控区域、内容描述、selectable/toggleable 语义
  - 尊重系统减少动画设置，低端设备效果降级
- 单元测试：会话状态机、筛选、统计
- 文档：README、PRODUCT、ARCHITECTURE、DESIGN_SYSTEM、ROADMAP、CHANGELOG、Trae 规则

### Fixed (device verification pass)

- 语言切换在冷启动时被占位默认值覆盖，导致 Activity 反复重建（黑屏）。现在只在 DataStore 真正读取到设置后才应用 per-app locale。
- Liquid Glass 在深色模式下把半透明玻璃的 alpha 直接替换为 0.72+，卡片变成不透明白色、文字不可读。玻璃填充改为按基础不透明度等比缩放（`PhotidyColors.glass`）。
- 整理页进度轨道缺少 `fillMaxWidth()`，宽度被填充条撑开，轨道完全不可见。
- 玻璃边框在深色模式下的 alpha 叠加不一致，统一使用 `glassBorder` 令牌。

### Notes

- 所有删除、相册移动、收藏、保留均为演示数据操作，不读取或修改真实照片。
- 未申请任何权限，无网络、账号、广告、分析 SDK。
- Liquid Glass 使用渐变与高光模拟，未启用实时背景模糊。
- 本阶段未接入 MediaStore / Room / Coil / Hilt，原因见 ARCHITECTURE.md 的权衡表。
