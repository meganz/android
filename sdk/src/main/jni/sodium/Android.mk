LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := sodium
LOCAL_SRC_FILES := $(LOCAL_PATH)/sodium/libsodium-android-$(TARGET_ARCH_ABI)/lib/libsodium.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/sodium/src/libsodium/include
LOCAL_EXPORT_CFLAGS := -DENABLE_CHAT
LOCAL_LDFLAGS += "-Wl,-z,max-page-size=16384"
include $(PREBUILT_STATIC_LIBRARY)
