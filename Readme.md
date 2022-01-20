MEGA Android Client
================

A fully-featured client to access your Cloud Storage provided by MEGA.

This document will guide you to build the application on a Linux machine with Android Studio.

### Setup development environment

* [Android Studio](https://developer.android.com/studio)

* [Android SDK Tools](https://developer.android.com/studio#Other)

* [Android NDK](https://developer.android.com/ndk/downloads)

### Build & Run the application

1. Get the source code.

```
git clone --recursive https://github.com/meganz/android.git
```

2. Install in your system the [Android NDK 21](https://dl.google.com/android/repository/android-ndk-r21d-linux-x86_64.zip) (latest version tested: NDK r21d).

3. Export `NDK_ROOT` variable or create a symbolic link at `${HOME}/android-ndk` to point to your Android NDK installation path.

```
export NDK_ROOT=/path/to/ndk
```
```
ln -s /path/to/ndk ${HOME}/android-ndk
```

4. Export `ANDROID_HOME` variable or create a symbolic link at `${HOME}/android-sdk` to point your Android SDK installation path.

```
export ANDROID_HOME=/path/to/sdk
```
```
ln -s /path/to/sdk ${HOME}/android-sdk
```

5. Export `JAVA_HOME` variable or create a symbolic link at `${HOME}/android-java` to point your Java installation path.

```
export JAVA_HOME=/path/to/jdk
```
```
ln -s /path/to/jdk ${HOME}/android-java
```

6. Download the link https://mega.nz/file/A4pxxQoJ#OoAuL0SKIGuWkw6iSrSPHRMF0-Ri7BSF64IDeIWq-qs, uncompress it and put the folder `webrtc` in the path `app/src/main/jni/megachat/`.

7. Before running the building script, install the required packages. For example for Ubuntu or other Debian-based distro:

```
sudo apt install build-essential swig automake libtool autoconf cmake
```

8. Build SDK by running `./build.sh all` at `app/src/main/jni/`. You could also run `./build.sh clean` to clean the previous configuration. **IMPORTANT:** check that the build process finished successfully, it should finish with the **Task finished OK** message. Otherwise, modify `LOG_FILE` variable in `build.sh` from `/dev/null` to a certain text file and run `./build.sh all` again for viewing the build errors.

9. Download the link https://mega.nz/#!1tcl3CrL!i23zkmx7ibnYy34HQdsOOFAPOqQuTo1-2iZ5qFlU7-k, uncompress it and put the folders `debug` and `release` in the path `app/src/`.

10. Open the project with Android Studio, let it build the project and hit _*Run*_.

#### macOS setup

To build jni libs on macOS, you need install these dependencies via brew:

    `brew install bash gnu-sed gnu-tar autoconf automake cmake coreutils libtool swig wget xz`

Then reboot MacOS to ensure newly installed latest bash(v5.x) overrides default v3.x in PATH

Then edit PATH env (Please make sure the gnu paths are setup in front of $PATH):

    `export PATH="/usr/local/opt/gnu-tar/libexec/gnubin:$PATH"`
    `export PATH="/usr/local/opt/gnu-sed/libexec/gnubin:$PATH"`

Then download and setup NDK follow guides above, then run this command to build:

    `bash ./build.sh all`


##### If the build script fails to detect cmake when building ffmpeg extension on a mac

1. In Android studio, open the SDK manager (Or through Settings>Appearance & Behaviour>System Settings>Android SDK)
2. Go to the SDK Tools tab
3. Check the "Show package details" box
4. Expand the CMake section in the list
5. Select 3.10.2.4988404
6. Click "OK"
7. Add the following to your PATH:
    `export PATH="/Users/{USERNAME}/Library/Android/sdk/cmake/3.10.2.4988404/bin:$PATH"`
8. Retry the build

### Notice

To use the *geolocation feature* you need a *Google Maps API key*:

1. To get one, follow the directions here: https://developers.google.com/maps/documentation/android/signup.

2. Once you have your key, replace the "google_maps_key" string in these files: `app/src/debug/res/values/google_maps_api.xml` and `app/src/release/res/values/google_maps_api.xml`.
