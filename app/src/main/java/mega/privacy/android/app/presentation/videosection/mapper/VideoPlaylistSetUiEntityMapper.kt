package mega.privacy.android.app.presentation.videosection.mapper

import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistSetUiEntity
import mega.privacy.android.domain.entity.set.UserSet
import javax.inject.Inject

/**
 * The mapper class to convert the UserSet to VideoPlaylistSetUiEntity
 */
class VideoPlaylistSetUiEntityMapper @Inject constructor() {

    /**
     * Convert the UserSet to VideoPlaylistSetUiEntity
     */
    operator fun invoke(set: UserSet) = VideoPlaylistSetUiEntity(
        id = set.id,
        title = set.name
    )
}