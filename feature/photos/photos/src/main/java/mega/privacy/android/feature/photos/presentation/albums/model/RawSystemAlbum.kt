package mega.privacy.android.feature.photos.presentation.albums.model

import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.SystemAlbum
import mega.privacy.android.shared.resources.R as sharedResR
import javax.inject.Inject

/**
 * System album type for RAW photos
 */
class RawSystemAlbum @Inject constructor() : SystemAlbum {

    override val albumNameResId: Int = sharedResR.string.system_album_raw_title

    override val hideWhenEmpty: Boolean = true

    override val mediaTimelineFilter: MediaTimelineFilter = MediaTimelineFilter(
        granularity = MediaTimelineFilter.Granularity.Day,
        category = MediaTimelineFilter.Category.All,
        location = MediaTimelineFilter.Location.CloudDriveAndVault,
        sensitivity = MediaTimelineFilter.Sensitivity.ShowAll,
        subCategory = MediaTimelineFilter.SubCategory.Raw,
    )
}
