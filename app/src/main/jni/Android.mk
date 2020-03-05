TOP_PATH := $(call my-dir)
NDK_PROJECT_PATH := $(TOP_PATH)
include $(TOP_PATH)/curl/Android.mk
include $(TOP_PATH)/cryptopp/Android.mk
include $(TOP_PATH)/sqlite/Android.mk
include $(TOP_PATH)/libuv/Android.mk
include $(TOP_PATH)/libwebsockets/Android.mk
include $(TOP_PATH)/sodium/Android.mk
include $(TOP_PATH)/mediainfo/Android.mk
include $(TOP_PATH)/megachat/Android.mk
include $(TOP_PATH)/mega/Android.mk
include $(TOP_PATH)/bindings/Android.mk
include $(TOP_PATH)/pdfviewer/Android.mk
