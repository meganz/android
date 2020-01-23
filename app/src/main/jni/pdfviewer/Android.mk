LOCAL_PATH := $(call my-dir)
PDFVIEWER := pdfviewer

#Prebuilt libraries
include $(CLEAR_VARS)
LOCAL_MODULE := aospPdfium

ARCH_PATH = $(TARGET_ARCH_ABI)
ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    ARCH_PATH = armeabi-v7a
endif

ifeq ($(TARGET_ARCH_ABI), arm64-v8a)
    ARCH_PATH = arm64-v8a
endif

ifeq ($(TARGET_ARCH_ABI), x86)
    ARCH_PATH = x86
endif

ifeq ($(TARGET_ARCH_ABI), x86_64)
    ARCH_PATH = x86_64
endif

LOCAL_SRC_FILES := $(LOCAL_PATH)/$(PDFVIEWER)/lib/$(ARCH_PATH)/libmodpdfium.so

include $(PREBUILT_SHARED_LIBRARY)

#libmodft2
include $(CLEAR_VARS)
LOCAL_MODULE := libmodft2

LOCAL_SRC_FILES := $(LOCAL_PATH)/$(PDFVIEWER)/lib/$(ARCH_PATH)/libmodft2.so

include $(PREBUILT_SHARED_LIBRARY)

#libmodpng
include $(CLEAR_VARS)
LOCAL_MODULE := libmodpng

LOCAL_SRC_FILES := $(LOCAL_PATH)/$(PDFVIEWER)/lib/$(ARCH_PATH)/libmodpng.so

include $(PREBUILT_SHARED_LIBRARY)

#Main JNI library
include $(CLEAR_VARS)
LOCAL_MODULE := jniPdfium

LOCAL_CFLAGS += -DHAVE_PTHREADS
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(PDFVIEWER)/include
LOCAL_SHARED_LIBRARIES += aospPdfium
LOCAL_LDLIBS += -llog -landroid -ljnigraphics -latomic

LOCAL_SRC_FILES :=  $(LOCAL_PATH)/$(PDFVIEWER)/src/mainJNILib.cpp

include $(BUILD_SHARED_LIBRARY)
