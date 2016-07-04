LOCAL_PATH := $(call my-dir)

local_c_includes := \
        $(LOCAL_PATH) \
        $(LOCAL_PATH)/.. \
        $(LOCAL_PATH)/sdk \
        $(LOCAL_PATH)/sdk/include \
        $(LOCAL_PATH)/sdk/include/mega/posix \
        $(LOCAL_PATH)/sdk/third_party/glob \
        $(LOCAL_PATH)/android

include $(CLEAR_VARS)
include $(LOCAL_PATH)/Makefile.inc
LOCAL_MODULE    := megasdk
LOCAL_CFLAGS := -fexceptions -frtti -fvisibility=hidden -fvisibility-inlines-hidden -fdata-sections -ffunction-sections -DDEBUG
LOCAL_SRC_FILES := $(CPP_SOURCES) $(C_SOURCES) $(C_WRAPPER_SOURCES)
LOCAL_C_INCLUDES += $(local_c_includes)
LOCAL_STATIC_LIBRARIES := curl cryptopp sqlite libuv
include $(BUILD_STATIC_LIBRARY)


# This last step is intended to strip all unneeded code from the shared library
# All other compilations have the flag -fvisibility=hidden -fdata-sections -ffunction-sections 
# The link step uses -Wl,-dead_strip,-gc-sections to strip all unused code
include $(CLEAR_VARS)
include $(LOCAL_PATH)/Makefile.inc
LOCAL_MODULE    := mega
LOCAL_CFLAGS := -fexceptions -frtti -fdata-sections -ffunction-sections -DDEBUG
LOCAL_SRC_FILES := $(JAVA_WRAPS)
LOCAL_C_INCLUDES += -fexceptions -frtti $(local_c_includes)
LOCAL_LDLIBS := -lm -lz -llog
LOCAL_LDFLAGS :=  -L$(LOCAL_PATH)/../../obj/local/armeabi/ -Wl,-dead_strip,-gc-sections 
LOCAL_STATIC_LIBRARIES := megasdk
include $(BUILD_SHARED_LIBRARY)


