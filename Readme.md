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

* Configure the variable `NDK_ROOT32` to point to your Android NDK 14 installation path and the variable `NDK_ROOT64` to point to your Android NDK 16 installation path at `app/src/main/jni/build.sh`.

* Download the link https://mega.nz/#!1wERDaYD!B66nc57HnZL6w9ArVuwOh80ZoVLprXrrSsuAE6CGfXc, uncompress it and put the folder `webrtc` in the path `app/src/main/jni/megachat/`.

* Go to `app/src/main/jni/` and execute: `./build.sh clean` and `./build.sh all`.

* Download the link https://mega.nz/#!1tcl3CrL!i23zkmx7ibnYy34HQdsOOFAPOqQuTo1-2iZ5qFlU7-k, uncompress it and put the folders `debug` and `release` in the path `app/src/`.

* Open the project with Android Studio, let it build the project and hit _*Run*_.

### Notice

To use the *geolocation feature* you need a *Google Maps API key*:

* To get one, follow the directions here: https://developers.google.com/maps/documentation/android/signup.

* Once you have your key, replace the "google_maps_key" string in these files: `app/src/debug/res/values/google_maps_api.xml` and `app/src/release/res/values/google_maps_api.xml`.
