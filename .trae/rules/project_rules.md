# Photidy 工程规则

本文件供 Trae 持续读取。所有新功能必须遵守以下规则。

## 产品定位

- Photidy（相册轻理）：安静、精致、隐私优先的本地照片整理应用。
- 核心主张：Powerful underneath. Effortless on the surface.
- 不做复杂仪表盘、营销内容、多余悬浮按钮；照片永远是视觉中心。
- 永久免费，无账号、无广告、无订阅、无追踪。

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- Navigation Compose、Coroutines / StateFlow、ViewModel
- DataStore（设置与当前快照）
- Gradle Kotlin DSL + 版本目录 + build-logic 约定插件
- minSdk 29，compileSdk / targetSdk 36；AGP 8.13.x；Kotlin 2.3.x
- 只引入确实需要的依赖；不引入无人维护的库

## 模块边界

- `core:model`：纯 Kotlin，模型 / 会话状态机 / 筛选 / 统计，必须可 JVM 单测
- `core:data`：DataStore、仓库、演示数据；是唯一写入口
- `core:designsystem`：令牌、主题、组件、绘制
- `core:localization`：全部用户可见字符串与格式化
- `core:platform`：设备能力探测
- `feature:*`：页面与 ViewModel；不得互相依赖；导航由 `app` 串联
- 不在 Composable 中写业务规则；不建立超大 ViewModel
- 依赖注入使用 `PhotidyGraph`；如需更换为 Hilt，先记录 ADR

## 编码规范

- 遵循 Kotlin 官方风格；4 空格；命名清晰；文件职责单一
- 禁止单文件堆积整个应用；单个 Composable 保持可读，必要时拆分
- 不硬编码用户可见文本；不硬编码颜色 / 间距 / 动画参数（用令牌）
- 主线程不做昂贵图像计算与 I/O
- 不使用已弃用 API，除非有明确兼容理由并在文档说明
- 不删除测试来“通过”构建；不虚构运行结果

## 设计系统约束

- 三套视觉风格：Minimal / Liquid Glass / Fluid Cloud，由设置即时切换
- 颜色、字体、间距、圆角、阴影、动画参数全部来自 `core:designsystem`
- 照片优先：整理页不得用大面积 UI 遮挡照片
- 动画克制：卡片旋转不超过 ±8°；不使用果冻/夸张形变
- 尊重系统“减少动态效果”、省电模式、低内存：只做临时降级，不改写用户偏好
- Liquid Glass 只用于浮动层；不覆盖整屏
- 玻璃透明度必须用 `PhotidyColors.glass(scale)` 按基准等比缩放，禁止 `glassSurface.copy(alpha = ...)` 直接覆盖（深色模式会变成不透明白色卡片）
- 流体云降低饱和度，漂移缓慢；不产生持续视觉干扰

## 国际化

- 默认英文 `values/strings.xml`，简体中文 `values-zh-rCN/strings.xml`
- 所有按钮、标题、提示、空状态、错误、对话框、无障碍描述必须资源化
- 日期 / 数字 / 字节 / 百分比按当前 Locale 格式化
- 语言切换使用 AppCompat per-app locale，并持久化到 DataStore
- 语言切换不得丢失整理进度
- 适配中英文长度差异与系统字体缩放

## 隐私与数据安全

- 不申请非必要权限；仅“可选本地 AI 模型下载”允许使用 INTERNET 权限，且只允许下载模型文件，任何照片、元数据、日志都不得上传
- 不上传照片、不接入分析 SDK、不写敏感日志
- 真实照片默认只读；删除 / 移动只允许通过 `MediaEditor` 发起系统确认（`MediaStore.createDeleteRequest` / `createWriteRequest`），应用不得直接删除或移动文件，也不申请全盘文件权限
- 删除 / 移动必须：二次确认 → 系统对话框 → 仅在 `RESULT_OK` 后更新本地状态并重新扫描；拒绝或失败不得改变状态，必须给出明确提示
- 删除 / 移动后必须调用 `SessionEngine.forgetPhotos` 或等价纯函数修复会话队列与 cursor，并补充单元测试
- 本地智能识别（截图、重复等）必须是 `core:model` 中的纯函数，只使用元数据，不解码像素、不引入模型文件、不联网，并补充单元测试
- 可选的本地 AI 识别必须默认关闭、由用户显式开启后下载小模型；推理只在本机执行，不上传照片；标签映射也是纯函数并补充单元测试
- 权限必须分级：API 33+ 用 `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO`，API 34+ 支持部分授权，API ≤32 用 `READ_EXTERNAL_STORAGE`
- 权限判断统一走 `core:platform` 的 `PhotoAccess`，界面不得自行拼接权限数组
- 权限被拒或部分授权时必须有明确状态与说明，不得显示为空相册或误报照片数量
- 相册来源（演示 / 设备）必须持久化，冷启动要恢复，不得静默重置
- 扫描与解码在后台线程执行；真实媒体渲染统一使用 `PhotoImage`
- 删除类操作必须二次确认并明确说明影响范围；确认入口、深色与三种视觉风格下的确认框都要回归

## 测试与质量

- 会话状态机、筛选、统计、设备库映射的修改必须补充或更新单元测试
- 每完成一个阶段运行：`./gradlew :app:assembleDebug`、`./gradlew test`、`./gradlew lintDebug`
- 无模拟器/设备时必须在交付说明中明确“未运行”的验证项，不得声称已通过
- 复杂动画需在代码注释中说明状态转换
- 关键组件提供 `@Preview`

## 文档

- 新增架构决策更新 `ARCHITECTURE.md`
- 新增视觉令牌 / 组件更新 `DESIGN_SYSTEM.md`
- 每个版本更新 `ROADMAP.md` 与 `CHANGELOG.md`
- README 保持环境要求、构建命令与已知限制最新
