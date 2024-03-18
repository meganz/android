package mega.privacy.android.app.presentation.settings.camerauploads.model

import androidx.annotation.StringRes
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption

/**
 * An enumeration of different Upload Options to be used in the UI Layer
 *
 * @property uploadOption The domain [UploadOption] that identifies the UI item
 * @property textRes The corresponding text represented as a [StringRes]
 */
internal enum class UploadOptionUiItem(
    val uploadOption: UploadOption,
    @StringRes val textRes: Int,
) {
    PhotosOnly(
        uploadOption = UploadOption.PHOTOS,
        textRes = R.string.settings_camera_upload_only_photos,
    ),
    VideosOnly(
        uploadOption = UploadOption.VIDEOS,
        textRes = R.string.settings_camera_upload_only_videos,
    ),
    PhotosAndVideos(
        uploadOption = UploadOption.PHOTOS_AND_VIDEOS,
        textRes = R.string.settings_camera_upload_photos_and_videos,
    ),
}