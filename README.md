# MEGA Android Client

A fully-featured client to access your Cloud Storage provided by MEGA.

This document provides step-by-step instructions to build the application on Linux and MacOS using Android Studio.

## 1. Setup Development Environment

Install the following prerequisites:

- [Android Studio](https://developer.android.com/studio)
- [Android SDK Tools](https://developer.android.com/studio#Other)
- [Android NDK](https://developer.android.com/ndk/downloads)
- JDK 21

## 2. Get the Source Code

Clone the repository with all submodules:

```bash
git clone --recursive https://github.com/meganz/android.git
```

## 3. NDK Configuration

### 3.1 Linux

1. Install [Android NDK r27b](https://dl.google.com/android/repository/android-ndk-r27b-linux.zip) (latest version tested: NDK r27b, version number: 27.1.12297006).

2. Export the `NDK_ROOT` environment variable or create a symbolic link at `${HOME}/android-ndk` pointing to your Android NDK installation path:

```bash
export NDK_ROOT=/path/to/ndk
ln -s /path/to/ndk ${HOME}/android-ndk
```

### 3.2 MacOS

1. Install NDK r27b using Android Studio by following [these instructions](https://developer.android.com/studio/projects/install-ndk#specific-version). Pay attention to the bottom-right `Show Package Details` checkbox to display available versions. Latest version tested: NDK r27b, version number: 27.1.12297006.

2. Export the `NDK_ROOT` environment variable or create a symbolic link at `${HOME}/android-ndk` pointing to your Android NDK installation path:

```bash
export NDK_ROOT="/Users/${USER}/Library/Android/sdk/ndk/27.1.12297006"
ln -s /path/to/ndk ${HOME}/android-ndk
```

## 4. ANDROID_HOME Configuration

### 4.1 Linux

Export the `ANDROID_HOME` environment variable or create a symbolic link at `${HOME}/android-sdk` pointing to your Android SDK installation path:

```bash
export ANDROID_HOME=/path/to/sdk
ln -s /path/to/sdk ${HOME}/android-sdk
```

### 4.2 MacOS

Export the `ANDROID_HOME` environment variable or create a symbolic link at `${HOME}/android-sdk` pointing to your Android SDK installation path:

```bash
export ANDROID_HOME="/Users/${USER}/Library/Android/sdk/"
ln -s /path/to/sdk ${HOME}/android-sdk
```

## 5. JAVA_HOME Configuration

### 5.1 Linux

Export the `JAVA_HOME` environment variable or create a symbolic link at `${HOME}/android-java` pointing to your Java installation path.

You can find the Java path in Android Studio at `Preferences > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK (default)`.

```bash
export JAVA_HOME=/path/to/jdk
ln -s /path/to/jdk ${HOME}/android-java
```

### 5.2 MacOS

Export the `JAVA_HOME` environment variable or create a symbolic link at `${HOME}/android-java` pointing to your Java installation path.

You can find the Java path in Android Studio at `Preferences > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK (default)`.

Default MacOS path:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jre/Contents/Home"
ln -s /path/to/jdk ${HOME}/android-java
```

## 6. Build SDK

### 6.1 For Linux Users

#### 6.1.1 Install Build Tool Dependencies

```bash
sudo apt install python3-pkg-resources libglib2.0-dev libgtk-3-dev libasound2-dev libpulse-dev
```

#### 6.1.2 Set Up VCPKG

Clone the VCPKG repository next to the Android repository folder. If you already have a local VCPKG clone, you can skip this step and use your existing VCPKG installation.

```bash
git clone https://github.com/microsoft/vcpkg
export VCPKG_ROOT=path/to/your/vcpkg/folder
```

#### 6.1.3 Build SDK

Build the SDK by running `./cmake.sh` from `sdk/src/main/jni/`.

**IMPORTANT:** Verify that the build process completes successfully. It should finish with the **Task finished OK** message. If it doesn't, modify the `LOG_FILE` variable in `cmake.sh` from `/dev/stdout` to a text file and run `./cmake.sh` again to view the build errors.

### 6.2 For Apple Silicon MacOS Users

> **⚠️ Deprecation Notice**
>
> This build process is **obsolete**, **no longer maintained**, and **not supported**.
>
> It is kept **only for Apple Silicon (ARM64) MacOS users**, because the **current SDK CMake-based build flow does not support Apple Silicon MacOS**.
>
> **Last verified on:** 2025-12-18

#### 6.2.1 Download WebRTC Files

1. Download the WebRTC files from: https://mega.nz/file/N2k2XRaA#bS9iudrjiULmMaGbBKErsYosELbnU22b8Zj213Ti1nE
2. Uncompress the archive and place the `webrtc` folder in `sdk/src/main/jni/megachat/`.

**Note:** The WebRTC download link may change over time. Please verify it matches the one specified in `build.sh`.

#### 6.2.2 Prerequisites for Running the Build Script

Before running the SDK build script, install the required dependencies via Homebrew:

```bash
brew install bash gnu-sed gnu-tar autoconf automake cmake coreutils libtool swig wget xz python3
```

Then reboot MacOS to ensure the newly installed bash (v5.x) overrides the default v3.x in PATH.

Edit your PATH environment variable (ensure GNU paths are set up before `$PATH):

- **For Intel chip**, add the following lines to `~/.zshrc`:

```bash
export PATH="/usr/local/opt/gnu-tar/libexec/gnubin:$PATH"
export PATH="/usr/local/opt/gnu-sed/libexec/gnubin:$PATH"
```

- **For Apple Silicon**, add the following lines to `~/.zshrc`:

```bash
export PATH="/opt/homebrew/opt/gnu-tar/libexec/gnubin:$PATH"
export PATH="/opt/homebrew/opt/gnu-sed/libexec/gnubin:$PATH"
ln -s /opt/homebrew/bin/python3 /opt/homebrew/bin/python
```

Then install CMake version 3.22.1 in `Android Studio > Tools > SDK Manager > SDK Tools > CMake`.

You must check the `Show Package Details` checkbox to display this specific version. After installation, add the following line to `~/.zshrc`:

```bash
export PATH="/Users/${USER}/Library/Android/sdk/cmake/3.22.1/bin:$PATH"
```

#### 6.2.3 Running the Build Script

Build the SDK by running `./build.sh all` from `sdk/src/main/jni/`. You can also run `./build.sh clean` to clean the previous configuration.

**IMPORTANT:** Verify that the build process completes successfully. It should finish with the **Task finished OK** message. If it doesn't, modify the `LOG_FILE` variable in `build.sh` from `/dev/null` to a text file and run `./build.sh all` again to view the build errors.

If you encounter an error (seen in the log file) due to licenses not being accepted, you can read and accept the licenses using the sdkmanager command-line tool:

```bash
/path-to-cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_HOME --licenses
```

## 7. Build and Run Android App

### 7.1 Download Required Files

Download the required files from: https://mega.nz/#!1tcl3CrL!i23zkmx7ibnYy34HQdsOOFAPOqQuTo1-2iZ5qFlU7-k

Uncompress the archive and place the `debug` and `release` folders in `app/src/`.

### 7.2 Disable Pre-built SDK

1. Open `buildSrc/src/main/kotlin/mega/privacy/android/build/Util.kt` and change the `shouldUsePrebuiltSdk()` method to:

```kotlin
fun shouldUsePrebuiltSdk(): Boolean = false
//        System.getenv("USE_PREBUILT_SDK")?.let { it != "false" } ?: true
```

2. Open `settings.gradle.kts` and change the `shouldUsePrebuiltSdk()` method to:

```kotlin
fun shouldUsePrebuiltSdk(): Boolean = false
//        System.getenv("USE_PREBUILT_SDK")?.let { it != "false" } ?: true
```

### 7.3 Build Mobile Analytics Library Locally

**Note:** You need to occasionally redo this section to ensure the latest analytics library is used.

1. Download and build the [Mobile Analytics](https://github.com/meganz/mobile-analytics) source code:

```bash
git clone --recursive https://github.com/meganz/mobile-analytics.git
cd mobile-analytics
git checkout main
./gradlew --no-daemon assembleRelease
```

2. Copy the following generated libraries to the root of the MEGA codebase:
   - `shared/build/outputs/aar/shared-release.aar`
   - `analytics-core/build/outputs/aar/analytics-core-release.aar`
   - `analytics-annotations/build/outputs/aar/analytics-annotations-release.aar`

3. Modify MEGA code to depend on local AAR files:
   - Search for `implementation(lib.mega.analytics)` throughout the project and replace all occurrences with the code below. Note: You may need to add `..` to the path if the `build.gradle.kts` is in a subproject.

```kotlin
//    implementation(lib.mega.analytics)
implementation(files("../shared-release.aar"))
implementation(files("../analytics-core-release.aar"))
implementation(files("../analytics-annotations-release.aar"))
```

### 7.4 Disable Library Dependencies

1. In the root `build.gradle.kts`, comment out the following code:

```kotlin
id("mega.android.release")
```

2. In `settings.gradle.kts`, comment out the following code:

```kotlin
maven {
    url =
        uri("${System.getenv("ARTIFACTORY_BASE_URL")}/artifactory/mega-gradle/megagradle")
}
```

and

```kotlin
resolutionStrategy {
    eachPlugin {
        if (requested.id.id == "mega.android.release") {
            useModule("mega.privacy:megagradle:${requested.version}")
        }
    }
}
```

### 7.5 Use Public Dependencies

In `lib.versions.toml`, replace dependencies of **Telephoto** and **uCrop** with their publicly available versions from their official GitHub repositories.

### 7.6 Run the Project

Open the project with Android Studio, let it build the project, and click **Run**.

## Notice

To use the **geolocation feature**, you need a **Google Maps API key**:

1. To get one, follow the directions here: https://developers.google.com/maps/documentation/android/signup

2. Once you have your key, replace the `"google_maps_key"` string in these files:
   - `app/src/debug/res/values/google_maps_api.xml`
   - `app/src/release/res/values/google_maps_api.xml`
