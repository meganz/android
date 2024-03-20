package mega.privacy.android.app.presentation.settings.camerauploads.model

import androidx.annotation.StringRes
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.VideoQuality

/**
 * An enumeration of different Video Quality Options to be used in the UI Layer
 *
 * @property videoQuality The domain [VideoQuality] that identifies the UI item
 * @property textRes The corresponding text represented as a [StringRes]
 */
internal enum class VideoQualityUiItem(
    val videoQuality: VideoQuality,
    @StringRes val textRes: Int,
) {
    Low(
        videoQuality = VideoQuality.LOW,
        textRes = R.string.settings_camera_uploads_video_quality_dialog_option_low,
    ),
    Medium(
        videoQuality = VideoQuality.MEDIUM,
        textRes = R.string.settings_camera_uploads_video_quality_dialog_option_medium,
    ),
    High(
        videoQuality = VideoQuality.HIGH,
        textRes = R.string.settings_camera_uploads_video_quality_dialog_option_high,
    ),
    Original(
        videoQuality = VideoQuality.ORIGINAL,
        textRes = R.string.settings_camera_uploads_video_quality_dialog_option_original,
    ),
}