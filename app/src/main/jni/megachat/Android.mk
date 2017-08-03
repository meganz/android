LOCAL_PATH := $(call my-dir)
local_c_includes := \
        $(LOCAL_PATH)/sdk/src \
        $(LOCAL_PATH)/sdk/src/base/ \
        $(LOCAL_PATH)/sdk/src/rtcModule/ \
        $(LOCAL_PATH)/sdk/src/strongvelope/ \
        $(LOCAL_PATH)/sdk/third-party/ \
        $(LOCAL_PATH)/include

ifneq ($(DISABLE_WEBRTC),true)
  LOCAL_PATH := $(call my-dir)
  include $(CLEAR_VARS)
  LOCAL_MODULE := webrtc
  LOCAL_SRC_FILES := $(LOCAL_PATH)/webrtc/libwebrtc_$(TARGET_ARCH).a
  LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/webrtc/include $(LOCAL_PATH)/webrtc/include/third_party/boringssl/src/include 
  LOCAL_EXPORT_CFLAGS := -DENABLE_WEBRTC -DWEBRTC_POSIX -DWEBRTC_LINUX -DWEBRTC_ANDROID
  include $(PREBUILT_STATIC_LIBRARY)
endif

include $(CLEAR_VARS)
LOCAL_MODULE    := libevent2
LOCAL_CFLAGS := -fexceptions -frtti -fvisibility=hidden -fdata-sections -ffunction-sections -DHAVE_CONFIG_H
LOCAL_SRC_FILES := $(addprefix sdk/third-party/libevent/, buffer.c bufferevent.c bufferevent_filter.c bufferevent_pair.c bufferevent_ratelim.c bufferevent_sock.c epoll.c evdns.c event.c event_tagging.c evmap.c evrpc.c evthread.c evthread_pthread.c evutil.c evutil_rand.c evutil_time.c http.c bufferevent_openssl.c listener.c log.c poll.c select.c signal.c)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/sdk/third-party/libevent/include $(LOCAL_PATH)/sdk/third-party/libevent $(LOCAL_PATH)/sdk/third-party/libevent/compat $(LOCAL_PATH)/include
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/sdk/third-party/libevent/include $(LOCAL_PATH)/sdk/third-party/libevent $(LOCAL_PATH)/sdk/third-party/libevent/compat $(LOCAL_PATH)/include
LOCAL_CFLAGS += -fexceptions -frtti -fvisibility=hidden -fdata-sections -ffunction-sections
LOCAL_STATIC_LIBRARIES := ssl crypto
LOCAL_EXPORT_CFLAGS :=
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := libws
LOCAL_CFLAGS := -fexceptions -frtti -fvisibility=hidden -fdata-sections -ffunction-sections -DHAVE_CONFIG_H
LOCAL_SRC_FILES := $(addprefix sdk/third-party/libws/src/, libws.c libws_compat.c libws_handshake.c libws_header.c libws_log.c libws_openssl.c libws_private.c libws_sha1.c libws_utf8.c)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/sdk/third-party/libws/src/ $(LOCAL_PATH)/include
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/sdk/third-party/libws/src/
LOCAL_CFLAGS += -fexceptions -frtti -fvisibility=hidden -fdata-sections -ffunction-sections
LOCAL_STATIC_LIBRARIES := ssl crypto libevent2
LOCAL_EXPORT_CFLAGS :=
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := megachat
LOCAL_CFLAGS := -fexceptions -frtti -fvisibility=hidden -fvisibility-inlines-hidden -fdata-sections -ffunction-sections
LOCAL_SRC_FILES := $(addprefix sdk/src/, megachatapi.cpp megachatapi_impl.cpp strongvelope/strongvelope.cpp presenced.cpp base64.cpp chatClient.cpp chatd.cpp url.cpp karereCommon.cpp userAttrCache.cpp base/logger.cpp base/cservices.cpp net/websocketsIO.cpp karereDbSchema.cpp)
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

LOCAL_CFLAGS += -DKARERE_DISABLE_WEBRTC=1 -DSVC_DISABLE_STROPHE
LOCAL_EXPORT_CFLAGS += -DKARERE_DISABLE_WEBRTC=1 -DSVC_DISABLE_STROPHE

include $(BUILD_STATIC_LIBRARY)

