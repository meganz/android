package mega.privacy.android.app.presentation.settings.camerauploads.mapper

import mega.privacy.android.app.presentation.settings.camerauploads.model.VideoQualityUiItem
import mega.privacy.android.domain.entity.VideoQuality
import javax.inject.Inject

/**
 * UI Mapper class that retrieves the appropriate [VideoQualityUiItem] from a given [VideoQuality]
 */
internal class VideoQualityUiItemMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param videoQuality The [VideoQuality]
     * @return the specific [VideoQualityUiItem]
     */
    operator fun invoke(videoQuality: VideoQuality) = when (videoQuality) {
        VideoQuality.LOW -> VideoQualityUiItem.Low
        VideoQuality.MEDIUM -> VideoQualityUiItem.Medium
        VideoQuality.HIGH -> VideoQualityUiItem.High
        VideoQuality.ORIGINAL -> VideoQualityUiItem.Original
    }
}