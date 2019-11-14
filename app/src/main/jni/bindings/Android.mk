LOCAL_PATH := $(call my-dir)

# This last step is intended to strip all unneeded code from the shared library
# All other compilations have the flag -fvisibility=hidden -fdata-sections -ffunction-sections 
# The link step uses -Wl,-dead_strip,-gc-sections to strip all unused code
include $(CLEAR_VARS)
LOCAL_MODULE := mega
LOCAL_CFLAGS := -fdata-sections -ffunction-sections -DDEBUG -I$(LOCAL_PATH)/../megachat/webrtc/include/third_party/abseil-cpp
LOCAL_SRC_FILES := $(LOCAL_PATH)/megasdk.cpp $(LOCAL_PATH)/megachat.cpp
LOCAL_LDLIBS := -lm -lz -llog -lGLESv2 -lOpenSLES -latomic
LOCAL_LDFLAGS :=  -Wl,-gc-sections
LOCAL_STATIC_LIBRARIES := megasdk megachat

ifeq ($(DISABLE_WEBRTC),false)
LOCAL_LDFLAGS += -Wl,--undefined=Java_org_webrtc_PeerConnectionFactory_nativeInitializeAndroidGlobals,--undefined=Java_org_webrtc_VideoTrack_nativeAddRenderer,--undefined=Java_org_webrtc_VideoFileRenderer_nativeCreateNativeByteBuffer,--undefined=Java_org_webrtc_WrappedNativeI420Buffer_nativeAddRef,--undefined=Java_org_webrtc_VideoRenderer_freeWrappedVideoRenderer,--undefined=Java_org_webrtc_PeerConnectionFactory_nativeCreateVideoSource,--undefined=Java_org_webrtc_VideoDecoderWrapperCallback_nativeOnDecodedFrame,--undefined=Java_org_webrtc_VideoSource_nativeAdaptOutputFormat,--undefined=Java_org_webrtc_Metrics_nativeEnable,--undefined=Java_org_webrtc_NetworkMonitor_nativeNotifyConnectionTypeChanged,--undefined=Java_org_webrtc_Histogram_nativeCreateCounts
LOCAL_STATIC_LIBRARIES += webrtc
endif

include $(BUILD_SHARED_LIBRARY)
