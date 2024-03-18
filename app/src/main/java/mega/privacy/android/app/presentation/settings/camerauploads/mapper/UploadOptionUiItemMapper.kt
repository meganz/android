package mega.privacy.android.app.presentation.settings.camerauploads.mapper

import mega.privacy.android.app.presentation.settings.camerauploads.model.UploadOptionUiItem
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import javax.inject.Inject

/**
 * UI Mapper class that retrieves the appropriate [UploadOptionUiItem] from a given [UploadOption]
 */
internal class UploadOptionUiItemMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param uploadOption The [UploadOption]
     * @return the specific [UploadOptionUiItem]
     */
    operator fun invoke(uploadOption: UploadOption) = when (uploadOption) {
        UploadOption.PHOTOS -> UploadOptionUiItem.PhotosOnly
        UploadOption.VIDEOS -> UploadOptionUiItem.VideosOnly
        UploadOption.PHOTOS_AND_VIDEOS -> UploadOptionUiItem.PhotosAndVideos
    }
}