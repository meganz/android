APP_PLATFORM=android-9
NDK_TOOLCHAIN_VERSION=clang
APP_STL := c++_static
APP_ABI := armeabi x86
APP_OPTIM := release
APP_PIE := false

# then enable c++11 extentions in source code
APP_CPPFLAGS += -std=c++11 -Wno-extern-c-compat
# or use APP_CPPFLAGS := -std=gnu++11

DISABLE_WEBRTC = true

