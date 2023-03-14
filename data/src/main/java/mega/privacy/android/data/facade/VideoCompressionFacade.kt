package mega.privacy.android.data.facade

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.view.Surface
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import mega.privacy.android.data.compression.video.InputSurface
import mega.privacy.android.data.compression.video.OutputSurface
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.VideoCompressorGateway
import mega.privacy.android.domain.entity.VideoAttachment
import mega.privacy.android.domain.entity.VideoCompressionState
import mega.privacy.android.domain.entity.VideoQuality
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Implementation of [VideoCompressorGateway]
 */
internal class VideoCompressionFacade @Inject constructor(private val fileGateway: FileGateway) :
    VideoCompressorGateway {

    private val config = VideoCompressionConfig()

    private companion object {
        const val TIMEOUT_USEC = 10000
        const val OUTPUT_VIDEO_MIME_TYPE = "video/avc"
        const val OUTPUT_VIDEO_FRAME_RATE = 30
        const val OUTPUT_VIDEO_IFRAME_INTERVAL = 10
        const val OUTPUT_VIDEO_COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        const val OUTPUT_AUDIO_MIME_TYPE = "audio/mp4a-latm"
        const val OUTPUT_AUDIO_BIT_RATE = 128 * 1024
        const val OUTPUT_AUDIO_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectHE
    }

    override fun setVideoQuality(videoQuality: VideoQuality) {
        config.videoQuality = videoQuality
    }

    override fun setOutputRoot(root: String) {
        config.outputRoot = root
    }

    override fun isRunning() = config.isRunning

    override fun stop() {
        config.isRunning = false
    }

    override fun start() = callbackFlow {
        try {
            with(config) {
                while (queue.isNotEmpty() && isRunning()) {
                    ensureActive()
                    val attachment = queue.poll()
                    currentFileIndex += 1
                    totalSizeProcessed += attachment?.originalPath.takeIf {
                        it.isNullOrEmpty().not()
                    }?.let {
                        File(it).length()
                    } ?: 0
                    runCatching {
                        attachment?.let {
                            outputRoot?.run {
                                if (!fileGateway.hasEnoughStorage(
                                        rootPath = this,
                                        File(it.originalPath)
                                    )
                                ) {
                                    send(VideoCompressionState.InsufficientStorage)
                                    return@let
                                }
                            } ?: run {
                                send(VideoCompressionState.InsufficientStorage)
                                return@let
                            }
                            prepareAndChangeResolution(attachment) { progress ->
                                trySend(
                                    VideoCompressionState.Progress(
                                        progress,
                                        currentFileIndex,
                                        config.total,
                                        attachment.newPath
                                    )
                                )
                            }
                            send(
                                VideoCompressionState.FinishedCompression(
                                    attachment.newPath,
                                    true,
                                    attachment.pendingMessageId,
                                )
                            )
                        }
                    }.onSuccess {
                        send(VideoCompressionState.Successful(attachment?.id))
                    }.onFailure {
                        Timber.d("Video Compression Failed $it")
                        send(VideoCompressionState.Failed(attachment?.id))
                    }
                }
            }
        } catch (exception: Exception) {
            Timber.d("Video Compression Failed $exception")
            send(VideoCompressionState.Failed())
        }
        send(VideoCompressionState.Finished)
        awaitClose { config.isRunning = false }
    }.cancellable()

    override fun addItems(videoAttachments: List<VideoAttachment>) {
        config.queue.addAll(videoAttachments)
        config.total += videoAttachments.size
    }

    /**
     * Prepare Encoder and Decoders and Change the resolution,bitrate  and mux Audio and Video
     * @param videoAttachment [VideoAttachment]
     * @param block a callback to return video compression progress
     */
    private suspend fun prepareAndChangeResolution(
        videoAttachment: VideoAttachment,
        block: (Int) -> Unit,
    ) = suspendCancellableCoroutine {
        Timber.d("prepareAndChangeResolution")
        var exception: Exception? = null
        val inputFile = videoAttachment.originalPath
        val outputFile = videoAttachment.newPath
        val videoCodecInfo = selectCodec(OUTPUT_VIDEO_MIME_TYPE)
            ?: return@suspendCancellableCoroutine
        val audioCodecInfo = selectCodec(OUTPUT_AUDIO_MIME_TYPE)
            ?: return@suspendCancellableCoroutine
        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var outputSurface: OutputSurface? = null
        var videoDecoder: MediaCodec? = null
        var audioDecoder: MediaCodec? = null
        var videoEncoder: MediaCodec? = null
        var audioEncoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: InputSurface? = null

        // internal function for releasing encoders and decoders
        fun dispose() {
            Timber.d("Release Encoders and Decoders")
            try {
                videoExtractor?.release()
                audioExtractor?.release()
                videoEncoder?.stop()
                videoEncoder?.release()
                videoDecoder?.stop()
                videoDecoder?.release()
                audioEncoder?.stop()
                audioEncoder?.release()
                audioDecoder?.stop()
                audioDecoder?.release()
                muxer?.stop()
                muxer?.release()
                inputSurface?.release()
                outputSurface?.release()
            } catch (e: Exception) {
                Timber.e("Release Exception $e")
            }
        }
        try {
            videoExtractor = createExtractor(inputFile)
            val videoInputTrack = getAndSelectVideoTrackIndex(videoExtractor)
            val inputFormat = videoExtractor.getTrackFormat(videoInputTrack)
            val metadataRetriever = MediaMetadataRetriever()
            metadataRetriever.setDataSource(inputFile)
            getOriginalWidthAndHeight(metadataRetriever)
            val bitrate = getBitrate(
                (metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    ?: return@suspendCancellableCoroutine)
                    .toInt(), config.videoQuality
            )
            val frameRate =
                if (inputFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) inputFormat.getInteger(
                    MediaFormat.KEY_FRAME_RATE
                ) else OUTPUT_VIDEO_FRAME_RATE
            val duration =
                (metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?: return@suspendCancellableCoroutine).toLong()
            videoAttachment.totalDuration = duration

            Timber.d("Video result width: ${config.resultWidth}, result height: ${config.resultHeight}, encode bitrate: $bitrate, encode frame rate: $frameRate")

            val outputVideoFormat =
                MediaFormat.createVideoFormat(
                    OUTPUT_VIDEO_MIME_TYPE,
                    config.resultWidth,
                    config.resultHeight,
                ).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, OUTPUT_VIDEO_COLOR_FORMAT)
                    setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                    setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                    setInteger(
                        MediaFormat.KEY_I_FRAME_INTERVAL,
                        OUTPUT_VIDEO_IFRAME_INTERVAL
                    )
                }

            val inputSurfaceReference = AtomicReference<Surface>()
            videoEncoder =
                createVideoEncoder(videoCodecInfo, outputVideoFormat, inputSurfaceReference)
            inputSurface = InputSurface(
                inputSurfaceReference.get()
            )
            inputSurface.makeCurrent()
            outputSurface = OutputSurface()
            videoDecoder = createVideoDecoder(inputFormat, outputSurface.surface)
            audioExtractor = createExtractor(inputFile)
            val audioInputTrack = getAndSelectAudioTrackIndex(audioExtractor).takeIf { it >= 0 }
                ?: throw RuntimeException("Audio information not found")
            val inputAudioFormat = audioExtractor.getTrackFormat(audioInputTrack)
            val outputAudioFormat = MediaFormat.createAudioFormat(
                inputAudioFormat.getString(MediaFormat.KEY_MIME)
                    ?: return@suspendCancellableCoroutine,
                inputAudioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                inputAudioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            )
            outputAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_AUDIO_BIT_RATE)
            outputAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, OUTPUT_AUDIO_AAC_PROFILE)
            audioEncoder = createAudioEncoder(audioCodecInfo, outputAudioFormat)
            audioDecoder = createAudioDecoder(inputAudioFormat)
            muxer = MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
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
                video = videoAttachment,
            ) { progress ->
                Timber.d("Current Video Compression Progress $it")
                block(progress)
            }
        } catch (e: Exception) {
            exception = e
            Timber.d("Change Resolution Exception $e")
        } finally {
            dispose()
        }
        if (exception != null) {
            Timber.e(exception, "Exception. Video not compressed, uploading original video.")
            it.resumeWith(Result.failure(exception))
        } else {
            it.resumeWith(Result.success(Unit))
        }
        it.invokeOnCancellation {
            dispose()
        }
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
     * @param [video] [VideoAttachment]
     */
    private fun changeResolution(
        videoExtractor: MediaExtractor?, audioExtractor: MediaExtractor?,
        videoDecoder: MediaCodec?, videoEncoder: MediaCodec?,
        audioDecoder: MediaCodec?, audioEncoder: MediaCodec?,
        muxer: MediaMuxer,
        inputSurface: InputSurface,
        outputSurface: OutputSurface,
        video: VideoAttachment,
        block: (Int) -> Unit,
    ) {
        Timber.d("change Resolution")
        val videoDecoderOutputBufferInfo = MediaCodec.BufferInfo()
        val videoEncoderOutputBufferInfo = MediaCodec.BufferInfo()

        val audioDecoderOutputBufferInfo = MediaCodec.BufferInfo()
        val audioEncoderOutputBufferInfo = MediaCodec.BufferInfo()

        var decoderOutputVideoFormat: MediaFormat?
        var decoderOutputAudioFormat: MediaFormat?
        var encoderOutputVideoFormat: MediaFormat? = null
        var encoderOutputAudioFormat: MediaFormat? = null

        var outputVideoTrack = -1
        var outputAudioTrack = -1

        var videoExtractorDone = false
        var videoDecoderDone = false
        var videoEncoderDone = false

        var audioExtractorDone = false
        var audioDecoderDone = false
        var audioEncoderDone = false

        var pendingAudioDecoderOutputBufferIndex = -1
        var muxing = false
        while ((!videoEncoderDone || !audioEncoderDone) && isRunning()) {
            while (!videoExtractorDone && (encoderOutputVideoFormat == null || muxing)) {
                val decoderInputBufferIndex =
                    videoDecoder?.dequeueInputBuffer(TIMEOUT_USEC.toLong()) ?: return
                if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
                val decoderInputBuffer = videoDecoder.getInputBuffer(decoderInputBufferIndex)
                val size =
                    (videoExtractor ?: return).readSampleData(decoderInputBuffer ?: return, 0)
                val presentationTime = videoExtractor.sampleTime
                if (size >= 0) {
                    videoDecoder.queueInputBuffer(
                        decoderInputBufferIndex,
                        0,
                        size,
                        presentationTime,
                        videoExtractor.sampleFlags
                    )
                }
                videoExtractorDone = (!videoExtractor.advance() && size == -1)
                if (videoExtractorDone) {
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
            while (audioDecoder != null && !audioExtractorDone && (encoderOutputAudioFormat == null || muxing)) {
                val decoderInputBufferIndex = audioDecoder.dequeueInputBuffer(TIMEOUT_USEC.toLong())
                if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
                val decoderInputBuffer = audioDecoder.getInputBuffer(decoderInputBufferIndex)
                val size =
                    (audioExtractor ?: return).readSampleData(decoderInputBuffer ?: return, 0)
                val presentationTime = audioExtractor.sampleTime
                if (size >= 0) audioDecoder.queueInputBuffer(
                    decoderInputBufferIndex,
                    0,
                    size,
                    presentationTime,
                    audioExtractor.sampleFlags
                )
                audioExtractorDone = (!audioExtractor.advance() && size == -1)
                if (audioExtractorDone) {
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
                val decoderOutputBufferIndex = videoDecoder?.dequeueOutputBuffer(
                    videoDecoderOutputBufferInfo,
                    TIMEOUT_USEC.toLong()
                ) ?: return
                if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
                if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    decoderOutputVideoFormat = videoDecoder.outputFormat
                    Timber.d("Current Video Format $decoderOutputVideoFormat")
                    break
                }
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
                    val progress =
                        ((videoDecoderOutputBufferInfo.presentationTimeUs.toFloat() / video.totalDuration.toFloat()) / 10).toInt()
                    video.currentDuration = videoDecoderOutputBufferInfo.presentationTimeUs
                    block(progress)
                }
                if (videoDecoderOutputBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    videoDecoderDone = true
                    videoEncoder?.signalEndOfInputStream()
                }
                break
            }
            while (audioDecoder != null && !audioDecoderDone && (encoderOutputAudioFormat == null || muxing)) {
                val decoderOutputBufferIndex = audioDecoder.dequeueOutputBuffer(
                    audioDecoderOutputBufferInfo, TIMEOUT_USEC.toLong()
                )
                if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
                if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    decoderOutputAudioFormat = audioDecoder.outputFormat
                    Timber.d("Current Audio Format $decoderOutputAudioFormat")
                    break
                }
                if (audioDecoderOutputBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    audioDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false)
                    break
                }
                pendingAudioDecoderOutputBufferIndex = decoderOutputBufferIndex
                break
            }
            while (audioEncoder != null && pendingAudioDecoderOutputBufferIndex != -1) {
                val encoderInputBufferIndex = audioEncoder.dequeueInputBuffer(TIMEOUT_USEC.toLong())
                val encoderInputBuffer = audioEncoder.getInputBuffer(encoderInputBufferIndex)
                val size = audioDecoderOutputBufferInfo.size
                val presentationTime = audioDecoderOutputBufferInfo.presentationTimeUs
                if (size >= 0) {
                    val decoderOutputBuffer =
                        audioDecoder?.getOutputBuffer(pendingAudioDecoderOutputBufferIndex)
                            ?.duplicate()
                    decoderOutputBuffer?.let {
                        it.position(audioDecoderOutputBufferInfo.offset)
                        it.limit(audioDecoderOutputBufferInfo.offset + size)
                        encoderInputBuffer?.position(0)
                        encoderInputBuffer?.put(it)
                    }
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
                if (audioDecoderOutputBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    audioDecoderDone = true
                }
                break
            }
            while (!videoEncoderDone && (encoderOutputVideoFormat == null || muxing)) {
                val encoderOutputBufferIndex = videoEncoder?.dequeueOutputBuffer(
                    videoEncoderOutputBufferInfo,
                    TIMEOUT_USEC.toLong()
                ) ?: return
                if (encoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
                if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    encoderOutputVideoFormat = videoEncoder.outputFormat
                    break
                }
                val encoderOutputBuffer = videoEncoder.getOutputBuffer(encoderOutputBufferIndex)
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
            while (audioEncoder != null && !audioEncoderDone && (encoderOutputAudioFormat == null || muxing)) {
                val encoderOutputBufferIndex = audioEncoder.dequeueOutputBuffer(
                    audioEncoderOutputBufferInfo, TIMEOUT_USEC.toLong()
                )
                if (encoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                }
                if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    encoderOutputAudioFormat = audioEncoder.outputFormat
                    break
                }
                val encoderOutputBuffer = audioEncoder.getOutputBuffer(encoderOutputBufferIndex)
                if (audioEncoderOutputBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    audioEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false)
                    break
                }
                if (audioEncoderOutputBufferInfo.size != 0) {
                    muxer.writeSampleData(
                        outputAudioTrack,
                        encoderOutputBuffer ?: return,
                        audioEncoderOutputBufferInfo
                    )
                }
                if (audioEncoderOutputBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    audioEncoderDone =
                        true
                }
                audioEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false)
                break
            }
            if (!muxing && encoderOutputVideoFormat != null && encoderOutputAudioFormat != null) {
                outputVideoTrack = muxer.addTrack(encoderOutputVideoFormat)
                outputAudioTrack = muxer.addTrack(encoderOutputAudioFormat)
                muxer.start()
                muxing = true
            }
        }
        // send video progress 100% event
        block(100)
    }


    private fun getBitrate(
        bitrate: Int,
        quality: VideoQuality,
    ): Int {
        return when (quality) {
            VideoQuality.LOW -> (bitrate * 0.2).roundToInt()
            VideoQuality.MEDIUM -> (bitrate * 0.3).roundToInt()
            VideoQuality.HIGH -> (bitrate * 0.4).roundToInt()
            VideoQuality.ORIGINAL -> bitrate
        }
    }


    /**
     * Retrieves the original width and height of the video
     *
     * @param metadataRetriever The video metadata
     */
    private fun getOriginalWidthAndHeight(metadataRetriever: MediaMetadataRetriever) {
        with(config) {
            try {
                width =
                    (metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                        ?: return).toInt()
                height =
                    (metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                        ?: return)
                        .toInt()
            } catch (e: Exception) {
                Timber.e("Metadata Retrieval Exception: $e")
            }
            val (newWidth, newHeight) = generateWidthAndHeight(width, height)
            config.resultWidth = newWidth
            config.resultHeight = newHeight
        }

    }

    /**
     * Generate new width and height for source file
     * @param width file's original width
     * @param height file's original height
     * @return new width and height pair
     */
    private fun generateWidthAndHeight(
        width: Int,
        height: Int,
    ): Pair<Int, Int> {
        val newWidth: Int
        val newHeight: Int
        when {
            width >= 1920 || height >= 1920 -> {
                newWidth = generateWidthHeightValue(width, 0.5)
                newHeight = generateWidthHeightValue(height, 0.5)
            }
            width >= 1280 || height >= 1280 -> {
                newWidth = generateWidthHeightValue(width, 0.75)
                newHeight = generateWidthHeightValue(height, 0.75)
            }
            width >= 960 || height >= 960 -> {
                newWidth = generateWidthHeightValue(width, 0.95)
                newHeight = generateWidthHeightValue(height, 0.95)
            }
            else -> {
                newWidth = generateWidthHeightValue(width, 0.9)
                newHeight = generateWidthHeightValue(height, 0.9)
            }
        }
        return Pair(newWidth, newHeight)
    }

    private fun roundEven(value: Int): Int = value + 1 and 1.inv()

    private fun generateWidthHeightValue(value: Int, factor: Double): Int =
        roundEven((((value * factor) / 16).roundToInt() * 16))

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
    private fun selectCodec(mimeType: String): MediaCodecInfo? {
        val codecsInfo = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        codecsInfo.filter { it.isEncoder }.forEach { codecInfo ->
            val types = codecInfo.supportedTypes
            for (j in types.indices) {
                if (types[j].equals(mimeType, ignoreCase = true)) {
                    return codecInfo
                }
            }
        }
        return null
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
     * Current Configuration for VideCompression
     *
     * @param isRunning [Boolean] video compression is running or not
     * @param videoQuality [VideoQuality] Video Quality based on the User Settings
     * @param width [Int] Original width of the video
     * @param height [Int] Original height of the video
     * @param resultWidth [Int] Calculated new width of the video
     * @param resultHeight [Int] Calculated new height of the video
     * @param outputRoot [String] root path to check whether enough disk space is available or not
     * @param currentFileIndex [Int] current video index which is being compressed
     * @param totalSizeProcessed [Long] total size processed for the current video
     * @param total [Int] total video count
     * @param queue [ConcurrentLinkedQueue] of [VideoAttachment] a queue to hold the videos to be processed one by one
     */
    inner class VideoCompressionConfig(
        var isRunning: Boolean = true,
        var videoQuality: VideoQuality = VideoQuality.ORIGINAL,
        var width: Int = 0,
        var height: Int = 0,
        var resultWidth: Int = 0,
        var resultHeight: Int = 0,
        var outputRoot: String? = null,
        var currentFileIndex: Int = 0,
        var totalSizeProcessed: Long = 0,
        var total: Int = 0,
        val queue: ConcurrentLinkedQueue<VideoAttachment> = ConcurrentLinkedQueue(),
    )
}
