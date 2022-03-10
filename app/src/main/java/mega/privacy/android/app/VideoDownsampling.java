package mega.privacy.android.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import mega.privacy.android.app.main.megachat.ChatUploadService;
import mega.privacy.android.app.utils.conversion.VideoCompressionCallback;

import static mega.privacy.android.app.constants.SettingsConstants.VIDEO_QUALITY_HIGH;
import static mega.privacy.android.app.constants.SettingsConstants.VIDEO_QUALITY_MEDIUM;
import static mega.privacy.android.app.utils.LogUtil.*;

import androidx.annotation.Nullable;

public class VideoDownsampling {

    private static final int TIMEOUT_USEC = 10000;

    private static final String OUTPUT_VIDEO_MIME_TYPE = "video/avc";
    private static final int OUTPUT_VIDEO_FRAME_RATE = 30;
    private static final int OUTPUT_VIDEO_IFRAME_INTERVAL = 10;
    private static final int OUTPUT_VIDEO_COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;

    private static final String OUTPUT_AUDIO_MIME_TYPE = "audio/mp4a-latm";
    private static final int OUTPUT_AUDIO_CHANNEL_COUNT = 2;
    private static final int OUTPUT_AUDIO_BIT_RATE = 128 * 1024;
    private static final int OUTPUT_AUDIO_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectHE;
    private static final int OUTPUT_AUDIO_SAMPLE_RATE_HZ = 44100;

    private static final int SHORT_SIDE_SIZE_MEDIUM = 1080;
    private static final int SHORT_SIDE_SIZE_LOW = 720;

    protected int quality;

    private int mWidth;
    private int mHeight;
    private int resultWidth;
    private int resultHeight;

    static Context context;

    static ConcurrentLinkedQueue<VideoUpload> queue;

    private boolean isRunning = true;

    protected class VideoUpload{
        String original;
        String outFile;
        long idPendingMessage;
        int percentage;
        long sizeInputFile, sizeRead;
        public VideoUpload(String original, String outFile, long sizeInputFile, long idMessage) {
            this.original = original;
            this.outFile = outFile;
            this.percentage = 0;
            this.sizeRead = 0;
            this.sizeInputFile = sizeInputFile;
            this.idPendingMessage = idMessage;
        }
    }

