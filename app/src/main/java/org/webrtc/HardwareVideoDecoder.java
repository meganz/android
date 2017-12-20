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

import android.annotation.TargetApi;
import android.graphics.Matrix;
import android.media.MediaCodec;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaFormat;
import android.os.SystemClock;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import org.webrtc.ThreadUtils.ThreadChecker;

/** Android hardware video decoder. */
@TargetApi(16)
@SuppressWarnings("deprecation") // Cannot support API 16 without using deprecated methods.
class HardwareVideoDecoder implements VideoDecoder {
  private static final String TAG = "HardwareVideoDecoder";

  // TODO(magjed): Use MediaFormat.KEY_* constants when part of the public API.
  private static final String MEDIA_FORMAT_KEY_STRIDE = "stride";
  private static final String MEDIA_FORMAT_KEY_SLICE_HEIGHT = "slice-height";
  private static final String MEDIA_FORMAT_KEY_CROP_LEFT = "crop-left";
  private static final String MEDIA_FORMAT_KEY_CROP_RIGHT = "crop-right";
  private static final String MEDIA_FORMAT_KEY_CROP_TOP = "crop-top";
  private static final String MEDIA_FORMAT_KEY_CROP_BOTTOM = "crop-bottom";

  // MediaCodec.release() occasionally hangs.  Release stops waiting and reports failure after
  // this timeout.
  private static final int MEDIA_CODEC_RELEASE_TIMEOUT_MS = 5000;

  // WebRTC queues input frames quickly in the beginning on the call. Wait for input buffers with a
  // long timeout (500 ms) to prevent this from causing the codec to return an error.
  private static final int DEQUEUE_INPUT_TIMEOUT_US = 500000;

  // Dequeuing an output buffer will block until a buffer is available (up to 100 milliseconds).
  // If this timeout is exceeded, the output thread will unblock and check if the decoder is still
  // running.  If it is, it will block on dequeue again.  Otherwise, it will stop and release the
  // MediaCodec.
  private static final int DEQUEUE_OUTPUT_BUFFER_TIMEOUT_US = 100000;

  private final String codecName;
  private final VideoCodecType codecType;

  private static class FrameInfo {
    final long decodeStartTimeMs;
    final int rotation;

    FrameInfo(long decodeStartTimeMs, int rotation) {
      this.decodeStartTimeMs = decodeStartTimeMs;
      this.rotation = rotation;
    }
  }

  private final Deque<FrameInfo> frameInfos;
  private int colorFormat;

  // Output thread runs a loop which polls MediaCodec for decoded output buffers.  It reformats
  // those buffers into VideoFrames and delivers them to the callback.
  private Thread outputThread;

  // Checker that ensures work is run on the output thread.
  private ThreadChecker outputThreadChecker;

  // Checker that ensures work is run on the decoder thread.  The decoder thread is owned by the
  // caller and must be used to call initDecode, decode, and release.
  private ThreadChecker decoderThreadChecker;

  private volatile boolean running = false;
  private volatile Exception shutdownException = null;

  // Prevents the decoder from being released before all output buffers have been released.
  private final Object activeOutputBuffersLock = new Object();
  private int activeOutputBuffers = 0; // Guarded by activeOutputBuffersLock

  // Dimensions (width, height, stride, and sliceHeight) may be accessed by either the decode thread
  // or the output thread.  Accesses should be protected with this lock.
  private final Object dimensionLock = new Object();
  private int width;
  private int height;
  private int stride;
  private int sliceHeight;

  // Whether the decoder has finished the first frame.  The codec may not change output dimensions
  // after delivering the first frame.
  private boolean hasDecodedFirstFrame;
  // Whether the decoder has seen a key frame.  The first frame must be a key frame.
  private boolean keyFrameRequired;

  // Decoding proceeds asynchronously.  This callback returns decoded frames to the caller.
  private Callback callback;

  private MediaCodec codec = null;

