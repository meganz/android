DISABLE_WEBRTC = false
USE_LIBWEBSOCKETS = true

USE_LIBWEBSOCKETS = true
APP_PLATFORM=android-26

NDK_TOOLCHAIN_VERSION=clang
APP_STL := c++_shared
APP_OPTIM := release
APP_PIE := false

APP_CPPFLAGS += -Wno-extern-c-compat -fexceptions -frtti -std=c++17
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    APP_CPPFLAGS += -mno-unaligned-access
else ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
    APP_CPPFLAGS += -mno-unaligned-access
endif

APP_LDFLAGS += -v -Wl,-allow-multiple-definition
