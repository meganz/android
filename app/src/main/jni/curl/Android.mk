LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := curl
LOCAL_SRC_FILES := $(LOCAL_PATH)/curl/curl-android-$(TARGET_ARCH_ABI)/lib/libcurl.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/curl/curl-android-$(TARGET_ARCH_ABI)/include

ifeq ($(DISABLE_WEBRTC),true)
LOCAL_STATIC_LIBRARIES := ares ssl crypto
else
# WebRTC contains BoringSSL
LOCAL_STATIC_LIBRARIES := ares webrtc
endif

include $(PREBUILT_STATIC_LIBRARY)


include $(CLEAR_VARS)

LOCAL_MODULE := ares
LOCAL_SRC_FILES := $(LOCAL_PATH)/ares/ares-android-$(TARGET_ARCH_ABI)/lib/libcares.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/ares/ares-android-$(TARGET_ARCH_ABI)/include

ifeq ($(DISABLE_WEBRTC),true)
LOCAL_STATIC_LIBRARIES := crypto ssl
else
# WebRTC contains BoringSSL
LOCAL_STATIC_LIBRARIES := webrtc 
endif

include $(PREBUILT_STATIC_LIBRARY)
