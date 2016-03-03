MEGA Android Client
================

A fully-featured client to access your Cloud Storage provided by MEGA.

This document will guide you to build the application on a Linux machine with Eclipse ADT.

### Setup development environment

* Android Developer Tools or [Eclipse](https://www.eclipse.org/downloads/) with [ADT plugin](https://marketplace.eclipse.org/content/android-development-tools-eclipse)

* [Android SDK Tools](http://developer.android.com/intl/es/sdk/index.html#Other)

* [Android NDK](http://developer.android.com/intl/es/ndk/downloads/index.html)

### Import 3rd-party dependencies

* Import into your workspace the following dependencies from your Android SDK path:

1. `android-support-design`: _\<android_sdk_path\>/extras/android/support/design_
2. `android-support-v7-appcompat`: _\<android_sdk_path\>/extras/android/support/v7/appcompat_
3. `google-play-services_lib`: _\<android_sdk_path\>/extras/google/google-play-services_
4. `RecyclerView`: _\<android_sdk_path\>/extras/android/support/v7/RecyclerView_

* Download and import `AndroidSlidingUpPanel` from its repository: https://github.com/umano/AndroidSlidingUpPanel

* Download and import `ScrollParallaxLib` from its repository: https://github.com/nirhart/ParallaxScroll

All the dependencies above must be checked as Android libraries: _Properties > Android > Is library._

Additionally, you need to reference `android-support-v7-appcompat` as a dependency for both `android-support-design` and `AndroidSlidingUpPanel`: _Properties > Android > Add..._


### Setup the project

* Get the source code

```
git clone https://github.com/meganz/android2.git
git submodule update --init --recursive
```

* Import the downloaded project into your workspace in Eclipse

* Configure the variable `NDK_ROOT` to point to your Android NDK installation path at `jni/Makefile`.

* Run the previous `Makefile` in order to build the MEGA SDK, its dependencies and the required bindings for Java. Moreover, it will automatically download and build several libraries required by the SDK: OpenSSL, cURL, ares, Crypto++, Sodium and SQLite.

* Configure the SDK bindings for the Android project: _Build Path > Configure Build Path... > Source > Add Folder..._ and select `jni/mega/sdk/bindings/java`

* Exclude the unnecessary bindings from compilation: select `MegaApiSwing.java` and _Delete_

* Finally, make sure the project actually references every aforementioned dependency: _Properties > Android > Add..._
