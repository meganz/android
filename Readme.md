MEGA Android Client
================

A fully-featured client to access your Cloud Storage provided by MEGA.

This document will guide you through building the application on a Linux machine with Android Studio.

### Set up development environment

* [Android Studio](https://developer.android.com/studio/)

* [Android SDK Tools](https://developer.android.com/studio/#downloads)

* [Android NDK](https://developer.android.com/ndk/downloads/)

### Build & run the application

* Get the source code:

```
git clone --recursive https://github.com/meganz/android.git
```

* Configure the variable `NDK_ROOT` to point to your Android NDK installation path at `jni/build.sh`.

* Open the project with Android Studio, let it build the project, and hit _*Run*_.
