LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := libwebsockets
LOCAL_CFLAGS    := 
LWS_LIB_PATH	:= libwebsockets/lib
LOCAL_C_INCLUDES:= $(LOCAL_PATH)/$(LWS_LIB_PATH) $(LOCAL_PATH)/include
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/$(LWS_LIB_PATH) $(LOCAL_PATH)/include

LOCAL_SRC_FILES := $(addprefix libwebsockets/lib/, base64-decode.c handshake.c libwebsockets.c service.c pollfd.c output.c parsers.c context.c alloc.c header.c client.c client-handshake.c client-parser.c ssl.c ssl-client.c sha-1.c lws-plat-unix.c libuv.c getifaddrs.c extension.c extension-permessage-deflate.c ranges.c fops-zip.c server.c server-handshake.c ssl-server.c)

ifeq ($(DISABLE_WEBRTC),true)
LOCAL_STATIC_LIBRARIES := libuv ssl crypto
else
LOCAL_STATIC_LIBRARIES := libuv webrtc
endif

include $(BUILD_STATIC_LIBRARY)

