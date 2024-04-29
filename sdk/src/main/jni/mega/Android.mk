LOCAL_PATH := $(call my-dir)

local_c_includes := \
        $(LOCAL_PATH) \
        $(LOCAL_PATH)/.. \
        $(LOCAL_PATH)/sdk \
        $(LOCAL_PATH)/sdk/include \
        $(LOCAL_PATH)/sdk/include/mega/posix \
        $(LOCAL_PATH)/sdk/src/fuse/common \
        $(LOCAL_PATH)/sdk/src/fuse/unsupported \
        $(LOCAL_PATH)/android

include $(CLEAR_VARS)
include $(LOCAL_PATH)/Makefile.inc
LOCAL_MODULE    := megasdk
LOCAL_CFLAGS := -fvisibility=hidden -fvisibility-inlines-hidden -fdata-sections -ffunction-sections -DDEBUG -DENABLE_CHAT -DENABLE_SYNC -DENABLE_CRASHLYTICS -DMEGA_USE_C_ARES -DUSE_POLL -DUSE_INOTIFY
LOCAL_SRC_FILES := $(CPP_SOURCES) $(C_SOURCES) $(C_WRAPPER_SOURCES)
LOCAL_C_INCLUDES += $(local_c_includes)
LOCAL_EXPORT_C_INCLUDES += $(local_c_includes)
LOCAL_EXPORT_CFLAGS += -DENABLE_CHAT -DENABLE_SYNC
LOCAL_STATIC_LIBRARIES := curl cryptopp sqlite libuv sodium mediainfo icu
include $(BUILD_STATIC_LIBRARY)

