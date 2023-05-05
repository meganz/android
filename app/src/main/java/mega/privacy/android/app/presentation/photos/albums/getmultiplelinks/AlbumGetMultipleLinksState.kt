package mega.privacy.android.app.presentation.photos.albums.getmultiplelinks

import mega.privacy.android.app.presentation.photos.albums.getlink.AlbumSummary
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumLink

/**
 * View State for the Get Multiple Links
 *
 * @property isInitialized
 * @property albumsSummaries
 * @property isSeparateKeyEnabled
 * @property albumLinks
 * @property albumLinksList
 * @property exitScreen
 */
data class AlbumGetMultipleLinksState(
    val isInitialized: Boolean = false,
    val albumsSummaries: Map<AlbumId, AlbumSummary> = mapOf(),
    val isSeparateKeyEnabled: Boolean = false,
    val albumLinks: Map<AlbumId, AlbumLink> = mapOf(),
    val albumLinksList: List<String> = listOf(),
    val exitScreen: Boolean = false,
)