    public VideoDownsampling(Context context) {
        this.context = context;
        queue = new ConcurrentLinkedQueue<VideoUpload>();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public void changeResolution(File f, String inputFile, long idMessage, int quality) throws Throwable {
        logDebug("changeResolution");

        this.quality = quality;

        queue.add(new VideoUpload(f.getAbsolutePath(), inputFile, f.length(), idMessage));

        ChangerWrapper.changeResolutionInSeparatedThread(this);
    }

    private static class ChangerWrapper implements Runnable {

        private Throwable mThrowable;
        private VideoDownsampling mChanger;

        private ChangerWrapper(VideoDownsampling changer) {
            mChanger = changer;
        }

        @Override
        public void run() {
            VideoUpload video = queue.peek();

            String out = video.outFile;

            try {
                while(!queue.isEmpty()){
                    VideoUpload toProcess = queue.poll();
                    mChanger.prepareAndChangeResolution(toProcess);
                }
            } catch (Throwable th) {
                mThrowable = th;
                if(out!=null){
                    if(context instanceof ChatUploadService) {
                        ((ChatUploadService)context).finishDownsampling(out, false, video.idPendingMessage);
                    }
                }
            }
        }

        public static void changeResolutionInSeparatedThread(VideoDownsampling changer) throws Throwable {
            logDebug("changeResolutionInSeparatedThread");
            ChangerWrapper wrapper = new ChangerWrapper(changer);
            Thread th = new Thread(wrapper, ChangerWrapper.class.getSimpleName());
            th.start();
//            th.join();
            if (wrapper.mThrowable != null)
                throw wrapper.mThrowable;
        }
    }

    /**
     * Creates the decoders and encoders to compress a video given a quality.
     *
     * Depending on quality these are the params the video encoder receives to perform the compression:
     *  - VIDEO_QUALITY_HIGH:
     *      * Original resolution.
     *      * Original frame rate.
     *      * Average bitrate reduced by 2%.
     *  - VIDEO_QUALITY_MEDIUM:
     *      * 1080p resolution if supported, the closest one if not.
     *      * OUTPUT_VIDEO_FRAME_RATE or the original one if smaller.
     *      * Half of average bitrate.
     *  - VIDEO_QUALITY_LOW:
     *      * 720p resolution if supported, the closest one if not.
     *      * OUTPUT_VIDEO_FRAME_RATE or the original one if smaller.
     *      * A third of average bitrate.
     *
     * @param video VideoUpload object containing the required info to compress a video.
     * @throws Exception If something wrong happens.
     */
    protected void prepareAndChangeResolution(VideoUpload video) throws Exception {
        logDebug("prepareAndChangeResolution");
        Exception exception = null;
        String mInputFile = video.original;

        String mOutputFile = video.outFile;

        MediaCodecInfo videoCodecInfo = selectCodec(OUTPUT_VIDEO_MIME_TYPE);
        if (videoCodecInfo == null)
            return;
        MediaCodecInfo audioCodecInfo = selectCodec(OUTPUT_AUDIO_MIME_TYPE);
        if (audioCodecInfo == null)
            return;

        MediaExtractor videoExtractor = null;
        MediaExtractor audioExtractor = null;
        OutputSurface outputSurface = null;
        MediaCodec videoDecoder = null;
        MediaCodec audioDecoder = null;
        MediaCodec videoEncoder = null;
        MediaCodec audioEncoder = null;
        MediaMuxer muxer = null;
        InputSurface inputSurface = null;
        try {
            videoExtractor = createExtractor(mInputFile);
            int videoInputTrack = getAndSelectVideoTrackIndex(videoExtractor);
            MediaFormat inputFormat = videoExtractor.getTrackFormat(videoInputTrack);
            MediaMetadataRetriever m = new MediaMetadataRetriever();
            m.setDataSource(mInputFile);

            getOriginalWidthAndHeight(m);

            resultWidth = mWidth;
            resultHeight = mHeight;

            int bitrate = Integer.parseInt(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
            int shortSideByQuality = quality == VIDEO_QUALITY_MEDIUM
                    ? SHORT_SIDE_SIZE_MEDIUM
                    : SHORT_SIDE_SIZE_LOW;

            int shortSide = Math.min(mWidth, mHeight);
            int frameRate = inputFormat.containsKey(MediaFormat.KEY_FRAME_RATE)
                    ? inputFormat.getInteger(MediaFormat.KEY_FRAME_RATE)
                    : OUTPUT_VIDEO_FRAME_RATE;

            logDebug("Video original width: " + mWidth + ", original height: " + mHeight
                    + ", average bitrate: " + bitrate + ", frame rate: " + frameRate);

            if (quality != VIDEO_QUALITY_HIGH) {
                bitrate = quality == VIDEO_QUALITY_MEDIUM
                        ? bitrate / 2
                        : bitrate / 3;

                frameRate = Math.min(frameRate, OUTPUT_VIDEO_FRAME_RATE);

                if (shortSide > shortSideByQuality) {
                    getCodecResolution(shortSideByQuality);
                }
            } else {
                // Since the METADATA_KEY_BITRATE is not the right value of the final bitrate
                // of a video but the average one, we can assume a 2% less to ensure the final size
                // is a bit less than the original one.
                bitrate *= 0.98;
            }

            logDebug("Video result width: " + resultWidth + ", result height: " + resultHeight
                    + ", encode bitrate: " + bitrate + ", encode frame rate: " + frameRate);

            MediaCodecInfo.VideoCapabilities capabilities = videoCodecInfo
                    .getCapabilitiesForType(OUTPUT_VIDEO_MIME_TYPE).getVideoCapabilities();

            boolean supported = capabilities.areSizeAndRateSupported(resultWidth, resultHeight, frameRate);

            if (!supported) {
                logWarning("Sizes width: " + resultWidth + " height: " + resultHeight + " not supported.");

                for (int i = shortSideByQuality; i< shortSide; i++) {
                    getCodecResolution(i);
                    supported = capabilities.areSizeAndRateSupported(resultWidth, resultHeight, frameRate);

                    if (supported) {
                        break;
                    }
                }
            }

            if (!supported && quality == VIDEO_QUALITY_MEDIUM) {
                logWarning("Sizes still not supported. Second try.");
                shortSideByQuality--;

                for (int i = shortSideByQuality; i > SHORT_SIDE_SIZE_LOW; i--) {
                    getCodecResolution(i);
                    supported = capabilities.areSizeAndRateSupported(resultWidth, resultHeight, frameRate);

                    if (supported) {
                        break;
                    }
                }
            }

            if (!supported) {
                String error = "Latest sizes width: " + resultWidth + " height: " + resultHeight + " not supported. " +
                        "Video not compressed, uploading original video.";
                logError(error);
                throw exception = new Exception(error);
            }

            MediaFormat outputVideoFormat = MediaFormat.createVideoFormat(OUTPUT_VIDEO_MIME_TYPE, resultWidth, resultHeight);
            outputVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, OUTPUT_VIDEO_COLOR_FORMAT);
            outputVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
            outputVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
            outputVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, OUTPUT_VIDEO_IFRAME_INTERVAL);

            AtomicReference<Surface> inputSurfaceReference = new AtomicReference<>();
            videoEncoder = createVideoEncoder(videoCodecInfo, outputVideoFormat, inputSurfaceReference);
            inputSurface = new InputSurface(inputSurfaceReference.get());
            inputSurface.makeCurrent();

            outputSurface = new OutputSurface();
            videoDecoder = createVideoDecoder(inputFormat, outputSurface.getSurface());

            audioExtractor = createExtractor(mInputFile);
            int audioInputTrack = getAndSelectAudioTrackIndex(audioExtractor);

            if (audioInputTrack >= 0) {
                MediaFormat inputAudioFormat = audioExtractor.getTrackFormat(audioInputTrack);
                MediaFormat outputAudioFormat = MediaFormat.createAudioFormat(inputAudioFormat.getString(MediaFormat.KEY_MIME),
                        inputAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                        inputAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
                outputAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_AUDIO_BIT_RATE);
                outputAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, OUTPUT_AUDIO_AAC_PROFILE);

                audioEncoder = createAudioEncoder(audioCodecInfo, outputAudioFormat);
                audioDecoder = createAudioDecoder(inputAudioFormat);
            }

            muxer = new MediaMuxer(mOutputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            changeResolution(videoExtractor, audioExtractor, videoDecoder, videoEncoder, audioDecoder, audioEncoder, muxer, inputSurface, outputSurface, video);
        } finally {
            try {
                if (videoExtractor != null)
                    videoExtractor.release();
            } catch(Exception e) {
                if (exception == null)
                    exception = e;
            }
            try {
                if (audioExtractor != null)
                    audioExtractor.release();
            } catch(Exception e) {
                if (exception == null)
                    exception = e;
            }
            try {
                if (videoDecoder != null) {
                    videoDecoder.stop();
                    videoDecoder.release();
                }
            } catch(Exception e) {
                if (exception == null)
                    exception = e;
            }
            try {
                if (outputSurface != null) {
                    outputSurface.release();
                }
            } catch(Exception e) {
                if (exception == null)
                    exception = e;
            }
            try {
                if (videoEncoder != null) {
                    videoEncoder.stop();
                    videoEncoder.release();
                }
            } catch(Exception e) {
                if (exception == null)
                    exception = e;
            }
            try {
                if (audioDecoder != null) {
                    audioDecoder.stop();
                    audioDecoder.release();
                }
            } catch(Exception e) {
                if (exception == null)
                    exception = e;
            }
            try {
                if (audioEncoder != null) {
                    audioEncoder.stop();
                    audioEncoder.release();
                }
            } catch(Exception e) {
                if (exception == null)
                    exception = e;
            }
            try {
                if (muxer != null) {
                    muxer.stop();
                    muxer.release();
                }
            } catch(Exception e) {
                if (exception == null)
                    exception = e;
            }
            try {
                if (inputSurface != null)
                    inputSurface.release();
            } catch(Exception e) {
                if (exception == null)
                    exception = e;
            }
        }
        if (exception != null){
            logError("Exception. Video not compressed, uploading original video.", exception);
            throw exception;
        }
        else{
            if(context instanceof ChatUploadService) {
                ((ChatUploadService)context).finishDownsampling(mOutputFile, true, video.idPendingMessage);
            }
        }
    }

