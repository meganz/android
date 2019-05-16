package mega.privacy.android.app;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.os.StatFs;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import mega.privacy.android.app.jobservices.SyncRecord;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.utils.conversion.VideoCompressionCallback;

import static android.media.MediaFormat.KEY_COLOR_FORMAT;
import static android.media.MediaFormat.KEY_FRAME_RATE;
import static android.media.MediaFormat.KEY_I_FRAME_INTERVAL;
import static android.media.MediaFormat.KEY_MIME;

public class VideoCompressor {

    private static final int TIMEOUT_USEC = 10000;

    private static final String OUTPUT_VIDEO_MIME_TYPE = "video/avc";
    private static final int OUTPUT_VIDEO_BIT_RATE = 1280 * 720;
    private static final int OUTPUT_VIDEO_FRAME_RATE = 30;
    private static final int OUTPUT_VIDEO_IFRAME_INTERVAL = 10;
    private static final int OUTPUT_VIDEO_COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;

    private static final String OUTPUT_AUDIO_MIME_TYPE = "audio/mp4a-latm";
    private static final int OUTPUT_AUDIO_BIT_RATE = 128 * 1024;
    private static final int OUTPUT_AUDIO_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectHE;

    private int mWidth;
    private int mHeight;

    private List<SyncRecord> pendingList;

    private String outputRoot;

    static ConcurrentLinkedQueue<VideoUpload> queue;

    private VideoCompressionCallback updater;

    private long totalInputSize;

    private long totalRead;

    private int totalCount;

    private int currentFileIndex;

    private boolean isRunning;

    private class VideoUpload {

        String original;

        String outFile;

        int percentage;

        long sizeInputFile;

        long sizeRead;

        VideoUpload(String original,String outFile,long sizeInputFile) {
            this.original = original;
            this.outFile = outFile;
            this.percentage = 0;
            this.sizeRead = 0;
            this.sizeInputFile = sizeInputFile;
        }

        @Override
        public String toString() {
            return "VideoUpload{" +
                    "original='" + original + '\'' +
                    ", outFile='" + outFile + '\'' +
                    ", percentage=" + percentage +
                    ", sizeInputFile=" + sizeInputFile +
                    ", sizeRead=" + sizeRead +
                    '}';
        }

    }

    public void stop() {
        isRunning = false;
        log("video compressor stopped");
    }

    public VideoCompressor(Context context,VideoCompressionCallback callback) {
        this.updater = callback;
        outputRoot = context.getCacheDir().toString() + File.separator;
        queue = new ConcurrentLinkedQueue<>();
    }

    public void setPendingList(List<SyncRecord> pendingList) {
        this.pendingList = pendingList;
        totalCount = pendingList.size();
        log("total compression videos count is " + totalCount);
        calculateTotalSize();
    }

    private void calculateTotalSize() {
        for (int i = 0;i < totalCount;i++) {
            totalInputSize += new File(pendingList.get(i).getLocalPath()).length();
        }
        log("total compression size is " + totalInputSize);
    }

    public long getTotalInputSize() {
        return this.totalInputSize;
    }

    public void setOutputRoot(String root) {
        this.outputRoot = root;
    }