  HardwareVideoDecoder(String codecName, VideoCodecType codecType, int colorFormat) {
    if (!isSupportedColorFormat(colorFormat)) {
      throw new IllegalArgumentException("Unsupported color format: " + colorFormat);
    }
    this.codecName = codecName;
    this.codecType = codecType;
    this.colorFormat = colorFormat;
    this.frameInfos = new LinkedBlockingDeque<>();
  }

  @Override
  public VideoCodecStatus initDecode(Settings settings, Callback callback) {
    this.decoderThreadChecker = new ThreadChecker();
    return initDecodeInternal(settings.width, settings.height, callback);
  }

  private VideoCodecStatus initDecodeInternal(int width, int height, Callback callback) {
    decoderThreadChecker.checkIsOnValidThread();
    if (outputThread != null) {
      Logging.e(TAG, "initDecodeInternal called while the codec is already running");
      return VideoCodecStatus.ERROR;
    }

    // Note:  it is not necessary to initialize dimensions under the lock, since the output thread
    // is not running.
    this.callback = callback;
    this.width = width;
    this.height = height;

    stride = width;
    sliceHeight = height;
    hasDecodedFirstFrame = false;
    keyFrameRequired = true;

    try {
      codec = MediaCodec.createByCodecName(codecName);
    } catch (IOException | IllegalArgumentException e) {
      Logging.e(TAG, "Cannot create media decoder " + codecName);
      return VideoCodecStatus.ERROR;
    }
    try {
      MediaFormat format = MediaFormat.createVideoFormat(codecType.mimeType(), width, height);
      format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
      codec.configure(format, null, null, 0);
      codec.start();
    } catch (IllegalStateException e) {
      Logging.e(TAG, "initDecode failed", e);
      release();
      return VideoCodecStatus.ERROR;
    }

    running = true;
    outputThread = createOutputThread();
    outputThread.start();

    return VideoCodecStatus.OK;
  }

  @Override
  public VideoCodecStatus decode(EncodedImage frame, DecodeInfo info) {
    decoderThreadChecker.checkIsOnValidThread();
    if (codec == null || callback == null) {
      return VideoCodecStatus.UNINITIALIZED;
    }

    if (frame.buffer == null) {
      Logging.e(TAG, "decode() - no input data");
      return VideoCodecStatus.ERR_PARAMETER;
    }

    int size = frame.buffer.remaining();
    if (size == 0) {
      Logging.e(TAG, "decode() - input buffer empty");
      return VideoCodecStatus.ERR_PARAMETER;
    }

    // Load dimensions from shared memory under the dimension lock.
    int width, height;
    synchronized (dimensionLock) {
      width = this.width;
      height = this.height;
    }

    // Check if the resolution changed and reset the codec if necessary.
    if (frame.encodedWidth * frame.encodedHeight > 0
        && (frame.encodedWidth != width || frame.encodedHeight != height)) {
      VideoCodecStatus status = reinitDecode(frame.encodedWidth, frame.encodedHeight);
      if (status != VideoCodecStatus.OK) {
        return status;
      }
    }

    if (keyFrameRequired) {
      // Need to process a key frame first.
      if (frame.frameType != EncodedImage.FrameType.VideoFrameKey) {
        Logging.e(TAG, "decode() - key frame required first");
        return VideoCodecStatus.ERROR;
      }
      if (!frame.completeFrame) {
        Logging.e(TAG, "decode() - complete frame required first");
        return VideoCodecStatus.ERROR;
      }
    }

    // TODO(mellem):  Support textures.
    int index;
    try {
      index = codec.dequeueInputBuffer(DEQUEUE_INPUT_TIMEOUT_US);
    } catch (IllegalStateException e) {
      Logging.e(TAG, "dequeueInputBuffer failed", e);
      return VideoCodecStatus.ERROR;
    }
    if (index < 0) {
      // Decoder is falling behind.  No input buffers available.
      // The decoder can't simply drop frames; it might lose a key frame.
      Logging.e(TAG, "decode() - no HW buffers available; decoder falling behind");
      return VideoCodecStatus.ERROR;
    }

    ByteBuffer buffer;
    try {
      buffer = codec.getInputBuffers()[index];
    } catch (IllegalStateException e) {
      Logging.e(TAG, "getInputBuffers failed", e);
      return VideoCodecStatus.ERROR;
    }

    if (buffer.capacity() < size) {
      Logging.e(TAG, "decode() - HW buffer too small");
      return VideoCodecStatus.ERROR;
    }
    buffer.put(frame.buffer);

    frameInfos.offer(new FrameInfo(SystemClock.elapsedRealtime(), frame.rotation));
    try {
      codec.queueInputBuffer(
          index, 0 /* offset */, size, frame.captureTimeMs * 1000, 0 /* flags */);
    } catch (IllegalStateException e) {
      Logging.e(TAG, "queueInputBuffer failed", e);
      frameInfos.pollLast();
      return VideoCodecStatus.ERROR;
    }
    if (keyFrameRequired) {
      keyFrameRequired = false;
    }
    return VideoCodecStatus.OK;
  }