    private void getOriginalWidthAndHeight(MediaMetadataRetriever m) {
        mWidth = Integer.parseInt(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        mHeight = Integer.parseInt(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        Bitmap thumbnail = m.getFrameAtTime();
        int inputWidth = thumbnail.getWidth();
        int inputHeight = thumbnail.getHeight();

        if (inputWidth > inputHeight) {
            if (mWidth < mHeight) {
                int w = mWidth;
                mWidth = mHeight;
                mHeight = w;
            }
        } else {
            if (mWidth > mHeight) {
                int w = mWidth;
                mWidth = mHeight;
                mHeight = w;
            }
        }
    }

    private MediaExtractor createExtractor(String mInputFile) throws IOException {
        MediaExtractor extractor;
        extractor = new MediaExtractor();
        extractor.setDataSource(mInputFile);
        return extractor;
    }

    private MediaCodec createVideoDecoder(MediaFormat inputFormat, Surface surface) throws IOException {
        MediaCodec decoder = MediaCodec.createDecoderByType(getMimeTypeFor(inputFormat));
        decoder.configure(inputFormat, surface, null, 0);
        decoder.start();
        return decoder;
    }

    private MediaCodec createVideoEncoder(MediaCodecInfo codecInfo, MediaFormat format, AtomicReference<Surface> surfaceReference) throws IOException {
        MediaCodec encoder = MediaCodec.createByCodecName(codecInfo.getName());
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        surfaceReference.set(encoder.createInputSurface());
        encoder.start();
        return encoder;
    }

    private MediaCodec createAudioDecoder(MediaFormat inputFormat) throws IOException {
        MediaCodec decoder = MediaCodec.createDecoderByType(getMimeTypeFor(inputFormat));
        decoder.configure(inputFormat, null, null, 0);
        decoder.start();
        return decoder;
    }

    private MediaCodec createAudioEncoder(MediaCodecInfo codecInfo, MediaFormat format) throws IOException {
        MediaCodec encoder = MediaCodec.createByCodecName(codecInfo.getName());
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();
        return encoder;
    }

    private int getAndSelectVideoTrackIndex(MediaExtractor extractor) {
        for (int index = 0; index < extractor.getTrackCount(); ++index) {
            if (isVideoFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index);
                return index;
            }
        }
        return -1;
    }
    private int getAndSelectAudioTrackIndex(MediaExtractor extractor) {
        for (int index = 0; index < extractor.getTrackCount(); ++index) {
            if (isAudioFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index);
                return index;
            }
        }
        return -1;
    }

