<div align="center">

<h1>ETS Viewer Android</h1>

<p>在 Android 上，清晰阅读 ETS100 云端作业与参考答案。</p>

<p>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square" alt="Android 8.0 及以上">
  <img src="https://img.shields.io/badge/Kotlin-2.1.20-7F52FF?style=flat-square" alt="Kotlin 2.1.20">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square" alt="Jetpack Compose">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0--only-64748B?style=flat-square" alt="GPL-3.0-only"></a>
</p>

<p>
  <a href="#功能概览">功能概览</a> ·
  <a href="#使用流程">使用流程</a> ·
  <a href="#开发与构建">开发与构建</a> ·
  <a href="#开源协议">开源协议</a>
</p>

</div>

---

## 功能概览

| 功能 | 说明 |
| --- | --- |
| 云端作业 | 查看当前与历史作业，按标题搜索和刷新列表 |
| 题目阅读 | 支持八类题型，按资源分页，清晰区分题目与参考答案 |
| 内容展开 | 按需查看原文、补充材料、其他答案与解析 |
| 媒体展示 | 展示题目中的图片，播放适用题型的音频和视频 |
| 导出与重试 | 导出 UTF-8 文本，重试失败资源，保留已读取内容 |
| 日常使用 | 深浅主题切换、可选记住密码、缓存清理与退出登录 |

> **开发状态**：当前版本仍在验证中，真实账号登录、官方网页验证及完整作业流程尚待真机确认。

## 使用流程

1. 输入 ETS100 手机号和密码登录，按提示完成身份验证或设备绑定。
2. 如有多个可用账户，选择需要查看的账户。
3. 在当前或历史作业中找到目标作业，打开后按资源分页阅读。
4. 需要保存时，通过阅读页右上角菜单导出答案文本。

## 开发与构建

使用 Android Studio 打开仓库根目录，准备 **JDK 17 或 21** 和 **Android SDK 36**。Gradle 版本由仓库自带的 Wrapper 管理。

### 1. 配置本地环境

由 Android Studio 设置 SDK 路径并生成 `local.properties`，然后复制服务配置模板：

```powershell
Copy-Item integration.properties.example integration.properties
```

将以下公开服务配置填入 `integration.properties`：

```properties
ets.pid=grlx
ets.signingSecret=555ffbe95ccf4e9535a110170b445ab8
ets.serial=test
```

这些值用于服务请求签名，与 APK 发布签名无关。本地配置文件、SDK 路径和发布签名密钥仍由 Git 忽略。

### 2. 构建与检查

在仓库根目录执行：

```powershell
# 构建 Debug APK
.\gradlew.bat :app:assembleDebug

# 运行应用与核心引擎的单元测试
.\gradlew.bat :app:testDebugUnitTest
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`。

macOS / Linux 下使用 `./gradlew` 替代 `.\gradlew.bat`。

<details>
<summary>Release 构建与签名</summary>

通过以下环境变量提供发布签名配置：

- `ETS_SIGNING_STORE`：密钥库路径。
- `ETS_SIGNING_STORE_PASSWORD`：密钥库密码。
- `ETS_SIGNING_ALIAS`：密钥别名。
- `ETS_SIGNING_KEY_PASSWORD`：密钥密码。

```powershell
.\gradlew.bat :app:assembleRelease
```

未提供签名配置时仅生成未签名 APK。覆盖安装需要使用与原应用一致的签名。

</details>

## 项目结构

```text
app/                            Android 界面与平台集成
engine/                         登录、资源处理与题目解析
gradle/wrapper/                 Gradle 构建工具
integration.properties.example  服务配置模板
```

## 开源协议

本项目采用 [GPL-3.0-only](LICENSE) 许可证。分发应用时，请同时提供对应的源代码和许可证。
