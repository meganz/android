LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := icu
LOCAL_SRC_FILES := $(LOCAL_PATH)/icuSource-71_1/icu/$(TARGET_ARCH_ABI)/lib/libicuuc.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/icuSource-71_1/icu/source/common
LOCAL_STATIC_LIBRARIES := icu

include $(PREBUILT_STATIC_LIBRARY)
