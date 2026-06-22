@echo off
setlocal EnableExtensions EnableDelayedExpansion

cd /d "%~dp0"

if not exist "gradlew.bat" (
  echo [ERROR] gradlew.bat not found. Please run this script from project root.
  exit /b 1
)

if not exist "app\src\main\cpp\CMakeLists.txt" (
  echo [ERROR] app\src\main\cpp\CMakeLists.txt not found.
  echo         Native source is missing. Cannot build .so from cpp.
  exit /b 1
)

echo [INFO] Building native libraries and exporting to app\src\main\jniLibs ...
call gradlew.bat :app:exportNativeSo --no-daemon --no-configuration-cache
if errorlevel 1 (
  echo [ERROR] Gradle task failed: :app:exportNativeSo
  exit /b 1
)

set "ABIS=armeabi-v7a arm64-v8a x86 x86_64"
for %%A in (%ABIS%) do (
  if not exist "app\src\main\jniLibs\%%A\liblolive_native.so" (
    echo [ERROR] Missing output: app\src\main\jniLibs\%%A\liblolive_native.so
    exit /b 1
  )
)

echo [OK] Native .so export complete:
for %%A in (%ABIS%) do (
  for %%F in ("app\src\main\jniLibs\%%A\liblolive_native.so") do (
    echo   %%~fF  ^(%%~zF bytes^)
  )
)

exit /b 0