  @Override
  public boolean getPrefersLateDecoding() {
    return true;
  }

  @Override
  public String getImplementationName() {
    return "HardwareVideoDecoder: " + codecName;
  }

  @Override
  public VideoCodecStatus release() {
    // TODO(sakal): This is not called on the correct thread but is still called synchronously.
    // Re-enable the check once this is called on the correct thread.
    // decoderThreadChecker.checkIsOnValidThread();
    try {
      // The outputThread actually stops and releases the codec once running is false.
      running = false;
      if (!ThreadUtils.joinUninterruptibly(outputThread, MEDIA_CODEC_RELEASE_TIMEOUT_MS)) {
        // Log an exception to capture the stack trace and turn it into a TIMEOUT error.
        Logging.e(TAG, "Media encoder release timeout", new RuntimeException());
        return VideoCodecStatus.TIMEOUT;
      }
      if (shutdownException != null) {
        // Log the exception and turn it into an error.  Wrap the exception in a new exception to
        // capture both the output thread's stack trace and this thread's stack trace.
        Logging.e(TAG, "Media encoder release error", new RuntimeException(shutdownException));
        shutdownException = null;
        return VideoCodecStatus.ERROR;
      }
    } finally {
      codec = null;
      callback = null;
      outputThread = null;
      frameInfos.clear();
    }
    return VideoCodecStatus.OK;
  }

  private VideoCodecStatus reinitDecode(int newWidth, int newHeight) {
    decoderThreadChecker.checkIsOnValidThread();
    VideoCodecStatus status = release();
    if (status != VideoCodecStatus.OK) {
      return status;
    }
    return initDecodeInternal(newWidth, newHeight, callback);
  }

  private Thread createOutputThread() {
    return new Thread("HardwareVideoDecoder.outputThread") {
      @Override
      public void run() {
        outputThreadChecker = new ThreadChecker();
        while (running) {
          deliverDecodedFrame();
        }
        releaseCodecOnOutputThread();
      }
    };
  }

