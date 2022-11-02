MEGA Android Client
================

A fully-featured client to access your Cloud Storage provided by MEGA.

This document will guide you to build the application on a Linux machine with Android Studio.

### 1. Setup development environment

* [Android Studio](https://developer.android.com/studio)

* [Android SDK Tools](https://developer.android.com/studio#Other)

* [Android NDK](https://developer.android.com/ndk/downloads)

### 2. Build & Run the application

1. Get the source code.

```
git clone --recursive https://github.com/meganz/android.git
```

### 3. NDK Configuration

#### 3.1 Linux

Install in the system the **[Android NDK](https://dl.google.com/android/repository/android-ndk-r21d-linux-x86_64.zip) [21](https://dl.google.com/android/repository/android-ndk-r21b-linux-x86_64.zip)** (latest version tested: NDK r21d, version number: 21.3.6528147).

Export  `NDK_ROOT`  variable or create a symbolic link at  `${HOME}/android-ndk`  to point to your Android NDK installation path.

`export NDK_ROOT=/path/to/ndk`

`ln -s /path/to/ndk ${HOME}/android-ndk`

#### 3.2 MacOS

Install NDK r21d by Android Studio following [these instructions](https://developer.android.com/studio/projects/install-ndk#specific-version) (pay attention to the bottom-right `Show Package Details` checkbox to display the available versions. Latest version tested: NDK r21d, version number: 21.3.6528147)

Export  `NDK_ROOT`  variable or create a symbolic link at  `${HOME}/android-ndk`  to point to your Android NDK installation path.

Default macOS path:  `export NDK_ROOT="/Users/YOUR_USER_NAME/Library/Android/sdk/ndk/21.3.6528147"`

`ln -s /path/to/ndk ${HOME}/android-ndk`

### 4. ANDROID_HOME Configuration

#### 4.1 Linux

Export  `ANDROID_HOME`  variable or create a symbolic link at  `${HOME}/android-sdk`  to point to your Android SDK installation path.

`export ANDROID_HOME=/path/to/sdk`

`ln -s /path/to/sdk ${HOME}/android-sdk`

#### 4.2 MacOS

Export  `ANDROID_HOME`  variable or create a symbolic link at  `${HOME}/android-sdk`  to point to your Android SDK installation path.

Default macOS path: `export ANDROID_HOME="/Users/YOUR_USER_NAME/Library/Android/sdk/"`)

`ln -s /path/to/sdk ${HOME}/android-sdk`

### 5. JAVA_HOME Configuration

#### 5.1 Linux

Export  `JAVA_HOME`  variable or create a symbolic link at  `${HOME}/android-java`  to point to your Java installation path.

You can use the path in Android Studio, which you can find in  `Preferences > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK (default)`

`export JAVA_HOME=/path/to/jdk`

`ln -s /path/to/jdk ${HOME}/android-java`

#### 5.2 MacOS

Export  `JAVA_HOME`  variable or create a symbolic link at  `${HOME}/android-java`  to point to your Java installation path.

You can use the path in Android Studio, which you can find in  `Preferences > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK (default)`

Default macOS path:  `export JAVA_HOME="/Applications/Android Studio.app/Contents/jre/Contents/Home"`)

`ln -s /path/to/jdk ${HOME}/android-java`

### 6. Download WEBRTC files

1. Download the link https://mega.nz/file/A4pxxQoJ#OoAuL0SKIGuWkw6iSrSPHRMF0-Ri7BSF64IDeIWq-qs, uncompress it and put the folder `webrtc` in the path `sdk/src/main/jni/megachat/`.Be mindful that the download link of webrtc may varies over time, please keep it in line with the one written in build.sh

### 7. Prerequisites of running the build script

#### 7.1 Linux

Before running the SDK building script, install the required packages. For example for Ubuntu or other Debian-based distro:

`sudo apt install build-essential swig automake libtool autoconf cmake ninja-build`

**ATTENTION:** After upgrading ExoPlayer to 2.18.1, we need to use CMake version 3.22.1

Then install CMake version 3.22.1 in `Android Studio > Tools > SDK Manager > SDK Tools > CMake`.

After installation, add below line in `~/.bashrc`

`export PATH="/home/$USER/Android/Sdk/cmake/3.22.1/bin:$PATH"`

You will need to source your `~/.bashrc` or logout/login (or restart the terminal) for the changes to take effect. To source your `~/.bashrc`, simply type:

`source ~/.bashrc`

#### 7.2 MacOS

Before running the SDK building script, install the required dependencies via HomeBrew:

Except below tools, please do not use HomeBrew to install other tools, according to SysAdmin's advice.

`brew install bash gnu-sed gnu-tar autoconf automake cmake coreutils libtool swig wget xz python3`

Then reboot MacOS - to ensure newly installed latest bash (v5.x) overrides default v3.x in PATH

Then edit PATH env (Please make sure the gnu paths are setup in front of $PATH):

-   For Intel chip, add below lines in  `~/.zshrc`

`export PATH="/usr/local/opt/gnu-tar/libexec/gnubin:$PATH"`  
`export PATH="/usr/local/opt/gnu-sed/libexec/gnubin:$PATH"`

-   For Apple chip, add below lines in  `~/.zshrc`

`export PATH="/opt/homebrew/opt/gnu-tar/libexec/gnubin:$PATH"`  
`export PATH="/opt/homebrew/opt/gnu-sed/libexec/gnubin:$PATH"`

`ln -s /opt/homebrew/bin/python3 /opt/homebrew/bin/python`

**ATTENTION:** After upgrading ExoPlayer to 2.18.1, we need to use CMake version 3.22.1

Then install CMake version 3.22.1 in `Android Studio > Tools > SDK Manager > SDK Tools > CMake`.

You have to tick checkbox 'Show Package Details' to display this specific version. After installation, add below line in  `~/.zshrc`

`export PATH="/Users/<USER_NAME>/Library/Android/sdk/cmake/3.22.1/bin:$PATH"`

### 8. Running the Build Script

Build SDK by running `./build.sh all` at `sdk/src/main/jni/`. You could also run `./build.sh clean` to clean the previous configuration.

**IMPORTANT:** check that the build process finished successfully, it should finish with the **Task finished OK** message. Otherwise, modify `LOG_FILE` variable in `build.sh` from `/dev/null` to a certain text file and run `./build.sh all` again for viewing the build errors.

In case of an error (seen in the log file mentioned) due to licenses not accepted, you can read and accept the licenses with the sdkmanager command-line tool (if you downloaded them)

`/path-to-cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_HOME --licenses`

### 9. Download Required Files

Download the link https://mega.nz/#!1tcl3CrL!i23zkmx7ibnYy34HQdsOOFAPOqQuTo1-2iZ5qFlU7-k, uncompress it and put the folders `debug` and `release` in the path `app/src/`.

### 10. Run the project

Open the project with Android Studio, let it build the project and hit _*Run*_.



##### If the build script fails to detect cmake when building ffmpeg extension on a mac

1. In Android studio, open the SDK manager (Or through Settings>Appearance & Behaviour>System Settings>Android SDK)
2. Go to the SDK Tools tab
3. Check the "Show package details" box
4. Expand the CMake section in the list
5. Select 3.22.1
6. Click "OK"
7. Add the following to your PATH:
    `export PATH="/Users/{USERNAME}/Library/Android/sdk/cmake/3.22.1/bin:$PATH"`
8. Retry the build

### Notice

To use the *geolocation feature* you need a *Google Maps API key*:

1. To get one, follow the directions here: https://developers.google.com/maps/documentation/android/signup.

2. Once you have your key, replace the "google_maps_key" string in these files: `app/src/debug/res/values/google_maps_api.xml` and `app/src/release/res/values/google_maps_api.xml`.
