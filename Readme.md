MEGA Android Client
================

A fully-featured client to access your Cloud Storage provided by MEGA.

This document will guide you to build the application on a Linux machine with Android Studio.

### Setup development environment

* [Android Studio](http://developer.android.com/intl/es/sdk/index.html)

* [Android SDK Tools](http://developer.android.com/intl/es/sdk/index.html#Other)

* [Android NDK](http://developer.android.com/intl/es/ndk/downloads/index.html)

### Build & Run the application

* Get the source code.

```
git clone --recursive https://github.com/meganz/android.git
```

* Install in your system the Android NDK 14 and Android NDK 16.

* Configure the variable `NDK_ROOT32` to point to your Android NDK 14 installation path and the variable `NDK_ROOT64` to point to your Android NDK 16 installation path at `jni/build.sh`.

* Download the folder https://mega.nz/#F!BzwF0Qba!-KXBHgwonRUnSptmVJr4qg and put it in the path `$PROJECT/app/src/main/jni/megachat/webrtc/`.

* Go to `$PROJECT/app/src/main/jni/` and execute: `./build.sh clean` and `./build.sh all`.

* Open the project with Android Studio, let it build the project and hit _*Run*_.
