package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.VideoQuality
import javax.inject.Inject

/**
 * VideoQualityMapper
 */

internal class VideoQualityMapper @Inject constructor() {
    /**
     * To video quality
     *
     * @param input
     */
    operator fun invoke(input: Int?) = when (input) {
        0 -> VideoQuality.LOW
        1 -> VideoQuality.MEDIUM
        2 -> VideoQuality.HIGH
        3 -> VideoQuality.ORIGINAL
        else -> null
    }
}
