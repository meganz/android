## About this module
This module contains the pre-built library for [android-database-sqlcipher](https://github.com/sqlcipher/android-database-sqlcipher)
The library has been rebuilt to support 16KB page size to support Android 15.

## How to build the library:
```shell
# In Android Studio, download NDK 23.0.7599858
 
# create a directory for the build
mkdir -p ~/Desktop/rebuild-sqlcipher
cd ~/Desktop/rebuild-sqlcipher
 
# download all source code
git clone https://github.com/sqlcipher/android-database-sqlcipher.git
git clone https://github.com/openssl/openssl.git
git clone https://github.com/sqlcipher/sqlcipher.git
 
# set needed environment variables(update to your actual path)
export ANDROID_NDK_HOME=/PATH/TO/ndk/23.0.7599858
export ANDROID_NDK_ROOT=/PATH/TO/ndk/23.0.7599858
export ANDROID_HOME=/PATH/TO/Android/sdk
export JAVA_HOME=PATH_TO_YOUR_JAVA_HOME
export PATH=$PATH:/PATH/TO/ndk/23.0.7599858
 
# Replace android-database-sqlcipher/src/main/cpp/Android.mk with attached "Android.mk"
 
cd android-database-sqlcipher
SQLCIPHER_ROOT=~/Desktop/rebuild-sqlcipher/sqlcipher \
OPENSSL_ROOT=~/Desktop/rebuild-sqlcipher/openssl \
SQLCIPHER_CFLAGS="-DSQLITE_HAS_CODEC -DSQLITE_TEMP_STORE=2" \
SQLCIPHER_ANDROID_VERSION="4.5.4" \
make build-release
 
# once build is successful, the aar file is created at android-database-sqlcipher/android-database-sqlcipher/build/outputs/aar/android-database-sqlcipher-4.5.4-release.aar
 
#copy this aar file to MEGA project.
```