  private void deliverDecodedFrame() {
    outputThreadChecker.checkIsOnValidThread();
    try {
      MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
      // Block until an output buffer is available (up to 100 milliseconds).  If the timeout is
      // exceeded, deliverDecodedFrame() will be called again on the next iteration of the output
      // thread's loop.  Blocking here prevents the output thread from busy-waiting while the codec
      // is idle.
      int result = codec.dequeueOutputBuffer(info, DEQUEUE_OUTPUT_BUFFER_TIMEOUT_US);
      if (result == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        reformat(codec.getOutputFormat());
        return;
      }

      if (result < 0) {
        Logging.v(TAG, "dequeueOutputBuffer returned " + result);
        return;
      }

      FrameInfo frameInfo = frameInfos.poll();
      Integer decodeTimeMs = null;
      int rotation = 0;
      if (frameInfo != null) {
        decodeTimeMs = (int) (SystemClock.elapsedRealtime() - frameInfo.decodeStartTimeMs);
        rotation = frameInfo.rotation;
      }

      hasDecodedFirstFrame = true;

      // Load dimensions from shared memory under the dimension lock.
      int width, height, stride, sliceHeight;
      synchronized (dimensionLock) {
        width = this.width;
        height = this.height;
        stride = this.stride;
        sliceHeight = this.sliceHeight;
      }

      // Output must be at least width * height bytes for Y channel, plus (width / 2) * (height / 2)
      // bytes for each of the U and V channels.
      if (info.size < width * height * 3 / 2) {
        Logging.e(TAG, "Insufficient output buffer size: " + info.size);
        return;
      }

      if (info.size < stride * height * 3 / 2 && sliceHeight == height && stride > width) {
        // Some codecs (Exynos) report an incorrect stride.  Correct it here.
        // Expected size == stride * height * 3 / 2.  A bit of algebra gives the correct stride as
        // 2 * size / (3 * height).
        stride = info.size * 2 / (height * 3);
      }

      ByteBuffer buffer = codec.getOutputBuffers()[result];
      buffer.position(info.offset);
      buffer.limit(info.size);

      final VideoFrame.I420Buffer frameBuffer;

      // TODO(mellem):  As an optimization, use libyuv via JNI to copy/reformatting data.
      if (colorFormat == CodecCapabilities.COLOR_FormatYUV420Planar) {
        if (sliceHeight % 2 == 0) {
          frameBuffer =
              createBufferFromI420(buffer, result, info.offset, stride, sliceHeight, width, height);
        } else {
          frameBuffer = new I420BufferImpl(width, height);
          // Optimal path is not possible because we have to copy the last rows of U- and V-planes.
          copyI420(buffer, info.offset, frameBuffer, stride, sliceHeight, width, height);
          codec.releaseOutputBuffer(result, false);
        }
      } else {
        frameBuffer = new I420BufferImpl(width, height);
        // All other supported color formats are NV12.
        nv12ToI420(buffer, info.offset, frameBuffer, stride, sliceHeight, width, height);
        codec.releaseOutputBuffer(result, false);
      }

      long presentationTimeNs = info.presentationTimeUs * 1000;
      VideoFrame frame = new VideoFrame(frameBuffer, rotation, presentationTimeNs, new Matrix());

      // Note that qp is parsed on the C++ side.
      callback.onDecodedFrame(frame, decodeTimeMs, null /* qp */);
      frame.release();
    } catch (IllegalStateException e) {
      Logging.e(TAG, "deliverDecodedFrame failed", e);
    }
  }

  private void reformat(MediaFormat format) {
    outputThreadChecker.checkIsOnValidThread();
    Logging.d(TAG, "Decoder format changed: " + format.toString());
    final int newWidth;
    final int newHeight;
    if (format.containsKey(MEDIA_FORMAT_KEY_CROP_LEFT)
        && format.containsKey(MEDIA_FORMAT_KEY_CROP_RIGHT)
        && format.containsKey(MEDIA_FORMAT_KEY_CROP_BOTTOM)
        && format.containsKey(MEDIA_FORMAT_KEY_CROP_TOP)) {
      newWidth = 1 + format.getInteger(MEDIA_FORMAT_KEY_CROP_RIGHT)
          - format.getInteger(MEDIA_FORMAT_KEY_CROP_LEFT);
      newHeight = 1 + format.getInteger(MEDIA_FORMAT_KEY_CROP_BOTTOM)
          - format.getInteger(MEDIA_FORMAT_KEY_CROP_TOP);
    } else {
      newWidth = format.getInteger(MediaFormat.KEY_WIDTH);
      newHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
    }
    // Compare to existing width, height, and save values under the dimension lock.
    synchronized (dimensionLock) {
      if (hasDecodedFirstFrame && (width != newWidth || height != newHeight)) {
        stopOnOutputThread(new RuntimeException("Unexpected size change. Configured " + width + "*"
            + height + ". New " + newWidth + "*" + newHeight));
        return;
      }
      width = newWidth;
      height = newHeight;
    }

    if (format.containsKey(MediaFormat.KEY_COLOR_FORMAT)) {
      colorFormat = format.getInteger(MediaFormat.KEY_COLOR_FORMAT);
      Logging.d(TAG, "Color: 0x" + Integer.toHexString(colorFormat));
      if (!isSupportedColorFormat(colorFormat)) {
        stopOnOutputThread(new IllegalStateException("Unsupported color format: " + colorFormat));
        return;
      }
    }

    // Save stride and sliceHeight under the dimension lock.
    synchronized (dimensionLock) {
      if (format.containsKey(MEDIA_FORMAT_KEY_STRIDE)) {
        stride = format.getInteger(MEDIA_FORMAT_KEY_STRIDE);
      }
      if (format.containsKey(MEDIA_FORMAT_KEY_SLICE_HEIGHT)) {
        sliceHeight = format.getInteger(MEDIA_FORMAT_KEY_SLICE_HEIGHT);
      }
      Logging.d(TAG, "Frame stride and slice height: " + stride + " x " + sliceHeight);
      stride = Math.max(width, stride);
      sliceHeight = Math.max(height, sliceHeight);
    }
  }

