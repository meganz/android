package mega.privacy.android.app

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.view.Surface
import mega.privacy.android.app.main.megachat.ChatUploadService
import mega.privacy.android.app.utils.conversion.VideoCompressionCallback
import mega.privacy.android.data.compression.video.InputSurface
import mega.privacy.android.data.compression.video.OutputSurface
import mega.privacy.android.domain.entity.VideoQuality
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

/**
 * Class used to compress videos
 *
 * @property context [Context]
 * @property videoQuality The video quality
 */
open class VideoDownSampling(
    private var context: Context,
    private var videoQuality: Int = 0,
) {
    private var isRunning = true
    private var mWidth = 0
    private var mHeight = 0
    private var resultWidth = 0
    private var resultHeight = 0
    private val queue = ConcurrentLinkedQueue<VideoUpload>()

    private companion object {
        const val TIMEOUT_USEC = 10000
        const val OUTPUT_VIDEO_MIME_TYPE = "video/avc"
        const val OUTPUT_VIDEO_FRAME_RATE = 30
        const val OUTPUT_VIDEO_IFRAME_INTERVAL = 10
        const val OUTPUT_VIDEO_COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        const val OUTPUT_AUDIO_MIME_TYPE = "audio/mp4a-latm"
        const val OUTPUT_AUDIO_BIT_RATE = 128 * 1024
        const val OUTPUT_AUDIO_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectHE
        const val SHORT_SIDE_SIZE_MEDIUM = 1080
        const val SHORT_SIDE_SIZE_LOW = 720
    }

    /**
     * Checks if [VideoDownSampling] is running or not
     * @return Boolean value
     */
    fun isVideoDownSamplingRunning() = isRunning

    /**
     * Sets whether [VideoDownSampling] is running or not
     * @param isRunning True if the service is still running
     * @return Boolean value
     */
    fun setVideoDownSamplingRunning(isRunning: Boolean) {
        this.isRunning = isRunning
    }

    /**
     * Change the video resolution
     *
     * @param file [File]
     * @param inputFile The video path
     * @param idMessage The pending message ID
     * @param videoQuality The selected video quality to compress
     */
    @Throws(Throwable::class)
    fun changeResolution(file: File, inputFile: String, idMessage: Long, videoQuality: Int) {
        Timber.d("changeResolution")
        this.videoQuality = videoQuality
        val videoUpload = VideoUpload(
            originalPath = file.absolutePath,
            newPath = inputFile,
            size = file.length(),
            pendingMessageId = idMessage,
        )
        queue.add(videoUpload)
        val changeVideoResolutionWrapper = ChangeVideoResolutionWrapper(
            mChanger = this,
            queue = this.queue,
            context = this.context,
        )
        changeResolutionInSeparateThread(changeVideoResolutionWrapper)
    }

    /**
     * Creates the decoders and encoders to compress a video given a quality.
     *
     *
     * Depending on quality these are the params the video encoder receives to perform the compression:
     * - VIDEO_QUALITY_HIGH:
     * * Original resolution.
     * * Original frame rate.
     * * Average bitrate reduced by 2%.
     * - VIDEO_QUALITY_MEDIUM:
     * * 1080p resolution if supported, the closest one if not.
     * * OUTPUT_VIDEO_FRAME_RATE or the original one if smaller.
     * * Half of average bitrate.
     * - VIDEO_QUALITY_LOW:
     * * 720p resolution if supported, the closest one if not.
     * * OUTPUT_VIDEO_FRAME_RATE or the original one if smaller.
     * * A third of average bitrate.
     *
     * @param video VideoUpload object containing the required info to compress a video.
     * @throws Exception If something wrong happens.
     */
    @Throws(Exception::class)
    fun prepareAndChangeResolution(video: VideoUpload) {
        Timber.d("prepareAndChangeResolution")
        var exception: Exception? = null
        val mInputFile = video.originalPath
        val mOutputFile = video.newPath
        val videoCodecInfo = selectCodec(OUTPUT_VIDEO_MIME_TYPE)
            ?: return
        val audioCodecInfo = selectCodec(OUTPUT_AUDIO_MIME_TYPE)
            ?: return
        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var outputSurface: OutputSurface? = null
        var videoDecoder: MediaCodec? = null
        var audioDecoder: MediaCodec? = null
        var videoEncoder: MediaCodec? = null
        var audioEncoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: InputSurface? = null
        try {
            videoExtractor = createExtractor(mInputFile)
            val videoInputTrack = getAndSelectVideoTrackIndex(videoExtractor)
            val inputFormat = videoExtractor.getTrackFormat(videoInputTrack)
            val metadataRetriever = MediaMetadataRetriever()
            metadataRetriever.setDataSource(mInputFile)
            getOriginalWidthAndHeight(metadataRetriever)
            resultWidth = mWidth
            resultHeight = mHeight
            var bitrate =
                (metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    ?: return)
                    .toInt()
            var shortSideByQuality =
                if (videoQuality == VideoQuality.MEDIUM.value) SHORT_SIDE_SIZE_MEDIUM else SHORT_SIDE_SIZE_LOW
            val shortSide = mWidth.coerceAtMost(mHeight)
            var frameRate =
                if (inputFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) inputFormat.getInteger(
                    MediaFormat.KEY_FRAME_RATE
                ) else OUTPUT_VIDEO_FRAME_RATE
            Timber.d(
                "Video original width: " + mWidth + ", original height: " + mHeight
                        + ", average bitrate: " + bitrate + ", frame rate: " + frameRate
            )
            if (videoQuality == VideoQuality.HIGH.value) {
                // Since the METADATA_KEY_BITRATE is not the right value of the final bitrate
                // of a video but the average one, we can assume a 2% less to ensure the final size
                // is a bit less than the original one.
                bitrate *= 0.98.toInt()
            } else {
                bitrate =
                    if (videoQuality == VideoQuality.MEDIUM.value) bitrate / 2 else bitrate / 3
                frameRate = frameRate.coerceAtMost(OUTPUT_VIDEO_FRAME_RATE)
                if (shortSide > shortSideByQuality) {
                    getCodecResolution(shortSideByQuality)
                }
            }
            Timber.d(
                "Video result width: " + resultWidth + ", result height: " + resultHeight
                        + ", encode bitrate: " + bitrate + ", encode frame rate: " + frameRate
            )
            val capabilities = videoCodecInfo
                .getCapabilitiesForType(OUTPUT_VIDEO_MIME_TYPE).videoCapabilities
            var supported = capabilities.areSizeAndRateSupported(
                resultWidth,
                resultHeight,
                frameRate.toDouble()
            )
            if (!supported) {
                Timber.w("Sizes width: %d height: %d not supported.", resultWidth, resultHeight)
                for (i in shortSideByQuality until shortSide) {
                    getCodecResolution(i)
                    supported = capabilities.areSizeAndRateSupported(
                        resultWidth,
                        resultHeight,
                        frameRate.toDouble()
                    )
                    if (supported) {
                        break
                    }
                }
            }
            if (!supported && videoQuality == VideoQuality.MEDIUM.value) {
                Timber.w("Sizes still not supported. Second try.")
                shortSideByQuality--
                for (i in shortSideByQuality downTo SHORT_SIDE_SIZE_LOW + 1) {
                    getCodecResolution(i)
                    supported = capabilities.areSizeAndRateSupported(
                        resultWidth,
                        resultHeight,
                        frameRate.toDouble()
                    )
                    if (supported) {
                        break
                    }
                }
            }
            if (!supported) {
                val error =
                    "Latest sizes width: " + resultWidth + " height: " + resultHeight + " not supported. " +
                            "Video not compressed, uploading original video."
                Timber.e(error)
                throw Exception(error).also { exception = it }
            }
            val outputVideoFormat =
                MediaFormat.createVideoFormat(OUTPUT_VIDEO_MIME_TYPE, resultWidth, resultHeight)
            outputVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, OUTPUT_VIDEO_COLOR_FORMAT)
            outputVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            outputVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            outputVideoFormat.setInteger(
                MediaFormat.KEY_I_FRAME_INTERVAL,
                OUTPUT_VIDEO_IFRAME_INTERVAL
            )
            val inputSurfaceReference = AtomicReference<Surface>()
            videoEncoder =
                createVideoEncoder(videoCodecInfo, outputVideoFormat, inputSurfaceReference)
            inputSurface = InputSurface(inputSurfaceReference.get())
            inputSurface.makeCurrent()
            outputSurface = OutputSurface()
            videoDecoder = createVideoDecoder(inputFormat, outputSurface.surface)
            audioExtractor = createExtractor(mInputFile)
            val audioInputTrack = getAndSelectAudioTrackIndex(audioExtractor)
            if (audioInputTrack >= 0) {
                val inputAudioFormat = audioExtractor.getTrackFormat(audioInputTrack)
                val outputAudioFormat = MediaFormat.createAudioFormat(
                    inputAudioFormat.getString(MediaFormat.KEY_MIME) ?: return,
                    inputAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                    inputAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                )
                outputAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_AUDIO_BIT_RATE)
                outputAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, OUTPUT_AUDIO_AAC_PROFILE)
                audioEncoder = createAudioEncoder(audioCodecInfo, outputAudioFormat)
                audioDecoder = createAudioDecoder(inputAudioFormat)
            }
            muxer = MediaMuxer(mOutputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            changeResolution(
                videoExtractor = videoExtractor,
                audioExtractor = audioExtractor,
                videoDecoder = videoDecoder,
                videoEncoder = videoEncoder,
                audioDecoder = audioDecoder,
                audioEncoder = audioEncoder,
                muxer = muxer,
                inputSurface = inputSurface,
                outputSurface = outputSurface,
                video = video,
            )
        } finally {
            try {
                videoExtractor?.release()
            } catch (e: Exception) {
                if (exception == null) exception = e
            }
            try {
                audioExtractor?.release()
            } catch (e: Exception) {
                if (exception == null) exception = e
            }
            try {
                if (videoDecoder != null) {
                    videoDecoder.stop()
                    videoDecoder.release()
                }
            } catch (e: Exception) {
                if (exception == null) exception = e
            }
            try {
                outputSurface?.release()
            } catch (e: Exception) {
                if (exception == null) exception = e
            }
            try {
                if (videoEncoder != null) {
                    videoEncoder.stop()
                    videoEncoder.release()
                }
            } catch (e: Exception) {
                if (exception == null) exception = e
            }
            try {
                if (audioDecoder != null) {
                    audioDecoder.stop()
                    audioDecoder.release()
                }
            } catch (e: Exception) {
                if (exception == null) exception = e
            }
            try {
                if (audioEncoder != null) {
                    audioEncoder.stop()
                    audioEncoder.release()
                }
            } catch (e: Exception) {
                if (exception == null) exception = e
            }
            try {
                if (muxer != null) {
                    muxer.stop()
                    muxer.release()
                }
            } catch (e: Exception) {
                if (exception == null) exception = e
            }
            try {
                inputSurface?.release()
            } catch (e: Exception) {
                if (exception == null) exception = e
            }
        }
        if (exception != null) {
            Timber.e(exception, "Exception. Video not compressed, uploading original video.")
            throw exception as Exception
        } else {
            if (context is ChatUploadService) {
                (context as ChatUploadService).finishDownsampling(
                    returnedFile = mOutputFile,
                    success = true,
                    idPendingMessage = video.pendingMessageId,
                )
            }
        }
    }

    /**
     * Instantiates a [Thread] to change video resolution there
     *
     * @param changeVideoResolutionWrapper [ChangeVideoResolutionWrapper]
     */
    private fun changeResolutionInSeparateThread(changeVideoResolutionWrapper: ChangeVideoResolutionWrapper) {
        Timber.d("changeResolutionInSeparatedThread")
        val thread = Thread(
            changeVideoResolutionWrapper,
            ChangeVideoResolutionWrapper::class.java.simpleName
        )
        thread.start()
        changeVideoResolutionWrapper.shouldThrow()
    }

    /**
     * Retrieves the original width and height of the video
     *
     * @param metadataRetriever The video metadata
     */
    private fun getOriginalWidthAndHeight(metadataRetriever: MediaMetadataRetriever) {
        mWidth =
            (metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?: return).toInt()
        mHeight =
            (metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?: return)
                .toInt()
        val thumbnail = metadataRetriever.frameAtTime
        val inputWidth = (thumbnail ?: return).width
        val inputHeight = thumbnail.height
        if (inputWidth > inputHeight) {
            if (mWidth < mHeight) {
                val w = mWidth
                mWidth = mHeight
                mHeight = w
            }
        } else {
            if (mWidth > mHeight) {
                val w = mWidth
                mWidth = mHeight
                mHeight = w
            }
        }
    }

    /**
     * Creates a video extractor
     *
     * @param mInputFile The video input file
     * @return [MediaExtractor]
     */
    @Throws(IOException::class)
    private fun createExtractor(mInputFile: String): MediaExtractor {
        MediaExtractor().also {
            it.setDataSource(mInputFile)
            return it
        }
    }

    /**
     * Creates a video decoder
     *
     * @param inputFormat [MediaFormat]
     * @param surface [Surface]
     * @return [MediaCodec]
     */
    @Throws(IOException::class)
    private fun createVideoDecoder(inputFormat: MediaFormat, surface: Surface?): MediaCodec {
        val decoder = MediaCodec.createDecoderByType(getMimeTypeFor(inputFormat)!!)
        decoder.configure(inputFormat, surface, null, 0)
        decoder.start()
        return decoder
    }

    /**
     * Creates a video encoder
     *
     * @param codecInfo [MediaCodecInfo]
     * @param format [MediaFormat]
     * @param surfaceReference [AtomicReference]
     * @return [MediaCodec]
     */
    @Throws(IOException::class)
    private fun createVideoEncoder(
        codecInfo: MediaCodecInfo,
        format: MediaFormat,
        surfaceReference: AtomicReference<Surface>,
    ): MediaCodec {
        val encoder = MediaCodec.createByCodecName(codecInfo.name)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        surfaceReference.set(encoder.createInputSurface())
        encoder.start()
        return encoder
    }

    /**
     * Creates an audio decoder
     *
     * @param inputFormat [MediaFormat]
     * @return [MediaCodec]
     */
    @Throws(IOException::class)
    private fun createAudioDecoder(inputFormat: MediaFormat): MediaCodec {
        val decoder = MediaCodec.createDecoderByType(getMimeTypeFor(inputFormat)!!)
        decoder.configure(inputFormat, null, null, 0)
        decoder.start()
        return decoder
    }

    /**
     * Creates an audio encoder
     *
     * @param codecInfo [MediaCodecInfo]
     * @param format [MediaFormat]
     * @return [MediaCodec]
     */
    @Throws(IOException::class)
    private fun createAudioEncoder(codecInfo: MediaCodecInfo, format: MediaFormat): MediaCodec {
        val encoder = MediaCodec.createByCodecName(codecInfo.name)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()
        return encoder
    }

    /**
     * Retrieves the video track index
     *
     * @param extractor [MediaExtractor]
     * @return [Int] represented as a video track index
     */
    private fun getAndSelectVideoTrackIndex(extractor: MediaExtractor?): Int {
        for (index in 0 until extractor!!.trackCount) {
            if (isVideoFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index)
                return index
            }
        }
        return -1
    }

    /**
     * Retrieves the audio track index
     *
     * @param extractor [MediaExtractor]
     * @return [Int] represented as an audio track index
     */
    private fun getAndSelectAudioTrackIndex(extractor: MediaExtractor?): Int {
        for (index in 0 until extractor!!.trackCount) {
            if (isAudioFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index)
                return index
            }
        }
        return -1
    }

    /**
     * Changes the video resolution
     *
     * @param videoExtractor [MediaExtractor]
     * @param audioExtractor [MediaExtractor]
     * @param videoDecoder [MediaCodec]
     * @param videoEncoder [MediaCodec]
     * @param [audioDecoder] [MediaCodec]
     * @param [audioEncoder] [MediaCodec]
     * @param [muxer] [MediaMuxer]
     * @param [inputSurface] [InputSurface]
     * @param [outputSurface] [OutputSurface]
     * @param [video] [VideoUpload]
     */
    @Suppress("DEPRECATION")
    private fun changeResolution(
        videoExtractor: MediaExtractor?, audioExtractor: MediaExtractor?,
        videoDecoder: MediaCodec?, videoEncoder: MediaCodec?,
        audioDecoder: MediaCodec?, audioEncoder: MediaCodec?,
        muxer: MediaMuxer,
        inputSurface: InputSurface,
        outputSurface: OutputSurface,
        video: VideoUpload,
    ) {
        Timber.d("changeResolution")
        val mOutputFile = video.newPath
        val videoDecoderInputBuffers: Array<ByteBuffer?>?
        var videoEncoderOutputBuffers: Array<ByteBuffer?>?
        val videoDecoderOutputBufferInfo: MediaCodec.BufferInfo?
        val videoEncoderOutputBufferInfo: MediaCodec.BufferInfo?
        videoDecoderInputBuffers = (videoDecoder ?: return).inputBuffers
        videoEncoderOutputBuffers = (videoEncoder ?: return).outputBuffers
        videoDecoderOutputBufferInfo = MediaCodec.BufferInfo()
        videoEncoderOutputBufferInfo = MediaCodec.BufferInfo()
        var encoderOutputVideoFormat: MediaFormat? = null
        var outputVideoTrack = -1
        var videoExtractorDone = false
        var videoDecoderDone = false
        var videoEncoderDone = false
        var audioDecoderInputBuffers: Array<ByteBuffer?>? = null
        var audioDecoderOutputBuffers: Array<ByteBuffer>? = null
        var audioEncoderInputBuffers: Array<ByteBuffer>? = null
        var audioEncoderOutputBuffers: Array<ByteBuffer?>? = null
        var audioDecoderOutputBufferInfo: MediaCodec.BufferInfo? = null
        var audioEncoderOutputBufferInfo: MediaCodec.BufferInfo? = null
        var audioExtractorDone = false
        var audioDecoderDone = false
        var audioEncoderDone = false
        if (audioDecoder != null && audioEncoder != null) {
            audioDecoderInputBuffers = audioDecoder.inputBuffers
            audioDecoderOutputBuffers = audioDecoder.outputBuffers
            audioEncoderInputBuffers = audioEncoder.inputBuffers
            audioEncoderOutputBuffers = audioEncoder.outputBuffers
            audioDecoderOutputBufferInfo = MediaCodec.BufferInfo()
            audioEncoderOutputBufferInfo = MediaCodec.BufferInfo()
        } else {
            audioExtractorDone = true
            audioDecoderDone = true
            audioEncoderDone = true
        }
        var encoderOutputAudioFormat: MediaFormat? = null
        var outputAudioTrack = -1
        var pendingAudioDecoderOutputBufferIndex = -1
        var muxing = false
        while ((!videoEncoderDone || !audioEncoderDone) && isVideoDownSamplingRunning()) {
            while (!videoExtractorDone && (encoderOutputVideoFormat == null || muxing)) {
                var decoderInputBufferIndex = videoDecoder.dequeueInputBuffer(TIMEOUT_USEC.toLong())
                if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
                val decoderInputBuffer = videoDecoderInputBuffers[decoderInputBufferIndex]
                val size =
                    (videoExtractor ?: return).readSampleData(decoderInputBuffer ?: return, 0)
                val presentationTime = videoExtractor.sampleTime
                video.readSize += size.toLong()
                if (size >= 0) {
                    videoDecoder.queueInputBuffer(
                        decoderInputBufferIndex,
                        0,
                        size,
                        presentationTime,
                        videoExtractor.sampleFlags
                    )
                }
                videoExtractorDone = !videoExtractor.advance()
                if (videoExtractorDone) {
                    decoderInputBufferIndex = videoDecoder.dequeueInputBuffer(-1)
                    videoDecoder.queueInputBuffer(
                        decoderInputBufferIndex,
                        0,
                        0,
                        0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                }
                break
            }
            while (audioDecoder != null && audioDecoderInputBuffers != null && !audioExtractorDone && (encoderOutputAudioFormat == null || muxing)) {
                var decoderInputBufferIndex = audioDecoder.dequeueInputBuffer(TIMEOUT_USEC.toLong())
                if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
                val decoderInputBuffer = audioDecoderInputBuffers[decoderInputBufferIndex]
                val size =
                    (audioExtractor ?: return).readSampleData(decoderInputBuffer ?: return, 0)
                val presentationTime = audioExtractor.sampleTime
                video.readSize += size.toLong()
                if (size >= 0) audioDecoder.queueInputBuffer(
                    decoderInputBufferIndex,
                    0,
                    size,
                    presentationTime,
                    audioExtractor.sampleFlags
                )
                audioExtractorDone = !audioExtractor.advance()
                if (audioExtractorDone) {
                    decoderInputBufferIndex = audioDecoder.dequeueInputBuffer(-1)
                    audioDecoder.queueInputBuffer(
                        decoderInputBufferIndex,
                        0,
                        0,
                        0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                }
                break
            }
            while (!videoDecoderDone && (encoderOutputVideoFormat == null || muxing)) {
                val decoderOutputBufferIndex = videoDecoder.dequeueOutputBuffer(
                    videoDecoderOutputBufferInfo,
                    TIMEOUT_USEC.toLong()
                )
                if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
                if (videoDecoderOutputBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false)
                    break
                }
                val render = videoDecoderOutputBufferInfo.size != 0
                videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, render)
                if (render) {
                    outputSurface.awaitNewImage()
                    outputSurface.drawImage()
                    inputSurface.setPresentationTime(videoDecoderOutputBufferInfo.presentationTimeUs * 1000)
                    inputSurface.swapBuffers()
                }
                if (videoDecoderOutputBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    videoDecoderDone = true
                    videoEncoder.signalEndOfInputStream()
                }
                break
            }
            while (audioDecoder != null && audioDecoderOutputBuffers != null && !audioDecoderDone && (encoderOutputAudioFormat == null || muxing)) {
                val decoderOutputBufferIndex = audioDecoder.dequeueOutputBuffer(
                    audioDecoderOutputBufferInfo ?: return, TIMEOUT_USEC.toLong()
                )
                if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
                if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    audioDecoderOutputBuffers = audioDecoder.outputBuffers
                    break
                }
                if (audioDecoderOutputBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    audioDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false)
                    break
                }
                pendingAudioDecoderOutputBufferIndex = decoderOutputBufferIndex
                break
            }
            while (audioEncoder != null && audioEncoderInputBuffers != null && pendingAudioDecoderOutputBufferIndex != -1) {
                val encoderInputBufferIndex = audioEncoder.dequeueInputBuffer(TIMEOUT_USEC.toLong())
                val encoderInputBuffer = audioEncoderInputBuffers[encoderInputBufferIndex]
                val size = (audioDecoderOutputBufferInfo ?: return).size
                val presentationTime = audioDecoderOutputBufferInfo.presentationTimeUs
                if (size >= 0) {
                    val decoderOutputBuffer =
                        (audioDecoderOutputBuffers
                            ?: return)[pendingAudioDecoderOutputBufferIndex].duplicate()
                    decoderOutputBuffer.position(audioDecoderOutputBufferInfo.offset)
                    decoderOutputBuffer.limit(audioDecoderOutputBufferInfo.offset + size)
                    encoderInputBuffer.position(0)
                    encoderInputBuffer.put(decoderOutputBuffer)
                    audioEncoder.queueInputBuffer(
                        encoderInputBufferIndex,
                        0,
                        size,
                        presentationTime,
                        audioDecoderOutputBufferInfo.flags
                    )
                }
                (audioDecoder ?: return).releaseOutputBuffer(
                    pendingAudioDecoderOutputBufferIndex,
                    false
                )
                pendingAudioDecoderOutputBufferIndex = -1
                if (audioDecoderOutputBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) audioDecoderDone =
                    true
                break
            }
            while (!videoEncoderDone && (encoderOutputVideoFormat == null || muxing)) {
                val encoderOutputBufferIndex = videoEncoder.dequeueOutputBuffer(
                    videoEncoderOutputBufferInfo,
                    TIMEOUT_USEC.toLong()
                )
                if (encoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
                if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    videoEncoderOutputBuffers = videoEncoder.outputBuffers
                    break
                }
                if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    encoderOutputVideoFormat = videoEncoder.outputFormat
                    break
                }
                val encoderOutputBuffer =
                    (videoEncoderOutputBuffers ?: return)[encoderOutputBufferIndex]
                if (videoEncoderOutputBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    videoEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false)
                    break
                }
                if (videoEncoderOutputBufferInfo.size != 0) {
                    muxer.writeSampleData(
                        outputVideoTrack,
                        encoderOutputBuffer ?: return,
                        videoEncoderOutputBufferInfo
                    )
                }
                if (videoEncoderOutputBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    videoEncoderDone = true
                }
                videoEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false)
                break
            }
            while (audioEncoder != null && audioEncoderOutputBuffers != null && !audioEncoderDone && (encoderOutputAudioFormat == null || muxing)) {
                val encoderOutputBufferIndex = audioEncoder.dequeueOutputBuffer(
                    audioEncoderOutputBufferInfo ?: return, TIMEOUT_USEC.toLong()
                )
                if (encoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                }
                if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    audioEncoderOutputBuffers = audioEncoder.outputBuffers
                    break
                }
                if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    encoderOutputAudioFormat = audioEncoder.outputFormat
                    break
                }
                val encoderOutputBuffer = audioEncoderOutputBuffers[encoderOutputBufferIndex]
                if (audioEncoderOutputBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    audioEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false)
                    break
                }
                if (audioEncoderOutputBufferInfo.size != 0) muxer.writeSampleData(
                    outputAudioTrack,
                    encoderOutputBuffer ?: return,
                    audioEncoderOutputBufferInfo
                )
                if (audioEncoderOutputBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) audioEncoderDone =
                    true
                audioEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false)
                break
            }
            if (!muxing && encoderOutputVideoFormat != null) {
                outputVideoTrack = muxer.addTrack(encoderOutputVideoFormat)
                if (encoderOutputAudioFormat != null) {
                    outputAudioTrack = muxer.addTrack(encoderOutputAudioFormat)
                }
                muxer.start()
                muxing = true
            }
            video.compressionPercentage = (100 * video.readSize / video.size).toInt()
            if (video.compressionPercentage % 5 == 0) {
                if (context is ChatUploadService) {
                    (context as ChatUploadService).updateProgressDownsampling(
                        percentage = video.compressionPercentage,
                        key = mOutputFile,
                    )
                }
            }
            if (context is VideoCompressionCallback) {
                (context as VideoCompressionCallback).onCompressUpdateProgress(video.compressionPercentage)
            }
        }
        video.compressionPercentage = 100
    }

    /**
     * Gets the resolution to compress the video.
     *
     * @param resolution Short side size.
     */
    private fun getCodecResolution(resolution: Int) {
        if (mWidth > mHeight) {
            resultWidth = mWidth * resolution / mHeight
            resultHeight = resolution
        } else {
            resultWidth = resolution
            resultHeight = mHeight * resolution / mWidth
        }
    }

    /**
     * Checks if the [MediaFormat] is a video format
     *
     * @param format [MediaFormat]
     * @return True if the [MediaFormat] is a video format
     */
    private fun isVideoFormat(format: MediaFormat): Boolean =
        getMimeTypeFor(format)?.startsWith("video/") ?: false

    /**
     * Checks if the [MediaFormat] is an audio format
     *
     * @param format [MediaFormat]
     * @return True if the [MediaFormat] is an audio format
     */
    private fun isAudioFormat(format: MediaFormat): Boolean =
        getMimeTypeFor(format)?.startsWith("audio/") ?: false

    /**
     * Retrieves the mime type of a [MediaFormat]
     *
     * @param format [MediaFormat]
     * @return A [String] that describes the mime type
     */
    private fun getMimeTypeFor(format: MediaFormat): String? =
        format.getString(MediaFormat.KEY_MIME)

    /**
     * Selects a [MediaCodecInfo] from a given mime type
     *
     * @param mimeType The mime type
     * @return [MediaCodecInfo]
     */
    @Suppress("DEPRECATION")
    private fun selectCodec(mimeType: String): MediaCodecInfo? {
        val numCodecs = MediaCodecList.getCodecCount()
        for (i in 0 until numCodecs) {
            val codecInfo = MediaCodecList.getCodecInfoAt(i)
            if (!codecInfo.isEncoder) {
                continue
            }
            val types = codecInfo.supportedTypes
            for (j in types.indices) {
                if (types[j].equals(mimeType, ignoreCase = true)) {
                    return codecInfo
                }
            }
        }
        return null
    }
}
