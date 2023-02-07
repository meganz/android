package mega.privacy.android.data.facade

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.os.StatFs
import android.view.Surface
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.cancellable
import mega.privacy.android.data.compression.video.InputSurface
import mega.privacy.android.data.compression.video.OutputSurface
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

/**
 * Implementation of [VideoCompressorGateway]
 */
internal class VideoCompressionFacade @Inject constructor() : VideoCompressorGateway {

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
        const val SHORT_SIDE_SIZE_MEDIUM = 1080
        const val SHORT_SIDE_SIZE_LOW = 720
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
                            if (hasEnoughStorage(it.originalPath)) {
                                send(VideoCompressionState.InsufficientStorage)
                                return@let
                            }
                            prepareAndChangeResolution(attachment) { progress ->
                                trySend(
                                    VideoCompressionState.Progress(
                                        progress,
                                        currentFileIndex,
                                        totalSizeProcessed,
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
                        send(VideoCompressionState.Failed(attachment?.id))
                    }
                }
            }
        } catch (exception: Exception) {
            send(VideoCompressionState.Failed())
        }
        send(VideoCompressionState.Finished)
        awaitClose { config.isRunning = false }
    }.cancellable()


    private fun hasEnoughStorage(path: String): Boolean {
        val size = File(path).length()
        val availableFreeSpace = try {
            StatFs(config.outputRoot).availableBytes.toDouble()
        } catch (exception: Exception) {
            Timber.e("Exception When Retrieving Free Space: $exception")
            Double.MAX_VALUE
        }
        return size > availableFreeSpace
    }

    override fun addItems(videoAttachments: List<VideoAttachment>) {
        config.queue.addAll(videoAttachments)
    }

    private suspend fun prepareAndChangeResolution(
        videoAttachment: VideoAttachment,
        block: suspend (Int) -> Unit,
    ) {
        Timber.d("prepareAndChangeResolution")
        var exception: Exception? = null
        val inputFile = videoAttachment.originalPath
        val outputFile = videoAttachment.newPath
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
            videoExtractor = createExtractor(outputFile)
            val videoInputTrack = getAndSelectVideoTrackIndex(videoExtractor)
            val inputFormat = videoExtractor.getTrackFormat(videoInputTrack)
            val metadataRetriever = MediaMetadataRetriever()
            metadataRetriever.setDataSource(inputFile)
            getOriginalWidthAndHeight(metadataRetriever)
            config.resultWidth = config.width
            config.resultHeight = config.height
            var bitrate =
                (metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    ?: return)
                    .toInt()
            var shortSideByQuality =
                if (config.videoQuality == VideoQuality.MEDIUM) SHORT_SIDE_SIZE_MEDIUM else SHORT_SIDE_SIZE_LOW
            val shortSide = config.width.coerceAtMost(config.height)
            var frameRate =
                if (inputFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) inputFormat.getInteger(
                    MediaFormat.KEY_FRAME_RATE
                ) else OUTPUT_VIDEO_FRAME_RATE
            Timber.d("Video original width: ${config.width}, original height: ${config.height}, average bitrate: $bitrate, frame rate: $frameRate")
            if (config.videoQuality == VideoQuality.HIGH) {
                // Since the METADATA_KEY_BITRATE is not the right value of the final bitrate
                // of a video but the average one, we can assume a 2% less to ensure the final size
                // is a bit less than the original one.
                bitrate *= 0.98.toInt()
            } else {
                bitrate =
                    if (config.videoQuality == VideoQuality.MEDIUM) bitrate / 2 else bitrate / 3
                frameRate = frameRate.coerceAtMost(OUTPUT_VIDEO_FRAME_RATE)
                if (shortSide > shortSideByQuality) {
                    configureCodecResolution(shortSideByQuality)
                }
            }
            Timber.d("Video result width: ${config.resultWidth}, result height: ${config.resultHeight}, encode bitrate: $bitrate, encode frame rate: $frameRate")
            val capabilities = videoCodecInfo
                .getCapabilitiesForType(OUTPUT_VIDEO_MIME_TYPE).videoCapabilities
            var supported = capabilities.areSizeAndRateSupported(
                config.resultWidth,
                config.resultHeight,
                frameRate.toDouble()
            )

            //local function
            fun checkSupported(index: Int): Boolean {
                configureCodecResolution(index)
                supported = capabilities.areSizeAndRateSupported(
                    config.resultWidth,
                    config.resultHeight,
                    frameRate.toDouble()
                )
                return supported
            }
            if (!supported) {
                Timber.w("Sizes width: ${config.resultWidth} height: ${config.resultHeight} not supported.")
                for (i in shortSideByQuality until shortSide) {
                    supported = checkSupported(i)
                    if (supported) {
                        break
                    }
                }
            }
            if (!supported && config.videoQuality == VideoQuality.MEDIUM) {
                Timber.w("Sizes still not supported. Second try.")
                shortSideByQuality--
                for (i in shortSideByQuality downTo SHORT_SIDE_SIZE_LOW + 1) {
                    supported = checkSupported(i)
                    if (supported) {
                        break
                    }
                }
            }
            if (!supported) {
                val error =
                    "Latest sizes width: ${config.resultWidth} height: ${config.resultHeight} not supported Video not compressed, uploading original video."
                Timber.e(error)
                throw Exception(error).also { exception = it }
            }
            val outputVideoFormat =
                MediaFormat.createVideoFormat(
                    OUTPUT_VIDEO_MIME_TYPE,
                    config.resultWidth,
                    config.resultHeight,
                )
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
            inputSurface = InputSurface(
                inputSurfaceReference.get()
            )
            inputSurface.makeCurrent()
            outputSurface = OutputSurface()
            videoDecoder = createVideoDecoder(inputFormat, outputSurface.surface)
            audioExtractor = createExtractor(inputFile)
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
            ) {
                block(it)
            }
        } finally {
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
                if (exception == null) exception = e
            }
        }
        if (exception != null) {
            Timber.e(exception, "Exception. Video not compressed, uploading original video.")
            throw exception as Exception
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
    private suspend fun changeResolution(
        videoExtractor: MediaExtractor?, audioExtractor: MediaExtractor?,
        videoDecoder: MediaCodec?, videoEncoder: MediaCodec?,
        audioDecoder: MediaCodec?, audioEncoder: MediaCodec?,
        muxer: MediaMuxer,
        inputSurface: InputSurface,
        outputSurface: OutputSurface,
        video: VideoAttachment,
        block: suspend (Int) -> Unit,
    ) {
        Timber.d("changeResolution")
        val videoDecoderOutputBufferInfo: MediaCodec.BufferInfo?
        val videoEncoderOutputBufferInfo: MediaCodec.BufferInfo?
        videoDecoderOutputBufferInfo = MediaCodec.BufferInfo()
        videoEncoderOutputBufferInfo = MediaCodec.BufferInfo()
        var encoderOutputVideoFormat: MediaFormat? = null
        var outputVideoTrack = -1
        var videoExtractorDone = false
        var videoDecoderDone = false
        var videoEncoderDone = false
        var audioDecoderOutputBufferInfo: MediaCodec.BufferInfo? = null
        var audioEncoderOutputBufferInfo: MediaCodec.BufferInfo? = null
        var audioExtractorDone = false
        var audioDecoderDone = false
        var audioEncoderDone = false
        if (audioDecoder != null && audioEncoder != null) {
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
        while ((!videoEncoderDone || !audioEncoderDone) && isRunning()) {
            while (!videoExtractorDone && (encoderOutputVideoFormat == null || muxing)) {
                var decoderInputBufferIndex =
                    videoDecoder?.dequeueInputBuffer(TIMEOUT_USEC.toLong()) ?: return
                if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
                val decoderInputBuffer = videoDecoder.getInputBuffer(decoderInputBufferIndex)
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
            while (audioDecoder != null && !audioExtractorDone && (encoderOutputAudioFormat == null || muxing)) {
                var decoderInputBufferIndex = audioDecoder.dequeueInputBuffer(TIMEOUT_USEC.toLong())
                if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
                val decoderInputBuffer = audioDecoder.getInputBuffer(decoderInputBufferIndex)
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
                val decoderOutputBufferIndex = videoDecoder?.dequeueOutputBuffer(
                    videoDecoderOutputBufferInfo,
                    TIMEOUT_USEC.toLong()
                ) ?: return
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
                    videoEncoder?.signalEndOfInputStream()
                }
                break
            }
            while (audioDecoder != null && !audioDecoderDone && (encoderOutputAudioFormat == null || muxing)) {
                val decoderOutputBufferIndex = audioDecoder.dequeueOutputBuffer(
                    audioDecoderOutputBufferInfo ?: return, TIMEOUT_USEC.toLong()
                )
                if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
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
                val size = (audioDecoderOutputBufferInfo ?: return).size
                val presentationTime = audioDecoderOutputBufferInfo.presentationTimeUs
                if (size >= 0) {
                    val decoderOutputBuffer =
                        audioEncoder.getOutputBuffer(pendingAudioDecoderOutputBufferIndex)
                    decoderOutputBuffer?.let {
                        decoderOutputBuffer.position(audioDecoderOutputBufferInfo.offset)
                        decoderOutputBuffer.limit(audioDecoderOutputBufferInfo.offset + size)
                        encoderInputBuffer?.position(0)
                        encoderInputBuffer?.put(decoderOutputBuffer)
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
                if (audioDecoderOutputBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) audioDecoderDone =
                    true
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
                    audioEncoderOutputBufferInfo ?: return, TIMEOUT_USEC.toLong()
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
            block(video.compressionPercentage)
        }
        video.compressionPercentage = 100
    }


    /**
     * Retrieves the original width and height of the video
     *
     * @param metadataRetriever The video metadata
     */
    private fun getOriginalWidthAndHeight(metadataRetriever: MediaMetadataRetriever) {
        config.width =
            (metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?: return).toInt()
        config.height =
            (metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?: return)
                .toInt()
        val thumbnail = metadataRetriever.frameAtTime
        val inputWidth = (thumbnail ?: return).width
        val inputHeight = thumbnail.height
        if (inputWidth > inputHeight) {
            if (config.width < config.height) {
                val w = config.width
                config.width = config.height
                config.height = w
            }
        } else {
            if (config.width > config.height) {
                val w = config.width
                config.width = config.height
                config.height = w
            }
        }
    }

    /**
     * configure resolution to compress the video.
     *
     * @param resolution Short side size.
     */
    private fun configureCodecResolution(resolution: Int) {
        with(config) {
            if (width > height) {
                resultWidth = width * resolution / height
                resultHeight = resolution
            } else {
                resultWidth = resolution
                resultHeight = height * resolution / width
            }
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
        val queue: ConcurrentLinkedQueue<VideoAttachment> = ConcurrentLinkedQueue(),
    )
}
