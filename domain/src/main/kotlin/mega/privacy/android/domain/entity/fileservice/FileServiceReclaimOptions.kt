package mega.privacy.android.domain.entity.fileservice

import kotlin.time.Duration

/**
 * Domain entity representing file service reclaim options.
 *
 * @property reclaimAgeThreshold How old should the files be before they are considered for
 * reclamation? Has minute precision; non-zero values smaller than 1 minute are rounded up to 1.
 * @property reclaimDelay How long after startup should we wait until we reclaim space?
 * @property reclaimPeriod How long should we wait between consecutive reclaims?
 * @property reclaimSizeThreshold The reclaim trigger threshold in bytes. When the used space
 * exceeds this value (and other required conditions are satisfied), a reclaim operation may be
 * triggered. Reclaiming stops once usage falls below this threshold.
 * - `0`: No minimum threshold (reclaim may run immediately).
 * - `-1`: Automatic reclamation is disabled.
 * @property reclaimTarget The target size in bytes that the used space should be reduced to when
 * a reclaim operation is triggered.
 */
data class FileServiceReclaimOptions(
    val reclaimAgeThreshold: Duration,
    val reclaimDelay: Duration,
    val reclaimPeriod: Duration,
    val reclaimSizeThreshold: Long,
    val reclaimTarget: Long,
) {
    companion object {
        /**
         * No minimum threshold — reclaim may run immediately.
         */
        const val RECLAIM_SIZE_THRESHOLD_NONE = 0L

        /**
         * Automatic reclamation is disabled.
         */
        const val RECLAIM_SIZE_THRESHOLD_DISABLED = -1L
    }
}
