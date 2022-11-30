package mega.privacy.android.app.presentation.photos.timeline.model

import mega.privacy.android.app.presentation.photos.model.FilterMediaType

enum class ApplyFilterMediaType(val type: FilterMediaType, val source: TimelinePhotosSource) {

    ALL_MEDIA_IN_CD_AND_CU(FilterMediaType.ALL_MEDIA, TimelinePhotosSource.ALL_PHOTOS),
    ALL_MEDIA_IN_CD(FilterMediaType.ALL_MEDIA, TimelinePhotosSource.CLOUD_DRIVE),
    ALL_MEDIA_IN_CU(FilterMediaType.ALL_MEDIA, TimelinePhotosSource.CAMERA_UPLOAD),

    IMAGES_IN_CD_AND_CU(FilterMediaType.IMAGES, TimelinePhotosSource.ALL_PHOTOS),
    IMAGES_IN_CD(FilterMediaType.IMAGES, TimelinePhotosSource.CLOUD_DRIVE),
    IMAGES_IN_CU(FilterMediaType.IMAGES, TimelinePhotosSource.CAMERA_UPLOAD),

    VIDEOS_IN_CD_AND_CU(FilterMediaType.VIDEOS, TimelinePhotosSource.ALL_PHOTOS),
    VIDEOS_IN_CD(FilterMediaType.VIDEOS, TimelinePhotosSource.CLOUD_DRIVE),
    VIDEOS_IN_CU(FilterMediaType.VIDEOS, TimelinePhotosSource.CAMERA_UPLOAD);

    companion object {
        /**
         * The default selected media type
         */
        val DEFAULT = ALL_MEDIA_IN_CD_AND_CU
    }
}