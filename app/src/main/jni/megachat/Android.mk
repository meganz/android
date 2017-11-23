LOCAL_PATH := $(call my-dir)
local_c_includes := \
        $(LOCAL_PATH)/sdk/src \
        $(LOCAL_PATH)/sdk/third-party/strophe-native \
        $(LOCAL_PATH)/sdk/src/rtctestapp/buildRelease/chatclient/rtcModule/base/strophe/include/ \
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
  LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/webrtc/include
  LOCAL_EXPORT_CFLAGS :=
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
LOCAL_C_INCLUDES += $(LOCAL_PATH)/sdk/third-party/libws/src/
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/sdk/third-party/libws/src/
LOCAL_CFLAGS += -fvisibility=hidden -fdata-sections -ffunction-sections
LOCAL_STATIC_LIBRARIES := ssl crypto libevent2
LOCAL_EXPORT_CFLAGS :=
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := megachat
LOCAL_CFLAGS := -DWEBRTC_ANDROID -DWEBRTC_POSIX -D__ANDROID__ -fvisibility=hidden -fvisibility-inlines-hidden -fdata-sections -ffunction-sections -D_FILE_OFFSET_BITS=64 -DNO_TCMALLOC -DDISABLE_NACL -DCHROMIUM_BUILD -DUSE_LIBJPEG_TURBO=1 -DUSE_PROPRIETARY_CODECS -DENABLE_BROWSER_CDMS -DENABLE_CONFIGURATION_POLICY -DDISCARDABLE_MEMORY_ALWAYS_SUPPORTED_NATIVELY -DSYSTEM_NATIVELY_SIGNALS_MEMORY_PRESSURE -DENABLE_EGLIMAGE=1 -DENABLE_AUTOFILL_DIALOG=1 -DCLD_VERSION=1 -DENABLE_PRINTING=1 -DENABLE_MANAGED_USERS=1 -DVIDEO_HOLE=1 -DWEBRTC_RESTRICT_LOGGING -DWEBRTC_MODULE_UTILITY_VIDEO -DWEBRTC_ARCH_ARM -DWEBRTC_ARCH_ARM_V7 -DWEBRTC_DETECT_ARM_NEON -DWEBRTC_POSIX -DWEBRTC_LINUX -DWEBRTC_ANDROID -DGTEST_HAS_POSIX_RE=0 -DGTEST_LANG_CXX11=0 -DU_USING_ICU_NAMESPACE=0 -DU_ENABLE_DYLOAD=0 -DU_STATIC_IMPLEMENTATION -DUSE_OPENSSL=1 -DUSE_OPENSSL_CERTS=1 -DANDROID -D__GNU_SOURCE=1 -DUSE_STLPORT=1 -D_STLP_USE_PTR_SPECIALIZATIONS=1 '-DCHROME_BUILD_ID=""' -DHAVE_SYS_UIO_H -DDYNAMIC_ANNOTATIONS_ENABLED=1 -DWTF_USE_DYNAMIC_ANNOTATIONS=1 -D__STDC_LIMIT_MACROS -D__STDC_CONSTANT_MACROS
LOCAL_SRC_FILES := $(addprefix sdk/src/, megachatapi.cpp megachatapi_impl.cpp strongvelope/strongvelope.cpp presenced.cpp base64.cpp chatClient.cpp chatd.cpp url.cpp karereCommon.cpp userAttrCache.cpp base/cservices.cpp base/logger.cpp net/websocketsIO.cpp net/libwsIO.cpp waiter/libeventWaiter.cpp karereDbSchema.cpp)
    
LOCAL_C_INCLUDES += $(local_c_includes)
LOCAL_EXPORT_C_INCLUDES += $(local_c_includes)
LOCAL_STATIC_LIBRARIES := curl cryptopp libws megasdk

ifeq ($(DISABLE_WEBRTC),true)
  LOCAL_CFLAGS += -DKARERE_DISABLE_WEBRTC=1 -DSVC_DISABLE_STROPHE
  LOCAL_EXPORT_CFLAGS += -DKARERE_DISABLE_WEBRTC=1 -DSVC_DISABLE_STROPHE
else
  LOCAL_SRC_FILES += dummyCrypto.cpp megaCryptoFunctions.cpp strophe.disco.cpp rtcModule/lib.cpp rtcModule/rtcModule.cpp rtcModule/rtcStats.cpp rtcModule/strophe.jingle.cpp rtcModule/strophe.jingle.sdp.cpp rtcModule/strophe.jingle.session.cpp rtcModule/webrtcAdapter.cpp
  LOCAL_STATIC_LIBRARIES += strophe
endif

include $(BUILD_STATIC_LIBRARY)

