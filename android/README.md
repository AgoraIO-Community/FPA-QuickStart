# Android 示例项目

## 简介

该仓库包含了使用 FPA Android SDK 的示例项目。

## 如何运行示例项目

### 前提条件

- 真实的 Android 设备或 Android 虚拟机（Android 4.3 或更高版本）。
- Android Studio (推荐最新版)

### 运行步骤

1. 在 Android Studio 中，开启 `/android`。
2. 将项目与 Gradle 文件同步。
3. 编辑 `android\app\src\main\assets\settings-example.json` 文件。

   - `"app_id":` 填入你的 App ID。
   - `"chain id":`填入你的全链路加速服务的 chain ID。
   - `"domain":`填入你的全链路加速服务的源站域名。
   - `"port":` 填入你的全链路加速服务的端口。

   > 本示例仅支持开启 **App ID 鉴权** 的 Agora 项目。
   > 参考 [开始使用 Agora 平台](https://docs.agora.io/cn/Agora%20Platform/get_appid_token) 了解如何获取 App ID。
   > 参考 [开通全链路加速服务](https://docs.agora.io/cn/global-accelerator/enable_fpa) 了解如何获取 chain ID、源站域名和端口。

4. 构建项目，在虚拟器或真实 Android 设备中运行项目，体验加速效果。

## 反馈

如果你有任何问题或建议，可以通过 issue 的形式反馈。

## 参考文档

- [全链路加速 FPA 产品概述](https://docs.agora.io/cn/global-accelerator/agora_ga_overview)
- [FPA Android SDK API 参考](https://docs.agora.io/cn/global-accelerator/fpa_okhttp_api)

## 相关资源

- 你可以先参阅 [常见问题](https://docs.agora.io/cn/faq)
- 如果你想了解更多官方示例，可以参考 [官方 SDK 示例](https://github.com/AgoraIO)
- 如果你想了解声网 SDK 在复杂场景下的应用，可以参考 [官方场景案例](https://github.com/AgoraIO-usecase)
- 如果你想了解声网的一些社区开发者维护的项目，可以查看 [社区](https://github.com/AgoraIO-Community)
- 若遇到问题需要开发者帮助，你可以到 [开发者社区](https://rtcdeveloper.com/) 提问
- 如果需要售后技术支持, 你可以在 [Agora Dashboard](https://dashboard.agora.io) 提交工单

## 代码许可

示例项目遵守 MIT 许可证。