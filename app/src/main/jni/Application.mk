DISABLE_WEBRTC = false
USE_LIBWEBSOCKETS = true

USE_LIBWEBSOCKETS = true
APP_PLATFORM = android-16

NDK_TOOLCHAIN_VERSION=clang
APP_STL := c++_static
APP_OPTIM := release
APP_PIE := false

APP_CPPFLAGS += -std=c++11 -Wno-extern-c-compat -mno-unaligned-access -fexceptions -frtti
