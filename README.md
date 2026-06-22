# lolive

lolive 是一个 Android 直播观看 Demo，包含直播列表、详情播放、网络状态监听和应用更新检测等能力。

## 技术栈

- 语言与平台: Kotlin, Android SDK (minSdk 24 / targetSdk 34)
- UI: Jetpack Compose, Navigation Compose, Material 3
- 架构: 分层结构（`presentation / domain / data`）+ MVVM
- 依赖注入: Hilt
- 网络: Retrofit + OkHttp + Kotlinx Serialization
- 本地存储: Room + Paging 3
- 播放: Media3 ExoPlayer（HLS，含 RTMP datasource 依赖）
- Native: C++(CMake) + JNI（`liblolive_native.so`）
- 质量保障: JUnit4, MockK, Truth, ktlint, detekt

## 运行环境

- Android Studio（建议 Giraffe+）
- JDK 17（CI 使用 JDK 21）
- Android SDK / 构建工具已安装
- 如需编译 Native：NDK `29.0.14206865`

## 快速开始

1. 克隆项目并使用 Android Studio 打开根目录。
2. 同步 Gradle。
3. 配置后端地址：编辑 `app/build.gradle.kts` 中的 `BASE_URL`。
4. 构建并运行：

```bash
# Windows
.\gradlew.bat :app:assembleDebug

# macOS / Linux
./gradlew :app:assembleDebug
```

## 关键配置

- `BASE_URL`：接口基础地址
- `ENABLE_HTTP_LOG`：是否开启 HTTP 日志（Debug 默认 `true`，Release 默认 `false`）
- Release 签名环境变量（本地或 CI）：
  - `SIGNING_STORE_FILE`
  - `SIGNING_STORE_PASSWORD`
  - `SIGNING_KEY_ALIAS`
  - `SIGNING_KEY_PASSWORD`

## 部署与发布

### 本地部署（APK）

```bash
# Debug 包
./gradlew :app:assembleDebug

# Release 包（需配置签名环境变量）
./gradlew :app:assembleRelease
```

- 产物目录：`app/build/outputs/apk/`
- 已开启 ABI 分包，默认输出 `armeabi-v7a / arm64-v8a / x86 / x86_64 / universal`

### GitHub Actions 自动发布

仓库内置工作流：`.github/workflows/manual-release-apk.yml`（手动触发 `workflow_dispatch`）。

发布流程：
1. 校验 Native 产物
2. 读取并注入签名信息
3. 自动递增 `versionCode` / `versionName`
4. 构建 Release APK
5. 自动打 Tag 并创建 GitHub Release，上传各 ABI APK

需要配置的 GitHub Secrets：
- `SIGNING_KEYSTORE_BASE64`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

## Native 说明

- 默认存在 `app/src/main/cpp/CMakeLists.txt` 时，构建时会编译 Native 代码。
- 如需使用预编译 `.so`，可通过 Gradle 参数启用：

```bash
./gradlew :app:assembleRelease -PusePrebuiltNative=true
```

- 可使用脚本导出 `.so` 到 `app/src/main/jniLibs`：

```bat
build_native_so.bat
```

## 常用命令

```bash
# 单元测试
./gradlew test

# 代码检查
./gradlew ktlintCheck detekt
```
