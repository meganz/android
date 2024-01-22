package mega.privacy.android.domain.usecase.videosection

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import javax.inject.Inject

/**
 * The use case for getting all video playlists.
 */
class GetVideoPlaylistsUseCase @Inject constructor() {

    /**
     * Get all video playlists.
     */
    operator fun invoke(): Flow<List<VideoPlaylist>> = flowOf(emptyList())
}