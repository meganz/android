DISABLE_WEBRTC = false
USE_LIBWEBSOCKETS = true

ifeq ($(DISABLE_WEBRTC),true)
APP_PLATFORM = android-9
APP_ABI := armeabi x86
else
USE_LIBWEBSOCKETS = true
APP_PLATFORM = android-16
APP_ABI := armeabi-v7a arm64-v8a x86 x86_64
endif

NDK_TOOLCHAIN_VERSION=clang
APP_STL := c++_static
APP_OPTIM := release
APP_PIE := false

APP_CPPFLAGS += -std=c++11 -Wno-extern-c-compat -mno-unaligned-access -fexceptions -frtti
