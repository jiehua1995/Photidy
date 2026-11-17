<p align="center">
  <img src="docs/assets/logo.svg" width="112" alt="Photidy logo">
</p>

<h1 align="center">Photidy · 相册轻理</h1>

<p align="center">A quieter way to organize your photos.<br>轻轻整理，留住美好。</p>

<p align="center">
  <a href="https://github.com/jiehua1995/Photidy/releases/latest"><img alt="Latest release" src="https://img.shields.io/github/v/release/jiehua1995/Photidy?label=download&color=6b8f7a"></a>
  <a href="LICENSE"><img alt="License" src="https://img.shields.io/github/license/jiehua1995/Photidy?color=6b8f7a"></a>
  <img alt="Android 10+" src="https://img.shields.io/badge/Android-10%2B-6b8f7a">
  <img alt="Kotlin & Compose" src="https://img.shields.io/badge/Kotlin-Compose-6b8f7a">
  <img alt="On-device" src="https://img.shields.io/badge/privacy-on--device-6b8f7a">
</p>

Photidy 是一款安静、精致的本地照片整理应用。它不催你清空相册，只是把“要不要留下这张”变成一次舒服的滑动。删除与移动都由 Android 系统对话框确认，照片始终留在你的设备上。

## 下载

前往 [Releases](https://github.com/jiehua1995/Photidy/releases/latest) 下载最新 APK，安装时允许“来自此来源的应用”即可。支持 Android 10（API 29）及以上。

## 能做什么

- **四向滑动整理**：方向、阈值、回弹与撤销都经过调校，四个方向可自定义动作
- **真实照片库**：读取本机照片与视频，按相册聚合，支持完整 / 部分 / 拒绝三档权限
- **滑动之外的整理**：收藏视图、批量多选、按月份的时间线、随手搜索文件名或日期
- **重复照片对比**：把大小与分辨率相同的照片并排摆出来，选一张保留，其余加入待删除
- **本地智能识别**：可选的 4 MB 小模型，完全在本机给出食物 / 宠物 / 文档 / 自然 / 建筑标签
- **回顾与洞察**：照片构成、每月分布、相册占用与可回收空间，全部来自本地元数据
- **三种视觉风格**：Minimal / Liquid Glass / Fluid Cloud，支持深色模式与减少动态效果
- **多语言**：简体中文、繁體中文、English、Deutsch、日本語、한국어、Español、Français、Italiano

## 安全吗

- 不注册账号，不接入广告、分析或崩溃上报 SDK
- 照片只通过 Android MediaStore 读取，不复制、不上传、不写敏感日志
- 删除与相册移动必须经系统对话框逐次确认；应用不申请全盘文件权限
- 智能筛选只使用文件名、大小、分辨率等元数据，不解码像素
- 本地 AI 默认关闭；开启后仅下载一次模型，推理全程在设备上完成
- 唯一使用的网络权限只用于下载这个可选模型

系统回收站中的照片仍可在 Android 允许的时间内恢复，应用会在待删除页提示剩余天数。

## 参与开发

构建步骤、模块结构、架构约定与发布流程见 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)。

## 许可

[MIT](LICENSE)
