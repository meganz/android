LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := libuv
LOCAL_CFLAGS := -fexceptions -frtti -fvisibility=hidden -fdata-sections -ffunction-sections
LOCAL_SRC_FILES := $(addprefix libuv/src/, fs-poll.c inet.c threadpool.c uv-common.c version.c) $(addprefix libuv/src/unix/, async.c core.c dl.c fs.c getaddrinfo.c getnameinfo.c loop-watcher.c loop.c pipe.c poll.c process.c signal.c stream.c tcp.c thread.c timer.c tty.c udp.c android-ifaddrs.c pthread-fixes.c linux-core.c linux-inotify.c linux-syscalls.c proctitle.c)

LOCAL_C_INCLUDES += $(LOCAL_PATH)/libuv/include $(LOCAL_PATH)/libuv/src
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/libuv/include $(LOCAL_PATH)/libuv/src
LOCAL_EXPORT_CFLAGS := -DHAVE_LIBUV
include $(BUILD_STATIC_LIBRARY)

