LOCAL_PATH := $(call my-dir)
local_c_includes := \
        $(LOCAL_PATH)/sdk/src \
        $(LOCAL_PATH)/sdk/src/base/ \
        $(LOCAL_PATH)/sdk/src/rtcModule/ \
        $(LOCAL_PATH)/sdk/src/strongvelope/ \
        $(LOCAL_PATH)/sdk/third-party/ \
        $(LOCAL_PATH)/include \
        $(LOCAL_PATH)/webrtc/include/third_party/abseil-cpp

ifneq ($(DISABLE_WEBRTC),true)
  LOCAL_PATH := $(call my-dir)
  include $(CLEAR_VARS)
  LOCAL_MODULE := webrtc
  LOCAL_SRC_FILES := $(LOCAL_PATH)/webrtc/libwebrtc_$(TARGET_ARCH).a
  LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/webrtc/include $(LOCAL_PATH)/webrtc/include/webrtc $(LOCAL_PATH)/webrtc/include/third_party/boringssl/src/include $(LOCAL_PATH)/webrtc/include/third_party/libyuv/include
  LOCAL_EXPORT_CFLAGS := -DENABLE_WEBRTC -DNDEBUG -DANDROID -DWEBRTC_POSIX -DWEBRTC_LINUX -DWEBRTC_ANDROID -DWEBRTC_INCLUDE_INTERNAL_AUDIO_DEVICE -DRTC_ENABLE_VP9 -DWEBRTC_HAVE_SCTP -DWEBRTC_LIBRARY_IMPL -DABSL_ALLOCATOR_NOTHROW=1 -DLIBYUV_DISABLE_SME -DLIBYUV_DISABLE_LSX -DLIBYUV_DISABLE_LASX

ifeq ($(TARGET_ARCH_ABI),x86)
  LOCAL_EXPORT_CFLAGS += -DLIBYUV_DISABLE_NEON -DLIBYUV_DISABLE_SVE
else ifeq ($(TARGET_ARCH_ABI),x86_64)
  LOCAL_EXPORT_CFLAGS += -D_FILE_OFFSET_BITS=64 -DLIBYUV_DISABLE_NEON -DLIBYUV_DISABLE_SVE
else ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
  LOCAL_EXPORT_CFLAGS += -D_FILE_OFFSET_BITS=64 -DWEBRTC_HAS_NEON -DWEBRTC_ARCH_ARM64
else ifeq ($(TARGET_ARCH_ABI),arm64-v7a)
  LOCAL_EXPORT_CFLAGS += -DWEBRTC_HAS_NEON -DLIBYUV_DISABLE_SVE -DWEBRTC_ARCH_ARM -DWEBRTC_ARCH_ARM_V7
endif
  include $(PREBUILT_STATIC_LIBRARY)
endif

include $(CLEAR_VARS)
LOCAL_MODULE    := libevent2
LOCAL_CFLAGS := -fvisibility=hidden -fdata-sections -ffunction-sections -DHAVE_CONFIG_H
LOCAL_SRC_FILES := $(addprefix sdk/third-party/libevent/, buffer.c bufferevent.c bufferevent_filter.c bufferevent_pair.c bufferevent_ratelim.c bufferevent_sock.c epoll.c evdns.c event.c event_tagging.c evmap.c evrpc.c evthread.c evthread_pthread.c evutil.c evutil_rand.c evutil_time.c http.c bufferevent_openssl.c listener.c log.c poll.c select.c signal.c)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/sdk/third-party/libevent/include $(LOCAL_PATH)/sdk/third-party/libevent $(LOCAL_PATH)/sdk/third-party/libevent/compat $(LOCAL_PATH)/include
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/sdk/third-party/libevent/include $(LOCAL_PATH)/sdk/third-party/libevent $(LOCAL_PATH)/sdk/third-party/libevent/compat $(LOCAL_PATH)/include
LOCAL_CFLAGS += -fvisibility=hidden -fdata-sections -ffunction-sections
LOCAL_STATIC_LIBRARIES := ssl crypto
LOCAL_EXPORT_CFLAGS :=
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := libws
LOCAL_CFLAGS := -fvisibility=hidden -fdata-sections -ffunction-sections -DHAVE_CONFIG_H
LOCAL_SRC_FILES := $(addprefix sdk/third-party/libws/src/, libws.c libws_compat.c libws_handshake.c libws_header.c libws_log.c libws_openssl.c libws_private.c libws_sha1.c libws_utf8.c)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/sdk/third-party/libws/src/ $(LOCAL_PATH)/include
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/sdk/third-party/libws/src/
LOCAL_CFLAGS += -fvisibility=hidden -fdata-sections -ffunction-sections
LOCAL_STATIC_LIBRARIES := ssl crypto libevent2
LOCAL_EXPORT_CFLAGS :=
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := megachat
LOCAL_CFLAGS := -fvisibility=hidden -fvisibility-inlines-hidden -fdata-sections -ffunction-sections
LOCAL_SRC_FILES := $(addprefix sdk/src/, megachatapi.cpp megachatapi_impl.cpp strongvelope/strongvelope.cpp presenced.cpp base64url.cpp chatClient.cpp chatd.cpp kareredb.cpp chatclientDb.cpp url.cpp karereCommon.cpp userAttrCache.cpp base/logger.cpp base/cservices.cpp net/websocketsIO.cpp karereDbSchema.cpp)
LOCAL_C_INCLUDES += $(local_c_includes)
LOCAL_EXPORT_C_INCLUDES += $(local_c_includes)
LOCAL_STATIC_LIBRARIES := curl cryptopp megasdk

ifeq ($(USE_LIBWEBSOCKETS),true)
LOCAL_CFLAGS += -DUSE_LIBWEBSOCKETS=1
LOCAL_SRC_FILES += $(addprefix sdk/src/, net/libwebsocketsIO.cpp waiter/libuvWaiter.cpp)
LOCAL_STATIC_LIBRARIES += libwebsockets
else
LOCAL_SRC_FILES += $(addprefix sdk/src/, net/libwsIO.cpp waiter/libeventWaiter.cpp)
LOCAL_STATIC_LIBRARIES += libws
endif

ifeq ($(DISABLE_WEBRTC),false)
LOCAL_SRC_FILES += $(addprefix sdk/src/, sfu.cpp)
LOCAL_SRC_FILES += $(addprefix sdk/src/, rtcCrypto.cpp)
LOCAL_SRC_FILES += $(addprefix sdk/src/rtcModule/, webrtc.cpp webrtcAdapter.cpp rtcStats.cpp)
LOCAL_STATIC_LIBRARIES += webrtc
else
LOCAL_CFLAGS += -DKARERE_DISABLE_WEBRTC=1 -DSVC_DISABLE_STROPHE
LOCAL_EXPORT_CFLAGS += -DKARERE_DISABLE_WEBRTC=1 -DSVC_DISABLE_STROPHE
endif

include $(BUILD_STATIC_LIBRARY)