    private boolean notEnoughSpace(long size) {
        double availableFreeSpace = Double.MAX_VALUE;
        try {
            StatFs stat = new StatFs(outputRoot);
            availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return (size > availableFreeSpace);
    }

    public void start() {
        isRunning = true;
        for (int i = 0;i < totalCount && isRunning;i++) {
            currentFileIndex = i + 1;
            SyncRecord record = pendingList.get(i);
            log("video compressor start: " + record.toString());
            String path = record.getLocalPath();
            File src = new File(path);
            long size = src.length();
            if (notEnoughSpace(size)) {
                updater.onInsufficientSpace();
                return;
            }

            VideoUpload video = new VideoUpload(path,record.getNewPath(),size);
            try {
                prepareAndChangeResolutionSingleThread(video);
                if (isRunning) {
                    updater.onCompressSuccessful(record);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                log(ex.getMessage());
                updater.onCompressFailed(record);
                currentFileIndex++;
                totalRead += size;
            }
        }
        updater.onCompressFinished(totalCount + "/" + totalCount);
        stop();
    }

    private void prepareAndChangeResolutionSingleThread(VideoUpload video) throws Exception {
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

            MediaFormat outputVideoFormat = MediaFormat.createVideoFormat(OUTPUT_VIDEO_MIME_TYPE,mWidth,mHeight);
            outputVideoFormat.setInteger(KEY_COLOR_FORMAT,OUTPUT_VIDEO_COLOR_FORMAT);
            outputVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE,OUTPUT_VIDEO_BIT_RATE);
            outputVideoFormat.setInteger(KEY_FRAME_RATE,OUTPUT_VIDEO_FRAME_RATE);
            outputVideoFormat.setInteger(KEY_I_FRAME_INTERVAL,OUTPUT_VIDEO_IFRAME_INTERVAL);

            AtomicReference<Surface> inputSurfaceReference = new AtomicReference<Surface>();
            videoEncoder = createVideoEncoder(videoCodecInfo,outputVideoFormat,inputSurfaceReference);
            inputSurface = new InputSurface(inputSurfaceReference.get());
            inputSurface.makeCurrent();

            outputSurface = new OutputSurface();
            videoDecoder = createVideoDecoder(inputFormat,outputSurface.getSurface());

            audioExtractor = createExtractor(mInputFile);
            int audioInputTrack = getAndSelectAudioTrackIndex(audioExtractor);
            MediaFormat inputAudioFormat = audioExtractor.getTrackFormat(audioInputTrack);
            MediaFormat outputAudioFormat = MediaFormat.createAudioFormat(inputAudioFormat.getString(KEY_MIME),
                    inputAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                    inputAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
            outputAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE,OUTPUT_AUDIO_BIT_RATE);
            outputAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,OUTPUT_AUDIO_AAC_PROFILE);

            audioEncoder = createAudioEncoder(audioCodecInfo,outputAudioFormat);
            audioDecoder = createAudioDecoder(inputAudioFormat);

            muxer = new MediaMuxer(mOutputFile,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            changeResolutionSingleThread(videoExtractor,audioExtractor,videoDecoder,videoEncoder,audioDecoder,audioEncoder,muxer,inputSurface,outputSurface,video);
        } finally {
            try {
                if (videoExtractor != null)
                    videoExtractor.release();
            } catch (Exception e) {
                if (exception == null)
                    exception = e;
            }
            try {
                if (audioExtractor != null)
                    audioExtractor.release();
            } catch (Exception e) {
                if (exception == null)
                    exception = e;
            }
            try {
                if (videoDecoder != null) {
                    videoDecoder.stop();
                    videoDecoder.release();
                }
            } catch (Exception e) {
                if (exception == null)
                    exception = e;
            }
            try {
                if (outputSurface != null) {
                    outputSurface.release();
                }
            } catch (Exception e) {
                if (exception == null)
                    exception = e;
            }
            try {
                if (videoEncoder != null) {
                    videoEncoder.stop();
                    videoEncoder.release();
                }
            } catch (Exception e) {
                if (exception == null)
                    exception = e;
            }
            try {
                if (audioDecoder != null) {
                    audioDecoder.stop();
                    audioDecoder.release();
                }
            } catch (Exception e) {
                if (exception == null)
                    exception = e;
            }
            try {
                if (audioEncoder != null) {
                    audioEncoder.stop();
                    audioEncoder.release();
                }
            } catch (Exception e) {
                if (exception == null)
                    exception = e;
            }
            try {
                if (muxer != null) {
                    muxer.stop();
                    muxer.release();
                }
            } catch (Exception e) {
                if (exception == null)
                    exception = e;
            }
            try {
                if (inputSurface != null)
                    inputSurface.release();
            } catch (Exception e) {
                if (exception == null)
                    exception = e;
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    private void resetWidthAndHeight(String mInputFile) {
        MediaMetadataRetriever m = new MediaMetadataRetriever();
        m.setDataSource(mInputFile);

        int rotation = Integer.valueOf(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
        if (rotation == 90 || rotation == 270) {
            mWidth = Integer.valueOf(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            mHeight = Integer.valueOf(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        } else {
            mWidth = Integer.valueOf(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            mHeight = Integer.valueOf(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        }
    }

    private MediaExtractor createExtractor(String mInputFile) throws IOException {
        MediaExtractor extractor;
        extractor = new MediaExtractor();
        extractor.setDataSource(mInputFile);
        return extractor;
    }

    /**
     * "video/avc" - H.264/AVC video
     * "video/hevc" - H.265/HEVC video
     * "video/mp4v-es" - MPEG4 video
     */
    private MediaCodec createVideoDecoder(MediaFormat inputFormat,Surface surface) throws IOException {
        MediaCodec decoder = MediaCodec.createDecoderByType(getMimeTypeFor(inputFormat));
        decoder.configure(inputFormat,surface,null,0);
        decoder.start();
        return decoder;
    }

    private MediaCodec createVideoEncoder(MediaCodecInfo codecInfo,MediaFormat format,AtomicReference<Surface> surfaceReference) throws IOException {
        MediaCodec encoder = MediaCodec.createByCodecName(codecInfo.getName());
        encoder.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        surfaceReference.set(encoder.createInputSurface());
        encoder.start();
        return encoder;
    }

    private MediaCodec createAudioDecoder(MediaFormat inputFormat) throws IOException {
        MediaCodec decoder = MediaCodec.createDecoderByType(getMimeTypeFor(inputFormat));
        decoder.configure(inputFormat,null,null,0);
        decoder.start();
        return decoder;
    }

    private MediaCodec createAudioEncoder(MediaCodecInfo codecInfo,MediaFormat format) throws IOException {
        MediaCodec encoder = MediaCodec.createByCodecName(codecInfo.getName());
        encoder.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();
        return encoder;
    }

    private int getAndSelectVideoTrackIndex(MediaExtractor extractor) {
        for (int index = 0;index < extractor.getTrackCount();++index) {
            if (isVideoFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index);
                return index;
            }
        }
        return -1;
    }

    private int getAndSelectAudioTrackIndex(MediaExtractor extractor) {
        for (int index = 0;index < extractor.getTrackCount();++index) {
            if (isAudioFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index);
                return index;
            }
        }
        return -1;
    }

    private void changeResolutionSingleThread(MediaExtractor videoExtractor,MediaExtractor audioExtractor,
                                              MediaCodec videoDecoder,MediaCodec videoEncoder,
                                              MediaCodec audioDecoder,MediaCodec audioEncoder,
                                              MediaMuxer muxer,
                                              InputSurface inputSurface,OutputSurface outputSurface,VideoUpload video) {
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
        audioDecoderOutputBuffers = audioDecoder.getOutputBuffers();
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
        while (((!videoEncoderDone) || (!audioEncoderDone)) && isRunning) {

            while (!videoExtractorDone && (encoderOutputVideoFormat == null || muxing)) {
                int decoderInputBufferIndex = videoDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER)
                    break;

                ByteBuffer decoderInputBuffer = videoDecoderInputBuffers[decoderInputBufferIndex];
                int size = videoExtractor.readSampleData(decoderInputBuffer,0);
                long presentationTime = videoExtractor.getSampleTime();
                totalRead += size;
                if (size >= 0) {
                    videoDecoder.queueInputBuffer(decoderInputBufferIndex,0,size,presentationTime,videoExtractor.getSampleFlags());
                }
                videoExtractorDone = !videoExtractor.advance();
                if (videoExtractorDone)
                    videoDecoder.queueInputBuffer(decoderInputBufferIndex,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                break;
            }

            while (!audioExtractorDone && (encoderOutputAudioFormat == null || muxing)) {
                int decoderInputBufferIndex = audioDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER)
                    break;

                ByteBuffer decoderInputBuffer = audioDecoderInputBuffers[decoderInputBufferIndex];
                int size = audioExtractor.readSampleData(decoderInputBuffer,0);
                long presentationTime = audioExtractor.getSampleTime();
                totalRead += size;
                if (size >= 0)
                    audioDecoder.queueInputBuffer(decoderInputBufferIndex,0,size,presentationTime,audioExtractor.getSampleFlags());

                audioExtractorDone = !audioExtractor.advance();
                if (audioExtractorDone)
                    audioDecoder.queueInputBuffer(decoderInputBufferIndex,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                break;
            }

            while (!videoDecoderDone && (encoderOutputVideoFormat == null || muxing)) {
                int decoderOutputBufferIndex = videoDecoder.dequeueOutputBuffer(videoDecoderOutputBufferInfo,TIMEOUT_USEC);
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
                    videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex,false);
                    break;
                }

                boolean render = videoDecoderOutputBufferInfo.size != 0;
                videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex,render);
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
                int decoderOutputBufferIndex = audioDecoder.dequeueOutputBuffer(audioDecoderOutputBufferInfo,TIMEOUT_USEC);
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
                    audioDecoder.releaseOutputBuffer(decoderOutputBufferIndex,false);
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
                    audioEncoder.queueInputBuffer(encoderInputBufferIndex,0,size,presentationTime,audioDecoderOutputBufferInfo.flags);
                }
                audioDecoder.releaseOutputBuffer(pendingAudioDecoderOutputBufferIndex,false);
                pendingAudioDecoderOutputBufferIndex = -1;
                if ((audioDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
                    audioDecoderDone = true;

                break;
            }

            while (!videoEncoderDone && (encoderOutputVideoFormat == null || muxing)) {
                int encoderOutputBufferIndex = videoEncoder.dequeueOutputBuffer(videoEncoderOutputBufferInfo,TIMEOUT_USEC);
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
                    videoEncoder.releaseOutputBuffer(encoderOutputBufferIndex,false);
                    break;
                }
                if (videoEncoderOutputBufferInfo.size != 0) {
                    muxer.writeSampleData(outputVideoTrack,encoderOutputBuffer,videoEncoderOutputBufferInfo);
                }
                if ((videoEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    videoEncoderDone = true;
                }
                videoEncoder.releaseOutputBuffer(encoderOutputBufferIndex,false);
                break;
            }

            while (!audioEncoderDone && (encoderOutputAudioFormat == null || muxing)) {
                int encoderOutputBufferIndex = audioEncoder.dequeueOutputBuffer(audioEncoderOutputBufferInfo,TIMEOUT_USEC);
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
                    audioEncoder.releaseOutputBuffer(encoderOutputBufferIndex,false);
                    break;
                }
                if (audioEncoderOutputBufferInfo.size != 0)
                    muxer.writeSampleData(outputAudioTrack,encoderOutputBuffer,audioEncoderOutputBufferInfo);
                if ((audioEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
                    audioEncoderDone = true;

                audioEncoder.releaseOutputBuffer(encoderOutputBufferIndex,false);
                break;
            }
            if (!muxing && (encoderOutputAudioFormat != null) && (encoderOutputVideoFormat != null)) {
                outputVideoTrack = muxer.addTrack(encoderOutputVideoFormat);
                outputAudioTrack = muxer.addTrack(encoderOutputAudioFormat);
                muxer.start();
                muxing = true;
            }
            int percentage = (int)Math.round((double)totalRead / totalInputSize * 100);
            if (percentage > 100) {
                percentage = 99;
            }
            updater.onCompressUpdateProgress(percentage,currentFileIndex + "/" + totalCount);
        }
    }

    private static boolean isVideoFormat(MediaFormat format) {
        return getMimeTypeFor(format).startsWith("video/");
    }

    private static boolean isAudioFormat(MediaFormat format) {
        return getMimeTypeFor(format).startsWith("audio/");
    }

    private static String getMimeTypeFor(MediaFormat format) {
        return format.getString(KEY_MIME);
    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0;i < numCodecs;i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0;j < types.length;j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    public boolean isRunning() {
        return isRunning;
    }

    private static void log(String message) {
        Util.log("VideoCompressor",message);
    }
}
