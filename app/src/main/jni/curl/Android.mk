LOCAL_PATH:= $(call my-dir)

CFLAGS := -Wpointer-arith -Wwrite-strings -Wunused -Winline -Wnested-externs -Wmissing-declarations -Wmissing-prototypes -Wno-long-long -Wfloat-equal -Wno-multichar -Wsign-compare -Wno-format-nonliteral -Wendif-labels -Wstrict-prototypes -Wdeclaration-after-statement -Wno-system-headers -Wno-nested-externs -DHAVE_CONFIG_H

include $(CLEAR_VARS)
include $(LOCAL_PATH)/curl/lib/Makefile.inc
LOCAL_MODULE := curl
LOCAL_SRC_FILES := $(addprefix curl/lib/,$(CSOURCES))
LOCAL_C_INCLUDES += $(LOCAL_PATH)/curl/include $(LOCAL_PATH)/curl/lib $(LOCAL_PATH)/include
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/curl/include $(LOCAL_PATH)/include 
LOCAL_CFLAGS += $(CFLAGS) -DHAVE_CONFIG_H -fexceptions -frtti -fvisibility=hidden -fdata-sections -ffunction-sections
LOCAL_STATIC_LIBRARIES := ares ssl crypto
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
include $(LOCAL_PATH)/ares/Makefile.inc
LOCAL_MODULE := ares
LOCAL_SRC_FILES := $(addprefix ares/,$(CSOURCES))
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/ares $(LOCAL_PATH)/include
LOCAL_C_INCLUDES += $(LOCAL_PATH)/ares $(LOCAL_PATH)/include 
LOCAL_CFLAGS += $(CFLAGS) -DHAVE_CONFIG_H -fexceptions -frtti -fvisibility=hidden -fdata-sections -ffunction-sections
LOCAL_STATIC_LIBRARIES := crypto ssl
include $(BUILD_STATIC_LIBRARY)