    private void changeResolution(MediaExtractor videoExtractor, MediaExtractor audioExtractor,
                                  MediaCodec videoDecoder, MediaCodec videoEncoder,
                                  @Nullable MediaCodec audioDecoder,  @Nullable MediaCodec audioEncoder,
                                  MediaMuxer muxer,
                                  InputSurface inputSurface, OutputSurface outputSurface, VideoUpload video) {
        logDebug("changeResolution");
        String mOutputFile = video.outFile;

        ByteBuffer[] videoDecoderInputBuffers = null;
        ByteBuffer[] videoDecoderOutputBuffers = null;
        ByteBuffer[] videoEncoderOutputBuffers = null;
        MediaCodec.BufferInfo videoDecoderOutputBufferInfo = null;
        MediaCodec.BufferInfo videoEncoderOutputBufferInfo = null;

        videoDecoderInputBuffers = videoDecoder.getInputBuffers();
        videoDecoderOutputBuffers = videoDecoder.getOutputBuffers();
        videoEncoderOutputBuffers = videoEncoder.getOutputBuffers();
        videoDecoderOutputBufferInfo = new MediaCodec.BufferInfo();
        videoEncoderOutputBufferInfo = new MediaCodec.BufferInfo();

        MediaFormat decoderOutputVideoFormat = null;
        MediaFormat encoderOutputVideoFormat = null;
        int outputVideoTrack = -1;

        boolean videoExtractorDone = false;
        boolean videoDecoderDone = false;
        boolean videoEncoderDone = false;

        ByteBuffer[] audioDecoderInputBuffers = null;
        ByteBuffer[] audioDecoderOutputBuffers = null;
        ByteBuffer[] audioEncoderInputBuffers = null;
        ByteBuffer[] audioEncoderOutputBuffers = null;
        MediaCodec.BufferInfo audioDecoderOutputBufferInfo = null;
        MediaCodec.BufferInfo audioEncoderOutputBufferInfo = null;

        boolean audioExtractorDone = false;
        boolean audioDecoderDone = false;
        boolean audioEncoderDone = false;

        if (audioDecoder != null && audioEncoder != null) {
            audioDecoderInputBuffers = audioDecoder.getInputBuffers();
            audioDecoderOutputBuffers = audioDecoder.getOutputBuffers();
            audioEncoderInputBuffers = audioEncoder.getInputBuffers();
            audioEncoderOutputBuffers = audioEncoder.getOutputBuffers();
            audioDecoderOutputBufferInfo = new MediaCodec.BufferInfo();
            audioEncoderOutputBufferInfo = new MediaCodec.BufferInfo();
        } else {
            audioExtractorDone = true;
            audioDecoderDone = true;
            audioEncoderDone = true;
        }

        MediaFormat decoderOutputAudioFormat = null;
        MediaFormat encoderOutputAudioFormat = null;
        int outputAudioTrack = -1;
        int pendingAudioDecoderOutputBufferIndex = -1;

        boolean muxing = false;

        while ((!videoEncoderDone || !audioEncoderDone) && isRunning) {
            while (!videoExtractorDone && (encoderOutputVideoFormat == null || muxing)) {
                int decoderInputBufferIndex = videoDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER)
                    break;

                ByteBuffer decoderInputBuffer = videoDecoderInputBuffers[decoderInputBufferIndex];
                int size = videoExtractor.readSampleData(decoderInputBuffer, 0);
                long presentationTime = videoExtractor.getSampleTime();
                video.sizeRead += size;

                if (size >= 0) {
                    videoDecoder.queueInputBuffer(decoderInputBufferIndex,0, size, presentationTime, videoExtractor.getSampleFlags());
                }
                videoExtractorDone = !videoExtractor.advance();
                if (videoExtractorDone) {
                    decoderInputBufferIndex = videoDecoder.dequeueInputBuffer(-1);
                    videoDecoder.queueInputBuffer(
                            decoderInputBufferIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }
                break;
            }

            while (audioDecoder != null && audioDecoderInputBuffers != null
                    && !audioExtractorDone && (encoderOutputAudioFormat == null || muxing)) {
                int decoderInputBufferIndex = audioDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER)
                    break;

                ByteBuffer decoderInputBuffer = audioDecoderInputBuffers[decoderInputBufferIndex];
                int size = audioExtractor.readSampleData(decoderInputBuffer, 0);
                long presentationTime = audioExtractor.getSampleTime();
                video.sizeRead += size;

                if (size >= 0)
                    audioDecoder.queueInputBuffer(decoderInputBufferIndex, 0, size, presentationTime, audioExtractor.getSampleFlags());

                audioExtractorDone = !audioExtractor.advance();
                if (audioExtractorDone) {
                    decoderInputBufferIndex = audioDecoder.dequeueInputBuffer(-1);
                    audioDecoder.queueInputBuffer(
                            decoderInputBufferIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }

                break;
            }

            while (!videoDecoderDone && (encoderOutputVideoFormat == null || muxing)) {
                int decoderOutputBufferIndex = videoDecoder.dequeueOutputBuffer(videoDecoderOutputBufferInfo, TIMEOUT_USEC);
                if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER)
                    break;

                if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    videoDecoderOutputBuffers = videoDecoder.getOutputBuffers();
                    break;
                }
                if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    decoderOutputVideoFormat = videoDecoder.getOutputFormat();
                    break;
                }

