package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.VideoQuality
import javax.inject.Inject

/**
 * VideoQualityIntMapper
 */
internal class VideoQualityIntMapper @Inject constructor() {
    /**
     * Video quality to int
     *
     * @param quality
     */
    operator fun invoke(quality: VideoQuality) = when (quality) {
        VideoQuality.LOW -> 0
        VideoQuality.MEDIUM -> 1
        VideoQuality.HIGH -> 2
        VideoQuality.ORIGINAL -> 3
    }
}