  private void releaseCodecOnOutputThread() {
    outputThreadChecker.checkIsOnValidThread();
    Logging.d(TAG, "Releasing MediaCodec on output thread");
    waitOutputBuffersReleasedOnOutputThread();
    try {
      codec.stop();
    } catch (Exception e) {
      Logging.e(TAG, "Media decoder stop failed", e);
    }
    try {
      codec.release();
    } catch (Exception e) {
      Logging.e(TAG, "Media decoder release failed", e);
      // Propagate exceptions caught during release back to the main thread.
      shutdownException = e;
    }
    codec = null;
    callback = null;
    outputThread = null;
    frameInfos.clear();
    Logging.d(TAG, "Release on output thread done");
  }

  private void waitOutputBuffersReleasedOnOutputThread() {
    outputThreadChecker.checkIsOnValidThread();
    synchronized (activeOutputBuffersLock) {
      while (activeOutputBuffers > 0) {
        Logging.d(TAG, "Waiting for all frames to be released.");
        try {
          activeOutputBuffersLock.wait();
        } catch (InterruptedException e) {
          Logging.e(TAG, "Interrupted while waiting for output buffers to be released.", e);
          return;
        }
      }
    }
  }

  private void stopOnOutputThread(Exception e) {
    outputThreadChecker.checkIsOnValidThread();
    running = false;
    shutdownException = e;
  }

  private boolean isSupportedColorFormat(int colorFormat) {
    for (int supported : MediaCodecUtils.DECODER_COLOR_FORMATS) {
      if (supported == colorFormat) {
        return true;
      }
    }
    return false;
  }

  private VideoFrame.I420Buffer createBufferFromI420(final ByteBuffer buffer,
      final int outputBufferIndex, final int offset, final int stride, final int sliceHeight,
      final int width, final int height) {
    final int uvStride = stride / 2;
    final int chromaWidth = (width + 1) / 2;
    final int chromaHeight = (height + 1) / 2;

    final int yPos = offset;
    final int uPos = yPos + stride * sliceHeight;
    final int vPos = uPos + uvStride * sliceHeight / 2;

    synchronized (activeOutputBuffersLock) {
      activeOutputBuffers++;
    }
    return new VideoFrame.I420Buffer() {
      private int refCount = 1;

      @Override
      public ByteBuffer getDataY() {
        ByteBuffer data = buffer.slice();
        data.position(yPos);
        data.limit(yPos + getStrideY() * height);
        return data;
      }

      @Override
      public ByteBuffer getDataU() {
        ByteBuffer data = buffer.slice();
        data.position(uPos);
        data.limit(uPos + getStrideU() * chromaHeight);
        return data;
      }

      @Override
      public ByteBuffer getDataV() {
        ByteBuffer data = buffer.slice();
        data.position(vPos);
        data.limit(vPos + getStrideV() * chromaHeight);
        return data;
      }

      @Override
      public int getStrideY() {
        return stride;
      }

      @Override
      public int getStrideU() {
        return uvStride;
      }

      @Override
      public int getStrideV() {
        return uvStride;
      }

      @Override
      public int getWidth() {
        return width;
      }

      @Override
      public int getHeight() {
        return height;
      }

      @Override
      public VideoFrame.I420Buffer toI420() {
        return this;
      }

      @Override
      public void retain() {
        refCount++;
      }

      @Override
      public void release() {
        refCount--;

        if (refCount == 0) {
          codec.releaseOutputBuffer(outputBufferIndex, false);
          synchronized (activeOutputBuffersLock) {
            activeOutputBuffers--;
            activeOutputBuffersLock.notifyAll();
          }
        }
      }
    };
  }

