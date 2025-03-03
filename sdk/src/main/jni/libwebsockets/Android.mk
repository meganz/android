LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := libwebsockets
LOCAL_CFLAGS    := 
LOCAL_SRC_FILES := $(LOCAL_PATH)/libwebsockets/libwebsockets-android-$(TARGET_ARCH_ABI)/lib/libwebsockets.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/libwebsockets/libwebsockets-android-$(TARGET_ARCH_ABI)/include
LOCAL_LDFLAGS += "-Wl,-z,max-page-size=16384"

ifeq ($(DISABLE_WEBRTC),true)
LOCAL_STATIC_LIBRARIES := libuv ssl crypto
else
LOCAL_STATIC_LIBRARIES := libuv webrtc
endif

include $(PREBUILT_STATIC_LIBRARY)
