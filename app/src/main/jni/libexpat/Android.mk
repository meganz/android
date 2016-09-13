LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := libexpat
LOCAL_CFLAGS := -fexceptions -frtti -fvisibility=hidden -fdata-sections -ffunction-sections -DHAVE_EXPAT_CONFIG_H
LOCAL_SRC_FILES := $(addprefix libexpat/lib/, xmlparse.c xmlrole.c xmltok.c xmltok_impl.c xmltok_ns.c)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include $(LOCAL_PATH)/libexpat/lib $(LOCAL_PATH)/libexpat
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/libexpat/lib
LOCAL_EXPORT_CFLAGS := 
include $(BUILD_STATIC_LIBRARY)

