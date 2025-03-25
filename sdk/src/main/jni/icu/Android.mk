LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

ICU_VERSION:= 71_1
LOCAL_MODULE := icu
LOCAL_SRC_FILES := $(LOCAL_PATH)/icuSource-${ICU_VERSION}/icu/$(TARGET_ARCH_ABI)/lib/libicuuc.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/icuSource-${ICU_VERSION}/icu/source/common
LOCAL_LDFLAGS += "-Wl,-z,max-page-size=16384"
LOCAL_STATIC_LIBRARIES := icu

include $(PREBUILT_STATIC_LIBRARY)
