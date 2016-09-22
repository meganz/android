LOCAL_PATH := $(call my-dir)

# This last step is intended to strip all unneeded code from the shared library
# All other compilations have the flag -fvisibility=hidden -fdata-sections -ffunction-sections 
# The link step uses -Wl,-dead_strip,-gc-sections to strip all unused code
include $(CLEAR_VARS)
LOCAL_MODULE := mega
LOCAL_CFLAGS := -fexceptions -frtti -fdata-sections -ffunction-sections -DDEBUG
LOCAL_SRC_FILES := $(LOCAL_PATH)/megasdk.cpp $(LOCAL_PATH)/megachat.cpp
LOCAL_LDLIBS := -lm -lz -llog -lGLESv2 -lOpenSLES -latomic
LOCAL_LDFLAGS :=  -Wl,-dead_strip,-gc-sections 
LOCAL_STATIC_LIBRARIES := megasdk megachat
include $(BUILD_SHARED_LIBRARY)
