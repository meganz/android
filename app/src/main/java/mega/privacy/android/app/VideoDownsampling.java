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

import mega.privacy.android.app.lollipop.megachat.ChatUploadService;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.utils.conversion.VideoCompressionCallback;

public class VideoDownsampling {

    private static final int TIMEOUT_USEC = 10000;

    private static final String OUTPUT_VIDEO_MIME_TYPE = "video/avc";
    private static final int OUTPUT_VIDEO_BIT_RATE = 1280 * 720;
    private static final int OUTPUT_VIDEO_FRAME_RATE = 30;
    private static final int OUTPUT_VIDEO_IFRAME_INTERVAL = 10;
    private static final int OUTPUT_VIDEO_COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;

    private static final String OUTPUT_AUDIO_MIME_TYPE = "audio/mp4a-latm";
    private static final int OUTPUT_AUDIO_CHANNEL_COUNT = 2;
    private static final int OUTPUT_AUDIO_BIT_RATE = 128 * 1024;
    private static final int OUTPUT_AUDIO_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectHE;
    private static final int OUTPUT_AUDIO_SAMPLE_RATE_HZ = 44100;

    private int mWidth = 1280;
    private int mHeight = 720;

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

    public void changeResolution(File f, String inputFile, long idMessage) throws Throwable {
        log("changeResolution");

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
            log("changeResolutionInSeparatedThread");
            ChangerWrapper wrapper = new ChangerWrapper(changer);
            Thread th = new Thread(wrapper, ChangerWrapper.class.getSimpleName());
            th.start();
//            th.join();
            if (wrapper.mThrowable != null)
                throw wrapper.mThrowable;
        }
    }

    protected void prepareAndChangeResolution(VideoUpload video) throws Exception {
        log("prepareAndChangeResolution");
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

            resetWidthAndHeight(mInputFile);

            MediaFormat outputVideoFormat = MediaFormat.createVideoFormat(OUTPUT_VIDEO_MIME_TYPE, mWidth, mHeight);
            outputVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, OUTPUT_VIDEO_COLOR_FORMAT);
            outputVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_VIDEO_BIT_RATE);
            outputVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, OUTPUT_VIDEO_FRAME_RATE);
            outputVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, OUTPUT_VIDEO_IFRAME_INTERVAL);

            AtomicReference<Surface> inputSurfaceReference = new AtomicReference<Surface>();
            videoEncoder = createVideoEncoder(videoCodecInfo, outputVideoFormat, inputSurfaceReference);
            inputSurface = new InputSurface(inputSurfaceReference.get());
            inputSurface.makeCurrent();

            outputSurface = new OutputSurface();
            videoDecoder = createVideoDecoder(inputFormat, outputSurface.getSurface());

            audioExtractor = createExtractor(mInputFile);
            int audioInputTrack = getAndSelectAudioTrackIndex(audioExtractor);
            MediaFormat inputAudioFormat = audioExtractor.getTrackFormat(audioInputTrack);
            MediaFormat outputAudioFormat = MediaFormat.createAudioFormat(inputAudioFormat.getString(MediaFormat.KEY_MIME),
                    inputAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                    inputAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
            outputAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_AUDIO_BIT_RATE);
            outputAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, OUTPUT_AUDIO_AAC_PROFILE);

            audioEncoder = createAudioEncoder(audioCodecInfo, outputAudioFormat);
            audioDecoder = createAudioDecoder(inputAudioFormat);

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
            log("VideoDownsampling Exception: "+exception.toString());
            throw exception;
        }
        else{
            if(context instanceof ChatUploadService) {
                ((ChatUploadService)context).finishDownsampling(mOutputFile, true, video.idPendingMessage);
            }
        }
    }

    private void resetWidthAndHeight(String mInputFile) {
        MediaMetadataRetriever m = new MediaMetadataRetriever();
        m.setDataSource(mInputFile);
        Bitmap thumbnail = m.getFrameAtTime();
        int inputWidth = thumbnail.getWidth(), inputHeight = thumbnail.getHeight();
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
                                  MediaCodec audioDecoder, MediaCodec audioEncoder,
                                  MediaMuxer muxer,
                                  InputSurface inputSurface, OutputSurface outputSurface, VideoUpload video) {
        log("changeResolution");
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

        ByteBuffer[] audioDecoderInputBuffers = null;
        ByteBuffer[] audioDecoderOutputBuffers = null;
        ByteBuffer[] audioEncoderInputBuffers = null;
        ByteBuffer[] audioEncoderOutputBuffers = null;
        MediaCodec.BufferInfo audioDecoderOutputBufferInfo = null;
        MediaCodec.BufferInfo audioEncoderOutputBufferInfo = null;

        audioDecoderInputBuffers = audioDecoder.getInputBuffers();
        audioDecoderOutputBuffers =  audioDecoder.getOutputBuffers();
        audioEncoderInputBuffers = audioEncoder.getInputBuffers();
        audioEncoderOutputBuffers = audioEncoder.getOutputBuffers();
        audioDecoderOutputBufferInfo = new MediaCodec.BufferInfo();
        audioEncoderOutputBufferInfo = new MediaCodec.BufferInfo();

        MediaFormat decoderOutputVideoFormat = null;
        MediaFormat decoderOutputAudioFormat = null;
        MediaFormat encoderOutputVideoFormat = null;
        MediaFormat encoderOutputAudioFormat = null;
        int outputVideoTrack = -1;
        int outputAudioTrack = -1;

        boolean videoExtractorDone = false;
        boolean videoDecoderDone = false;
        boolean videoEncoderDone = false;

        boolean audioExtractorDone = false;
        boolean audioDecoderDone = false;
        boolean audioEncoderDone = false;

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
                if (videoExtractorDone)
                    videoDecoder.queueInputBuffer(decoderInputBufferIndex,0, 0, 0,  MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                break;
            }

            while (!audioExtractorDone && (encoderOutputAudioFormat == null || muxing)) {
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
                if (audioExtractorDone)
                    audioDecoder.queueInputBuffer(decoderInputBufferIndex, 0, 0,0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);

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

            while (!audioDecoderDone && pendingAudioDecoderOutputBufferIndex == -1 && (encoderOutputAudioFormat == null || muxing)) {
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

            while (pendingAudioDecoderOutputBufferIndex != -1) {
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

            while (!audioEncoderDone && (encoderOutputAudioFormat == null || muxing)) {
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
            if (!muxing && (encoderOutputAudioFormat != null) && (encoderOutputVideoFormat != null)) {
                outputVideoTrack = muxer.addTrack(encoderOutputVideoFormat);
                outputAudioTrack = muxer.addTrack(encoderOutputAudioFormat);
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

    private static void log(String log) {
        Util.log("VideoDownsampling", log);
    }
}