# lolive

Android 直播观看应用：平台切换、直播列表浏览、详情播放（HLS/RTMP）、网络状态监听、GitHub Release 更新检测。

| 项 | 值 |
|---|---|
| 包名 | `com.ho.lolive` |
| 版本 | versionName `1.21` / versionCode `23` |
| minSdk / targetSdk | 24 / 34 |
| JDK | **21**（`settings.gradle.kts` 强制校验） |

## 功能

- 平台列表与底部弹窗切换
- 按当前平台分页展示直播间，支持标题搜索
- 详情页 ExoPlayer 播放（HLS / RTMP）
- 手势：左滑下一个、右滑上一个；左半屏调亮度、右半屏调音量；点击显隐控制栏
- 断网提示与有条件重试（避免正常播放被无故打断）
- GitHub Release 版本对比与更新弹窗

## 技术栈

| 领域 | 技术 |
|---|---|
| 语言 | Kotlin 1.9.x，C++（CMake / JNI） |
| UI | 传统 XML + View + Material Components（**无 Compose**） |
| 架构 | 分层 `presentation / domain / data` + MVVM |
| DI | Hilt |
| 网络 | Retrofit + OkHttp + Kotlinx Serialization |
| 本地 | Room + Paging 3 |
| 播放 | Media3 ExoPlayer 1.3.x（HLS + RTMP） |
| 图片 | Coil |
| 质量 | ktlint、detekt、JUnit4、Truth、MockK |

## 架构

```
Presentation (MainActivity / DetailActivity / ViewModel)
      ↓
Domain (Model / UseCase / LiveRepository 接口)
      ↑
Data (Repository 实现 / Room DAO / Retrofit / DTO)
```

主要入口：

- `MainActivity`：首页列表
- `DetailActivity`：播放详情
- 导航：`Intent` extras（`DetailActivity.createIntent`）

Native（`app/src/main/cpp/native_endpoints.cpp`）用 XOR 在运行时解码 API 基址与路径，经 `NativeEndpointBridge` 提供给网络层。`BuildConfig.BASE_URL` 仅为 Retrofit 占位（当前 `https://localhost/`），**真实请求 URL 来自 JNI**。

## 环境要求

- Android Studio（建议较新稳定版）
- **JDK 21**（本地与 CI 一致；JDK 17/26 会在配置阶段被拒绝）
- Android SDK；编译 Native 时需要 NDK `29.0.14206865`

## 快速开始

1. 克隆仓库，用 Android Studio 打开根目录。
2. 确认本机 `JAVA_HOME` 指向 JDK 21。
3. 同步 Gradle。
4. 运行 Debug：

```powershell
# Windows
.\gradlew.bat :app:assembleDebug

# macOS / Linux
./gradlew :app:assembleDebug
```

Debug APK 目录：`app/build/outputs/apk/debug/`  
命名示例：`lolive-v1.21-23-debug-arm64-v8a.apk`（含 ABI 分包 + universal）。

## 本地 Release 签名

**不要**把 `.jks` / `.keystore` 提交进仓库。本地通过环境变量注入：

```powershell
$env:SIGNING_STORE_FILE="D:\secrets\lolive-release.jks"
$env:SIGNING_STORE_PASSWORD="你的密码"
$env:SIGNING_KEY_ALIAS="lolive"
$env:SIGNING_KEY_PASSWORD="你的密码"

.\gradlew.bat :app:assembleRelease
```

## GitHub Actions 发版

工作流：`.github/workflows/manual-release-apk.yml`（`workflow_dispatch` 手动触发）。

流程概要：

1. 校验 Native（源码 CMake 或预编译 `jniLibs`）
2. 从 Secrets 解码临时 keystore
3. 自动递增 `versionCode` / `versionName` 并 push
4. 构建 Release ABI 分包 APK
5. 打 Tag、创建 GitHub Release 并上传 APK

### 需要的 Repository Secrets

在仓库 **Settings → Secrets and variables → Actions** 配置（**不要**把 jks 提交到代码库）：

| Secret | 说明 |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | keystore 文件的 base64 全文 |
| `SIGNING_STORE_PASSWORD` | keystore 密码 |
| `SIGNING_KEY_ALIAS` | key 别名 |
| `SIGNING_KEY_PASSWORD` | key 密码 |

本机生成 base64（PowerShell）：

```powershell
[Convert]::ToBase64String(
  [IO.File]::ReadAllBytes("D:\secrets\lolive-release.jks")
) | Set-Clipboard
```

将剪贴板内容粘贴为 `SIGNING_KEYSTORE_BASE64` 的值即可。CI 仅在 runner 临时目录解码，job 结束后文件销毁。

## Native 构建

- 存在 `app/src/main/cpp/CMakeLists.txt` 且未指定预编译时：走 CMake 编译
- 使用预编译 `.so`：

```powershell
.\gradlew.bat :app:assembleRelease -PusePrebuiltNative=true
```

- 导出预编译库到 `app/src/main/jniLibs`：

```bat
build_native_so.bat
```

说明：`*.so` 默认被 gitignore，不进入仓库；CI 有源码则现场编译。

## 常用命令

```powershell
# 单元测试
.\gradlew.bat test

# 指定 UseCase 测试
.\gradlew.bat test --tests "com.ho.lolive.domain.usecase.*"

# 代码检查
.\gradlew.bat ktlintCheck detekt
```

## 目录结构（摘要）

```
app/src/main/java/com/ho/lolive/
├── MainActivity.kt / DetailActivity.kt
├── core/           # AppResult、Logger、网络监听、Native 桥
├── data/           # Room、Retrofit、Repository 实现
├── domain/         # Model、Repository 接口、UseCase
├── presentation/   # Home/Detail ViewModel、XML 适配器
└── di/             # Hilt Module

app/src/main/cpp/   # native_endpoints.cpp + CMake
.github/workflows/  # 手动 Release 工作流
```

## 说明与边界

- 上游直播 API 对 keep-alive 不稳定，OkHttp 对目标主机使用 `Connection: close`
- 房间列表按 **当前选中平台** 过滤；搜索仅在当前平台内匹配标题
- 房间 ID 使用平台 + 标题 + 稳定化后的流地址（去掉 query/fragment），降低 CDN 签名参数导致的 ID 漂移
- 单平台仅 1 个房间时，上一个/下一个不可用（不自循环重载）
- XOR 混淆端点仅提高静态提取成本，**不能**当作完整安全方案

## License

见仓库内 License 文件（若有）。未声明时默认仅供学习与个人使用。
