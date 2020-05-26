MEGA Android Client
================

A fully-featured client to access your Cloud Storage provided by MEGA.

This document will guide you to build the application on a Linux machine with Android Studio.

### Setup development environment

* [Android Studio](http://developer.android.com/intl/es/sdk/index.html)

* [Android SDK Tools](http://developer.android.com/intl/es/sdk/index.html#Other)

* [Android NDK](http://developer.android.com/intl/es/ndk/downloads/index.html)

### Build & Run the application

1. Get the source code.

```
git clone --recursive https://github.com/meganz/android.git
```

2. Install in your system the [Android NDK 14](https://dl.google.com/android/repository/android-ndk-r14b-linux-x86_64.zip) and [Android NDK 16](https://dl.google.com/android/repository/android-ndk-r16b-linux-x86_64.zip).

3. Configure the variable `NDK_ROOT32` to point to your Android NDK 14 installation path and the variable `NDK_ROOT64` to point to your Android NDK 16 installation path at `app/src/main/jni/build.sh`.

4. Download the link https://mega.nz/#!wixgSaZZ!6zRMV_d8ogouBaEidHzGws1KvLrBwBiKEm0VIVgXEPk, uncompress it and put the folder `webrtc` in the path `app/src/main/jni/megachat/`.

5. Before running the building script, install the required packages. For example for Ubuntu or other Debian-based distro:

```
sudo apt install build-essential swig automake libtool autoconf cmake
```

6. Build SDK by running `./build.sh all` at `app/src/main/jni/`. You could also run `./build.sh clean` to clean the previous configuration. **IMPORTANT:** check that the build process finished successfully, it should finish with the **Task finished OK** message. Otherwise, modify `LOG_FILE` variable in `build.sh` from `/dev/null` to a certain text file and run `./build.sh all` again for viewing the build errors.

7. Download the link https://mega.nz/#!1tcl3CrL!i23zkmx7ibnYy34HQdsOOFAPOqQuTo1-2iZ5qFlU7-k, uncompress it and put the folders `debug` and `release` in the path `app/src/`.

8. Open the project with Android Studio, let it build the project and hit _*Run*_.

### Notice

To use the *geolocation feature* you need a *Google Maps API key*:

1. To get one, follow the directions here: https://developers.google.com/maps/documentation/android/signup.

2. Once you have your key, replace the "google_maps_key" string in these files: `app/src/debug/res/values/google_maps_api.xml` and `app/src/release/res/values/google_maps_api.xml`.
