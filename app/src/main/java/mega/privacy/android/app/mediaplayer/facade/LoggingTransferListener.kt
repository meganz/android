package mega.privacy.android.app.mediaplayer.facade

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.upstream.BandwidthMeter
import timber.log.Timber

/**
 * Logs bandwidth statistics for the upstream media data source.
 *
 * The data spec URI is never logged: for public and foreign nodes the local streaming link
 * carries the node decryption key and the file name.
 */
@OptIn(UnstableApi::class)
internal class LoggingTransferListener(
    private val bandwidthMeter: BandwidthMeter,
) : TransferListener {
    private var totalBytesTransferred = 0L
    private var transferStartTime = 0L
    private var lastLogTime = 0L
    private var lastTotalBytesTransferred = 0L

    override fun onTransferInitializing(
        source: DataSource,
        dataSpec: DataSpec,
        isNetwork: Boolean,
    ) {
        Timber.d("[Cache] MISS: position=${dataSpec.position}, length=${dataSpec.length}")
    }

    override fun onTransferStart(
        source: DataSource,
        dataSpec: DataSpec,
        isNetwork: Boolean,
    ) {
        transferStartTime = System.currentTimeMillis()
        lastLogTime = transferStartTime
        lastTotalBytesTransferred = 0L
        Timber.d(
            "TransferListener started: isNetwork=$isNetwork, " +
                    "position=${dataSpec.position}, length=${dataSpec.length}"
        )
    }

    override fun onBytesTransferred(
        source: DataSource,
        dataSpec: DataSpec,
        isNetwork: Boolean,
        bytesTransferred: Int,
    ) {
        if (isNetwork && bytesTransferred > 0) {
            totalBytesTransferred += bytesTransferred
            val currentTime = System.currentTimeMillis()

            // Log only if 1 second has passed since last log
            if (currentTime - lastLogTime >= 1000) {
                val bitrate = bandwidthMeter.bitrateEstimate
                val duration = currentTime - transferStartTime

                // Calculate current bitrate for the last second
                val bytesInLastSecond = totalBytesTransferred - lastTotalBytesTransferred
                val currentBitrate = bytesInLastSecond * 8

                if (duration > 0) {
                    val averageBitrate = (totalBytesTransferred * 8 * 1000) / duration
                    Timber.d(
                        """
                            TransferListener Bandwidth:
                            Estimated: %.2f kbps
                            Current: %.2f kbps (last second)
                            Average: %.2f kbps (from start)
                            Total transferred: %.2f KB
                            Duration: %ds
                            Bytes in last second: %.2f KB
                        """.trimIndent(),
                        bitrate / 1024f,
                        currentBitrate / 1024f,
                        averageBitrate / 1024f,
                        totalBytesTransferred / 1024f,
                        duration / 1000,
                        bytesInLastSecond / 1024f
                    )
                } else {
                    Timber.d(
                        """
                            TransferListener Bandwidth:
                            Estimated: %.2f kbps
                            Current: %.2f kbps (last second)
                            Total transferred: %.2f KB
                            Duration: %ds
                            Bytes in last second: %.2f KB
                        """.trimIndent(),
                        bitrate / 1024f,
                        currentBitrate / 1024f,
                        totalBytesTransferred / 1024f,
                        duration / 1000,
                        bytesInLastSecond / 1024f
                    )
                }

                lastLogTime = currentTime
                lastTotalBytesTransferred = totalBytesTransferred
            }
        }
    }

    override fun onTransferEnd(
        source: DataSource,
        dataSpec: DataSpec,
        isNetwork: Boolean,
    ) {
        val currentTime = System.currentTimeMillis()
        val duration = currentTime - transferStartTime
        val bitrate = bandwidthMeter.bitrateEstimate

        if (duration > 0) {
            val averageBitrate = (totalBytesTransferred * 8 * 1000) / duration
            Timber.d(
                """
                    TransferListener Transfer ended:
                    Total: %.2f KB
                    Duration: %ds
                    Avg bitrate: %.2f kbps
                    Estimated: %.2f kbps
                """.trimIndent(),
                totalBytesTransferred / 1024f,
                duration / 1000,
                averageBitrate / 1024f,
                bitrate / 1024f
            )
        } else {
            Timber.d(
                """
                    TransferListener Transfer ended:
                    Total: %.2f KB
                    Estimated: %.2f kbps
                """.trimIndent(),
                totalBytesTransferred / 1024f,
                bitrate / 1024f
            )
        }

        totalBytesTransferred = 0L
        transferStartTime = 0L
        lastLogTime = 0L
        lastTotalBytesTransferred = 0L
    }
}
