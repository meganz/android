/*
 *  Copyright 2017 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc;

import static org.webrtc.MediaCodecUtils.EXYNOS_PREFIX;
import static org.webrtc.MediaCodecUtils.INTEL_PREFIX;
import static org.webrtc.MediaCodecUtils.NVIDIA_PREFIX;
import static org.webrtc.MediaCodecUtils.QCOM_PREFIX;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecList;
import android.os.Build;

/** Factory for Android hardware VideoDecoders. */
@SuppressWarnings("deprecation") // API level 16 requires use of deprecated methods.
public class HardwareVideoDecoderFactory implements VideoDecoderFactory {
  private static final String TAG = "HardwareVideoDecoderFactory";

  @Override
  public VideoDecoder createDecoder(String codecType) {
    VideoCodecType type = VideoCodecType.valueOf(codecType);
    MediaCodecInfo info = findCodecForType(type);

    if (info == null) {
      return null; // No support for this codec type.
    }

    CodecCapabilities capabilities = info.getCapabilitiesForType(type.mimeType());
    return new HardwareVideoDecoder(info.getName(), type,
        MediaCodecUtils.selectColorFormat(MediaCodecUtils.DECODER_COLOR_FORMATS, capabilities));
  }

  private MediaCodecInfo findCodecForType(VideoCodecType type) {
    // HW decoding is not supported on builds before KITKAT.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
      return null;
    }

    for (int i = 0; i < MediaCodecList.getCodecCount(); ++i) {
      MediaCodecInfo info = null;
      try {
        info = MediaCodecList.getCodecInfoAt(i);
      } catch (IllegalArgumentException e) {
        Logging.e(TAG, "Cannot retrieve encoder codec info", e);
      }

      if (info == null || info.isEncoder()) {
        continue;
      }

      if (isSupportedCodec(info, type)) {
        return info;
      }
    }
    return null; // No support for this type.
  }

  // Returns true if the given MediaCodecInfo indicates a supported encoder for the given type.
  private boolean isSupportedCodec(MediaCodecInfo info, VideoCodecType type) {
    if (!MediaCodecUtils.codecSupportsType(info, type)) {
      return false;
    }
    // Check for a supported color format.
    if (MediaCodecUtils.selectColorFormat(
            MediaCodecUtils.DECODER_COLOR_FORMATS, info.getCapabilitiesForType(type.mimeType()))
        == null) {
      return false;
    }
    return isHardwareSupported(info, type);
  }

  private boolean isHardwareSupported(MediaCodecInfo info, VideoCodecType type) {
    String name = info.getName();
    switch (type) {
      case VP8:
        // QCOM, Intel, Exynos, and Nvidia all supported for VP8.
        return name.startsWith(QCOM_PREFIX) || name.startsWith(INTEL_PREFIX)
            || name.startsWith(EXYNOS_PREFIX) || name.startsWith(NVIDIA_PREFIX);
      case VP9:
        // QCOM and Exynos supported for VP9.
        return name.startsWith(QCOM_PREFIX) || name.startsWith(EXYNOS_PREFIX);
      case H264:
        // QCOM, Intel, and Exynos supported for H264.
        return name.startsWith(QCOM_PREFIX) || name.startsWith(INTEL_PREFIX)
            || name.startsWith(EXYNOS_PREFIX);
      default:
        return false;
    }
  }
}
