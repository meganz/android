package mega.privacy.android.core.nodecomponents.mapper

import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt
import mega.privacy.android.domain.entity.node.NodeSourceType
import javax.inject.Inject

/**
 * Mapper from legacy view type (Int) to [NodeSourceType].
 * Use when navigating to destinations that expect [NodeSourceType] (e.g. [NodeOptionsBottomSheetNavKey])
 * from callers that only have Int? (e.g. [LegacyTextEditorNavKey.nodeSourceType]).
 */
class ViewTypeToNodeSourceTypeMapper @Inject constructor() {

    /**
     * Maps legacy Int view type to [NodeSourceType].
     * @param viewType Legacy adapter/view type constant, or null
     * @return Corresponding [NodeSourceType], or [NodeSourceType.CLOUD_DRIVE] when null or unknown
     */
    operator fun invoke(viewType: Int?): NodeSourceType = when (viewType) {
        NodeSourceTypeInt.FILE_BROWSER_ADAPTER -> NodeSourceType.CLOUD_DRIVE
        NodeSourceTypeInt.RUBBISH_BIN_ADAPTER -> NodeSourceType.RUBBISH_BIN
        NodeSourceTypeInt.LINKS_ADAPTER -> NodeSourceType.LINKS
        NodeSourceTypeInt.INCOMING_SHARES_ADAPTER -> NodeSourceType.INCOMING_SHARES
        NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER -> NodeSourceType.OUTGOING_SHARES
        NodeSourceTypeInt.BACKUPS_ADAPTER -> NodeSourceType.BACKUPS
        NodeSourceTypeInt.FAVOURITES_ADAPTER -> NodeSourceType.FAVOURITES
        NodeSourceTypeInt.DOCUMENTS_BROWSE_ADAPTER -> NodeSourceType.DOCUMENTS
        NodeSourceTypeInt.AUDIO_BROWSE_ADAPTER -> NodeSourceType.AUDIO
        NodeSourceTypeInt.VIDEO_BROWSE_ADAPTER -> NodeSourceType.VIDEOS
        NodeSourceTypeInt.SEARCH_BY_ADAPTER -> NodeSourceType.SEARCH
        NodeSourceTypeInt.RECENTS_BUCKET_ADAPTER -> NodeSourceType.RECENTS_BUCKET
        NodeSourceTypeInt.VIDEO_PLAYLISTS_ADAPTER -> NodeSourceType.VIDEO_PLAYLISTS
        NodeSourceTypeInt.VIDEO_RECENTLY_WATCHED_ADAPTER -> NodeSourceType.VIDEO_RECENTLY_WATCHED
        else -> NodeSourceType.CLOUD_DRIVE
    }
}