                ByteBuffer decoderOutputBuffer = videoDecoderOutputBuffers[decoderOutputBufferIndex];
                if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
                    break;
                }

                boolean render = videoDecoderOutputBufferInfo.size != 0;
                videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, render);
                if (render) {
                    outputSurface.awaitNewImage();
                    outputSurface.drawImage();
                    inputSurface.setPresentationTime(videoDecoderOutputBufferInfo.presentationTimeUs * 1000);
                    inputSurface.swapBuffers();
                }
                if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    videoDecoderDone = true;
                    videoEncoder.signalEndOfInputStream();
                }
                break;
            }

            while (audioDecoder != null && audioDecoderOutputBuffers != null
                    && !audioDecoderDone && (encoderOutputAudioFormat == null || muxing)) {
                int decoderOutputBufferIndex = audioDecoder.dequeueOutputBuffer(audioDecoderOutputBufferInfo, TIMEOUT_USEC);
                if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER)
                    break;

                if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    audioDecoderOutputBuffers = audioDecoder.getOutputBuffers();
                    break;
                }
                if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    decoderOutputAudioFormat = audioDecoder.getOutputFormat();
                    break;
                }
                ByteBuffer decoderOutputBuffer = audioDecoderOutputBuffers[decoderOutputBufferIndex];
                if ((audioDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    audioDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
                    break;
                }
                pendingAudioDecoderOutputBufferIndex = decoderOutputBufferIndex;
                break;
            }

            while (audioEncoder != null && audioEncoderInputBuffers != null
                    && pendingAudioDecoderOutputBufferIndex != -1) {
                int encoderInputBufferIndex = audioEncoder.dequeueInputBuffer(TIMEOUT_USEC);
                ByteBuffer encoderInputBuffer = audioEncoderInputBuffers[encoderInputBufferIndex];
                int size = audioDecoderOutputBufferInfo.size;
                long presentationTime = audioDecoderOutputBufferInfo.presentationTimeUs;

                if (size >= 0) {
                    ByteBuffer decoderOutputBuffer = audioDecoderOutputBuffers[pendingAudioDecoderOutputBufferIndex].duplicate();
                    decoderOutputBuffer.position(audioDecoderOutputBufferInfo.offset);
                    decoderOutputBuffer.limit(audioDecoderOutputBufferInfo.offset + size);
                    encoderInputBuffer.position(0);
                    encoderInputBuffer.put(decoderOutputBuffer);
                    audioEncoder.queueInputBuffer(encoderInputBufferIndex,0, size, presentationTime, audioDecoderOutputBufferInfo.flags);
                }
                audioDecoder.releaseOutputBuffer(pendingAudioDecoderOutputBufferIndex, false);
                pendingAudioDecoderOutputBufferIndex = -1;
                if ((audioDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
                    audioDecoderDone = true;

                break;
            }

            while (!videoEncoderDone && (encoderOutputVideoFormat == null || muxing)) {
                int encoderOutputBufferIndex = videoEncoder.dequeueOutputBuffer(videoEncoderOutputBufferInfo, TIMEOUT_USEC);
                if (encoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER)
                    break;
                if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    videoEncoderOutputBuffers = videoEncoder.getOutputBuffers();
                    break;
                }
                if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    encoderOutputVideoFormat = videoEncoder.getOutputFormat();
                    break;
                }

                ByteBuffer encoderOutputBuffer = videoEncoderOutputBuffers[encoderOutputBufferIndex];
                if ((videoEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    videoEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                    break;
                }
                if (videoEncoderOutputBufferInfo.size != 0) {
                    muxer.writeSampleData(outputVideoTrack, encoderOutputBuffer, videoEncoderOutputBufferInfo);
                }
                if ((videoEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    videoEncoderDone = true;
                }
                videoEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                break;
            }

            while (audioEncoder != null && audioEncoderOutputBuffers != null
                    && !audioEncoderDone && (encoderOutputAudioFormat == null || muxing)) {
                int encoderOutputBufferIndex = audioEncoder.dequeueOutputBuffer(audioEncoderOutputBufferInfo, TIMEOUT_USEC);
                if (encoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break;
                }
                if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    audioEncoderOutputBuffers = audioEncoder.getOutputBuffers();
                    break;
                }
                if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    encoderOutputAudioFormat = audioEncoder.getOutputFormat();
                    break;
                }

                ByteBuffer encoderOutputBuffer = audioEncoderOutputBuffers[encoderOutputBufferIndex];
                if ((audioEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    audioEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                    break;
                }
                if (audioEncoderOutputBufferInfo.size != 0)
                    muxer.writeSampleData(outputAudioTrack, encoderOutputBuffer, audioEncoderOutputBufferInfo);
                if ((audioEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
                    audioEncoderDone = true;

                audioEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                break;
            }
            if (!muxing && encoderOutputVideoFormat != null) {
                outputVideoTrack = muxer.addTrack(encoderOutputVideoFormat);

                if (encoderOutputAudioFormat != null) {
                    outputAudioTrack = muxer.addTrack(encoderOutputAudioFormat);
                }

                muxer.start();
                muxing = true;
            }

            video.percentage = (int)((100 * video.sizeRead) / video.sizeInputFile);
//            log("The percentage complete is: " + video.percentage + " (" + video.sizeRead + "/" + video.sizeInputFile +")");
            if(video.percentage%5==0){
//                log("Percentage: "+mOutputFile + "  " + video.percentage + " (" + video.sizeInputFile + "/" + video.sizeInputFile +")");
                if(context instanceof  ChatUploadService) {
                    ((ChatUploadService)context).updateProgressDownsampling(video.percentage, mOutputFile);
                }
            }
            if(context instanceof VideoCompressionCallback) {
                ((VideoCompressionCallback) context).onCompressUpdateProgress(video.percentage);
            }
        }
        video.percentage = 100;
    }

    private static boolean isVideoFormat(MediaFormat format) {
        return getMimeTypeFor(format).startsWith("video/");
    }
    private static boolean isAudioFormat(MediaFormat format) {
        return getMimeTypeFor(format).startsWith("audio/");
    }
    private static String getMimeTypeFor(MediaFormat format) {
        return format.getString(MediaFormat.KEY_MIME);
    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    /**
     * Gets the resolution to compress the video.
     *
     * @param resolution Short side size.
     */
    private void getCodecResolution(int resolution) {
        if (mWidth > mHeight) {
            resultWidth = mWidth * resolution / mHeight;
            resultHeight = resolution;
        } else {
            resultWidth = resolution;
            resultHeight = mHeight * resolution / mWidth;
        }
    }
}