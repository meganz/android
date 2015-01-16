TOP_PATH := $(call my-dir)
NDK_PROJECT_PATH := $(TOP_PATH)/openssl
include $(TOP_PATH)/openssl/Android.mk
NDK_PROJECT_PATH := $(TOP_PATH)
include $(TOP_PATH)/curl/Android.mk
include $(TOP_PATH)/cryptopp/Android.mk
#include $(TOP_PATH)/sodium/Android.mk
include $(TOP_PATH)/mega/Android.mk

