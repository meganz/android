package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import javax.inject.Inject

/**
 * Mapper that converts a [HeartbeatStatus] into an [Integer]
 */
internal class HeartbeatStatusIntMapper @Inject constructor() {
    operator fun invoke(heartbeatStatus: HeartbeatStatus) = when (heartbeatStatus) {
        HeartbeatStatus.UP_TO_DATE -> 100
        HeartbeatStatus.INACTIVE -> -1
        else -> 0
    }
}