  private static void copyI420(ByteBuffer src, int offset, VideoFrame.I420Buffer frameBuffer,
      int stride, int sliceHeight, int width, int height) {
    int uvStride = stride / 2;
    int chromaWidth = (width + 1) / 2;
    // Note that hardware truncates instead of rounding.  WebRTC expects rounding, so the last
    // row will be duplicated if the sliceHeight is odd.
    int chromaHeight = (sliceHeight % 2 == 0) ? (height + 1) / 2 : height / 2;

    int yPos = offset;
    int uPos = yPos + stride * sliceHeight;
    int vPos = uPos + uvStride * sliceHeight / 2;

    copyPlane(
        src, yPos, stride, frameBuffer.getDataY(), 0, frameBuffer.getStrideY(), width, height);
    copyPlane(src, uPos, uvStride, frameBuffer.getDataU(), 0, frameBuffer.getStrideU(), chromaWidth,
        chromaHeight);
    copyPlane(src, vPos, uvStride, frameBuffer.getDataV(), 0, frameBuffer.getStrideV(), chromaWidth,
        chromaHeight);

    // If the sliceHeight is odd, duplicate the last rows of chroma.  Copy the last row of the U and
    // V channels and append them at the end of each channel.
    if (sliceHeight % 2 != 0) {
      int strideU = frameBuffer.getStrideU();
      int endU = chromaHeight * strideU;
      copyRow(frameBuffer.getDataU(), endU - strideU, frameBuffer.getDataU(), endU, chromaWidth);
      int strideV = frameBuffer.getStrideV();
      int endV = chromaHeight * strideV;
      copyRow(frameBuffer.getDataV(), endV - strideV, frameBuffer.getDataV(), endV, chromaWidth);
    }
  }

  private static void nv12ToI420(ByteBuffer src, int offset, VideoFrame.I420Buffer frameBuffer,
      int stride, int sliceHeight, int width, int height) {
    int yPos = offset;
    int uvPos = yPos + stride * sliceHeight;
    int chromaWidth = (width + 1) / 2;
    int chromaHeight = (height + 1) / 2;

    copyPlane(
        src, yPos, stride, frameBuffer.getDataY(), 0, frameBuffer.getStrideY(), width, height);

    // Split U and V rows.
    int dstUPos = 0;
    int dstVPos = 0;
    for (int i = 0; i < chromaHeight; ++i) {
      for (int j = 0; j < chromaWidth; ++j) {
        frameBuffer.getDataU().put(dstUPos + j, src.get(uvPos + j * 2));
        frameBuffer.getDataV().put(dstVPos + j, src.get(uvPos + j * 2 + 1));
      }
      dstUPos += frameBuffer.getStrideU();
      dstVPos += frameBuffer.getStrideV();
      uvPos += stride;
    }
  }

  private static void copyPlane(ByteBuffer src, int srcPos, int srcStride, ByteBuffer dst,
      int dstPos, int dstStride, int width, int height) {
    for (int i = 0; i < height; ++i) {
      copyRow(src, srcPos, dst, dstPos, width);
      srcPos += srcStride;
      dstPos += dstStride;
    }
  }

  private static void copyRow(ByteBuffer src, int srcPos, ByteBuffer dst, int dstPos, int width) {
    for (int i = 0; i < width; ++i) {
      dst.put(dstPos + i, src.get(srcPos + i));
    }
  }
}
