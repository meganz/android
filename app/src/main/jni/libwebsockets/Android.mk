LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := libwebsockets
LOCAL_CFLAGS    := 
LWS_LIB_PATH	:= libwebsockets/lib
LOCAL_C_INCLUDES:= $(LOCAL_PATH)/$(LWS_LIB_PATH) $(LOCAL_PATH)/include
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(LWS_LIB_PATH) $(LOCAL_PATH)/include

LOCAL_SRC_FILES := $(addprefix libwebsockets/lib/, misc/base64-decode.c handshake.c libwebsockets.c service.c pollfd.c output.c server/parsers.c context.c alloc.c header.c misc/lws-ring.c client/client.c client/client-handshake.c client/client-parser.c ssl.c misc/lws-genhash.c client/ssl-client.c misc/sha-1.c plat/lws-plat-unix.c event-libs/libuv.c misc/getifaddrs.c ext/extension.c ext/extension-permessage-deflate.c server/ranges.c server/fops-zip.c server/server.c server/server-handshake.c server/ssl-server.c)

ifeq ($(DISABLE_WEBRTC),true)
LOCAL_STATIC_LIBRARIES := libuv ssl crypto
else
LOCAL_STATIC_LIBRARIES := libuv webrtc
endif

include $(BUILD_STATIC_LIBRARY)

