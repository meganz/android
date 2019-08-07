MEGA Android Client
================

A fully-featured client to access your Cloud Storage provided by MEGA.

This document will guide you to build the application on a Linux machine with Android Studio.

### Setup development environment

* [Android Studio](http://developer.android.com/intl/es/sdk/index.html)

* [Android SDK Tools](http://developer.android.com/intl/es/sdk/index.html#Other)

* [Android NDK](http://developer.android.com/intl/es/ndk/downloads/index.html)

### Build & Run the application

* Get the source code

```
git clone --recursive https://github.com/meganz/android.git
```

* Configure the variable `NDK_ROOT` to point to your Android NDK installation path at `app/src/main/jni/build.sh`.

* Go to `app/src/main/jni/` and execute: `./build.sh clean` and `./build.sh all`.

* Download the link https://mega.nz/#!1tcl3CrL!i23zkmx7ibnYy34HQdsOOFAPOqQuTo1-2iZ5qFlU7-k, uncompress it and put the folders `debug` and `release` in the path `app/src/`.

* Open the project with Android Studio, let it build the project and hit _*Run*_.

### Notice

To use the *geolocation feature* you need a *Google Maps API key*:

* To get one, follow the directions here: https://developers.google.com/maps/documentation/android/signup.

* Once you have your key, replace the "google_maps_key" string in these files: `app/src/debug/res/values/google_maps_api.xml` and `app/src/release/res/values/google_maps_api.xml`
