LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := sqlite
LOCAL_CFLAGS := -fvisibility=hidden -fvisibility-inlines-hidden -fdata-sections -ffunction-sections
LOCAL_SRC_FILES := sqlite/sqlite3.c
LOCAL_C_INCLUDES += sqlite/sqlite3.h
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/sqlite
include $(BUILD_STATIC_LIBRARY)

