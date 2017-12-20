/*
 *  Copyright (c) 2017 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc;

/**
 * VideoDecoder callback that calls VideoDecoderWrapper.OnDecodedFrame for the decoded frames.
 */
class VideoDecoderWrapperCallback implements VideoDecoder.Callback {
  private final long nativeDecoder;

  public VideoDecoderWrapperCallback(long nativeDecoder) {
    this.nativeDecoder = nativeDecoder;
  }

  @Override
  public void onDecodedFrame(VideoFrame frame, Integer decodeTimeMs, Integer qp) {
    nativeOnDecodedFrame(nativeDecoder, frame, decodeTimeMs, qp);
  }

  private native static void nativeOnDecodedFrame(
      long nativeDecoder, VideoFrame frame, Integer decodeTimeMs, Integer qp);
}
