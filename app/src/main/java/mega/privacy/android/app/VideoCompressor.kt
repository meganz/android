package mega.privacy.android.app

import android.content.Context
import android.os.StatFs
import mega.privacy.android.app.utils.conversion.VideoCompressionCallback
import mega.privacy.android.domain.entity.SyncRecord
import timber.log.Timber
import java.io.File

/**
 * The class to compress videos
 *
 * @property context The [Context] object
 * @property callback The [VideoCompressionCallback]
 * @property videoQuality The video quality to compress to
 */
class VideoCompressor(
    context: Context,
    private val callback: VideoCompressionCallback,
    videoQuality: Int,
) : VideoDownSampling(
    context = context,
    videoQuality = videoQuality,
) {
    private var pendingList = listOf<SyncRecord>()
    private var outputRoot: String? = null
    private var totalRead = 0L

    /**
     * Total input size
     */
    var totalInputSize = 0L

    /**
     * Overall video count
     */
    var totalCount = 0

    /**
     * Index of current file
     */
    var currentFileIndex = 0

    /**
     * Stops the video compression
     */
    fun stop() {
        setVideoDownSamplingRunning(false)
        Timber.d("Video compressor stopped")
    }

    /**
     * Sets the list of videos to be compressed
     *
     * @param pendingList A list of [SyncRecord]
     */
    fun setPendingList(pendingList: List<SyncRecord>) {
        this.pendingList = pendingList
        totalCount = pendingList.size
        Timber.d("Total compression videos count is %s", totalCount)
        calculateTotalSize()
    }

    /**
     * Calculates the total size of all videos to be compressed
     */
    private fun calculateTotalSize() {
        for (i in 0 until totalCount) {
            pendingList[i].localPath?.let { nonNullLocalPath ->
                totalInputSize += File(nonNullLocalPath).length()
            }
        }
        Timber.d("Total compression size is %s", totalInputSize)
    }

    /**
     * Sets the Output Root [String]
     *
     * @param root The Root [String], which can be nullable
     */
    fun setOutputRoot(root: String?) {
        outputRoot = root
    }

    /**
     * Checks whether there is enough space to compress the videos
     *
     * @return Whether there is enough space to compress the videos or not
     */
    private fun notEnoughSpace(size: Long): Boolean {
        val availableFreeSpace = try {
            StatFs(outputRoot).availableBytes.toDouble()
        } catch (exception: Exception) {
            Timber.e("Exception When Retrieving Free Space: $exception")
            Double.MAX_VALUE
        }
        return size > availableFreeSpace
    }

    /**
     * Starts the video compression
     */
    fun start() {
        setVideoDownSamplingRunning(true)
        var i = 0
        while (i < totalCount && isVideoDownSamplingRunning()) {
            currentFileIndex = i + 1
            val record = pendingList[i]
            Timber.d("Video compressor start: %s", record.toString())
            val path = record.localPath

            path?.let { nonNullPath ->
                val src = File(nonNullPath)
                val size = src.length()
                if (notEnoughSpace(size)) {
                    callback.onInsufficientSpace()
                    return
                }

                val newPath = record.newPath
                if (newPath == null) {
                    callback.onCompressFailed(record)
                    return
                }

                val video = VideoUpload(
                    originalPath = path,
                    newPath = newPath,
                    size = size,
                    pendingMessageId = -1,
                )
                try {
                    prepareAndChangeResolution(video)
                    if (isVideoDownSamplingRunning()) {
                        callback.onCompressSuccessful(record)
                    }
                } catch (ex: Exception) {
                    Timber.e(ex)
                    callback.onCompressFailed(record)
                    currentFileIndex++
                    totalRead += size
                }
            }
            i++
        }
        callback.onCompressFinished("$totalCount/$totalCount")
        stop()
    }
}
