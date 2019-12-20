DISABLE_WEBRTC = false
USE_LIBWEBSOCKETS = true

USE_LIBWEBSOCKETS = true
APP_PLATFORM = android-16

NDK_TOOLCHAIN_VERSION=clang
APP_STL := c++_shared
APP_OPTIM := release
APP_PIE := false

APP_CPPFLAGS += -Wno-extern-c-compat -mno-unaligned-access -fexceptions -frtti
APP_LDFLAGS += -v -Wl,-allow-multiple-definition
