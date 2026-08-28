package mega.privacy.android.domain.usecase.videosection

import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.repository.VideoSectionRepository
import javax.inject.Inject

/**
 * The use case for getting all video playlist sets
 */
class GetVideoPlaylistSetsUseCase @Inject constructor(
    private val videoSectionRepository: VideoSectionRepository,
) {
    /**
     * Get all video playlist sets, ordered by name.
     *
     * The order is fixed rather than following the cloud sort order: the screen listing these
     * sets offers no sort control, and the cloud sort order's dimensions (size, label, favourite)
     * do not apply to a playlist.
     *
     * @return all video playlist sets ordered by name
     */
    suspend operator fun invoke(): List<UserSet> =
        videoSectionRepository.getVideoPlaylistSets()
            .sortedWith(compareBy(playlistTitleComparator, UserSet::name))
}
