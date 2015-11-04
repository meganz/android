APP_PLATFORM=android-9
NDK_TOOLCHAIN_VERSION=4.8
APP_STL := gnustl_static
APP_ABI := armeabi x86
APP_OPTIM := release
APP_PIE := false

# then enable c++11 extentions in source code
APP_CPPFLAGS += -std=c++11
# or use APP_CPPFLAGS := -std=gnu++11